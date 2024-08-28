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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class defines a unit of data migration tied to single select request with results
 * processed on per row basis and stored back to a database using the same parametrized
 * update statement.
 */
@Getter
@AllArgsConstructor
public final class TableDefinition {
    private final String schema;
    private final String table;
    private final String[] selectColumns;
    private final String[] updateColumns;
    private final String[] idColumns;
    private final IdConverter idConverter;

    /**
     *  @param table table containing records to process
     * @param selectColumns array of column names being selected and processed
     * @param updateColumns array of column names being updated
     * @param idColumns array of column names representing a row id
     * @param idConverter implementation of {@link IdConverter} able to extract a row id from {@link java.sql.ResultSet} as opaque
     *                    and set it back to {@link java.sql.PreparedStatement}
     */
    public TableDefinition(final String table, final String[] selectColumns, final String[] updateColumns,
                           final String[] idColumns, final IdConverter idConverter) {
        this.updateColumns = updateColumns;
        this.schema = null;
        this.table = table;
        this.selectColumns = selectColumns;
        this.idColumns = idColumns;
        this.idConverter = idConverter;
    }
}
