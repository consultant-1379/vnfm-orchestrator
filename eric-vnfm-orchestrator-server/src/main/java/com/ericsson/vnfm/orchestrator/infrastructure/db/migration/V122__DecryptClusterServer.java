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
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration;

import static com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.Utils.createSelectQuery;
import static com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.Utils.updateRows;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DBFields.APP_CLUSTER_CONFIG_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DBFields.APP_VNF_INSTANCE_NAMESPACE_DETAILS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DBFields.CLUSTER_SERVER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.google.common.base.Strings;

@Component
@SuppressWarnings("squid:S00101")
public class V122__DecryptClusterServer extends BaseJavaMigration {

    private static final List<String> CLUSTER_CONFIG_FILES_COLUMNS = List.of(
            ID, CLUSTER_SERVER
    );

    private static final List<String> VNF_INSTANCE_NAMESPACE_DETAILS_COLUMNS = List.of(
            ID, CLUSTER_SERVER
    );

    @Autowired
    private CryptoService cryptoService;

    @Override
    public void migrate(final Context context) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        decryptData(jdbcTemplate, APP_CLUSTER_CONFIG_FILE, ID, CLUSTER_CONFIG_FILES_COLUMNS);
        decryptData(jdbcTemplate, APP_VNF_INSTANCE_NAMESPACE_DETAILS, ID, VNF_INSTANCE_NAMESPACE_DETAILS_COLUMNS);
    }

    private void decryptData(final JdbcTemplate jdbcTemplate, String tableName, String id, List<String> columns) {
        List<Map<String, String>> sensitiveInfo = queryData(jdbcTemplate, tableName, columns, id);
        sensitiveInfo.forEach(row -> updateRows(jdbcTemplate, tableName, id, columns, row));
    }

    private List<Map<String, String>> queryData(final JdbcTemplate jdbcTemplate, String tableName, List<String> columns, final String id) {
        String queryColumns = createSelectQuery(tableName, columns);
        return jdbcTemplate.query(queryColumns, new SensitiveRowMapper(columns, id));
    }


    private final class SensitiveRowMapper implements RowMapper<Map<String, String>> {
        private final List<String> columns;
        private final String id;

        private SensitiveRowMapper(List<String> columns, String id) {
            this.columns = columns; // NOSONAR
            this.id = id;
        }

        @Override
        public Map<String, String> mapRow(final ResultSet resultSet, final int rowNum) {
            Map<String, String> row = new HashMap<>();
            columns.forEach(col -> {
                try {
                    String val = resultSet.getString(col);
                    if (!Strings.isNullOrEmpty(val) && !id.equals(col)) {
                        val = cryptoService.decryptString(val);
                    }
                    row.put(col, val);
                } catch (SQLException e) {
                    throw new InternalRuntimeException(e);
                }
            });
            return row;
        }
    }
}
