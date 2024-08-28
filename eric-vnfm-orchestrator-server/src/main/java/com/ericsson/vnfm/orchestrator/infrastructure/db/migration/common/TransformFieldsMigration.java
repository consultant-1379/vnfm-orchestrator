/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for threaded java based Flyway migration intended for independent per-record data transformation.
 *
 * Subclasses should supply list of {@link TableDefinition} objects implementing {@link #getTableDefinitions()}
 * method.
 * For each definition a SELECT query will be built and executed. That query just selects all the data
 * columns and all id columns listed in the {@link TableDefinition}. Subclasses may customize this query overriding
 * {@link #buildSelectQuery(TableDefinition)} method.
 * Each row is converted to {@link FieldsTransformingTask} object that is placed in queue for processing. Subclasses that needs
 * any customization of task object may override the {@link #createTransformingTask(TableDefinition, Object, String[])} method.
 * Rows processing is performed by thread pool running a {@link Runnable}s that calls a {@link Consumer}&lt;{@link FieldsTransformingTask}&gt;
 * supplied at construction time. {@link Consumer}'s implementation is responsible for actual data transformation.
 * Transformation results are passed back to main thread that updated corresponding records in the database.
 * Updates are performed as a batch whenever possible. As with select, an actual query for update can also be customized
 * by overriding {@link #buildUpdateQuery(TableDefinition)} method.
 */
@Slf4j
public abstract class TransformFieldsMigration extends BaseJavaMigration {
    protected static final String DELIMITER = ", ";

    protected final boolean dryRun;
    protected final int threadsNumber;
    protected final Consumer<FieldsTransformingTask> transformer;
    protected BlockingQueue<FieldsTransformingTask> scheduled;
    protected BlockingQueue<FieldsTransformingTask> completed;
    protected boolean batchSupported;

    protected TransformFieldsMigration(final boolean dryRun, final int threadsNumber,
                                       final Consumer<FieldsTransformingTask> transformer) {
        this.dryRun = dryRun;
        this.threadsNumber = threadsNumber;
        this.transformer = transformer;
    }

    @Override
    public void migrate(final Context context) throws Exception {
        if (dryRun) {
            LOGGER.info("Dry run requested, skipping migration actions");
            return;
        }
        Connection connection = context.getConnection();
        batchSupported = connection.getMetaData().supportsBatchUpdates();
        ExecutorService executorService = setupWorkers();
        try {
            List<TableDefinition> tables = getTableDefinitions();
            for (TableDefinition table : tables) {
                int tasksSubmitted = loadAndSubmitTableRows(connection, table);
                LOGGER.debug("Loaded {} rows from table {}", tasksSubmitted, table.getTable());
                handleResults(connection, table, tasksSubmitted);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * This method must be implemented by successors to supply a list of objects that defines
     * an array of column names being extracted from SELECT query ResultSet, an array of
     * column names that forms a row id (composite keys are supported as well) and
     * {@link IdConverter} implementation that is able to extract id column(s) value(s) as an
     * opaque object and set them back to UPDATE query as parameters. For more details please
     * refer the {@link TableDefinition} documentation.
     * @return List of {@link TableDefinition} objects, each representing an iteration of migration cycle.
     */
    protected abstract List<TableDefinition> getTableDefinitions();

    protected String buildSelectQuery(TableDefinition table) {
        return  "SELECT " + String.join(DELIMITER, table.getSelectColumns())
                + DELIMITER + String.join(DELIMITER, table.getIdColumns()) + " FROM " + table.getTable();
    }

    protected String buildUpdateQuery(final TableDefinition table) {
        return "UPDATE " + table.getTable() + " SET "
                + Arrays.stream(table.getUpdateColumns()).map(s -> s + "=?").collect(Collectors.joining(", "))
                + " WHERE " + Arrays.stream(table.getIdColumns()).map(s -> s + "=?").collect(Collectors.joining(" AND "));
    }

    private int loadAndSubmitTableRows(final Connection connection, final TableDefinition table) throws SQLException, InterruptedException {
        int tasksSubmitted = 0;
        String selectSql = buildSelectQuery(table);
        try (Statement select = connection.createStatement();
                ResultSet resultSet = select.executeQuery(selectSql)) {
            while (resultSet.next()) {
                submitTransformingTask(table, resultSet);
                tasksSubmitted++;
            }
        }
        return tasksSubmitted;
    }

    private void submitTransformingTask(TableDefinition table, ResultSet resultSet) throws SQLException, InterruptedException {
        String[] fields = new String[table.getSelectColumns().length];
        Object id = table.getIdConverter().extractId(resultSet);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = resultSet.getString(i + 1);
        }
        FieldsTransformingTask task = createTransformingTask(table, id, fields);
        scheduled.put(task);
    }

    protected FieldsTransformingTask createTransformingTask(TableDefinition table, Object id, String[] fields) {
        return new FieldsTransformingTask(table, id, fields);
    }

    private void handleResults(final Connection connection, final TableDefinition table,
                               final int tasksSubmitted) throws SQLException, InterruptedException {
        String updateSQL = buildUpdateQuery(table);
        LOGGER.debug("Updating table {} using query: {}", table.getTable(), updateSQL);
        int tasksRemaining = tasksSubmitted;
        try (PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {
            while (tasksRemaining > 0) {
                handleCompletedTask(updateStatement);
                tasksRemaining--;
            }
            if (batchSupported) {
                LOGGER.debug("Executing a batch of update queries for table {}", table.getTable());
                updateStatement.executeBatch();
            }
        }
    }

    private void handleCompletedTask(PreparedStatement updateStatement) throws SQLException, InterruptedException {
        FieldsTransformingTask task = completed.take();
        if (!task.isSucceeded()) {
            throw new TransformationFailedException("Transformation failed with " + task.getFailureCause().getClass().getName(),
                                                    task.getFailureCause());
        }
        for (int i = 0; i < task.getUpdateValues().length; i++) {
            updateStatement.setString(i + 1, task.getUpdateValues()[i]);
        }
        task.getTableDefinition().getIdConverter().setId(updateStatement, task.getId());
        if (batchSupported) {
            updateStatement.addBatch();
        } else {
            updateStatement.executeUpdate();
        }
    }

    private ExecutorService setupWorkers() {
        scheduled = new LinkedBlockingQueue<>();
        completed = new LinkedBlockingQueue<>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadsNumber);
        for (int i = 0; i < threadsNumber; i++) {
            executorService.submit(new TransformingRunnable(scheduled, completed, transformer));
        }
        return executorService;
    }
}
