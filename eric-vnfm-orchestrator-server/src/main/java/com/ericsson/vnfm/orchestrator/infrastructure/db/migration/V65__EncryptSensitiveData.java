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
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.DBFields.CONFIG_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.VNF_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.SITEBASIC_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.APP_LIFECYCLE_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.COMBINED_VALUES_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.TEMP_INSTANCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_FILE_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ADD_NODE_OSS_TOPOLOGY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.INSTANTIATE_OSS_TOPOLOGY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.OSS_NODE_PROTOCOL_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.OSS_TOPOLOGY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.APP_VNF_INSTANCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.google.common.base.Strings;

@Profile({ "prod" })
@Component
@SuppressWarnings("squid:S00101")
public class V65__EncryptSensitiveData extends BaseJavaMigration {
    private static final List<String> VNF_INSTANCE_COLUMNS = List.of(
            VNF_ID, COMBINED_VALUES_FILE, OSS_TOPOLOGY, ADD_NODE_OSS_TOPOLOGY, INSTANTIATE_OSS_TOPOLOGY,
            OSS_NODE_PROTOCOL_FILE, TEMP_INSTANCE, SITEBASIC_FILE
    );
    private static final List<String> LIFECYCLE_OPERATIONS_COLUMNS = List.of(
            OPERATION_OCCURRENCE_ID, OPERATION_PARAMS, VALUES_FILE_PARAMS, COMBINED_VALUES_FILE
    );
    private static final List<String> CLUSTER_CONFIG_FILES_COLUMNS = List.of(
            ID, CONFIG_FILE
    );

    @Autowired
    private CryptoService cryptoService;

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        encryptData(jdbcTemplate, APP_VNF_INSTANCE, VNF_ID, VNF_INSTANCE_COLUMNS);
        encryptData(jdbcTemplate, APP_LIFECYCLE_OPERATIONS, OPERATION_OCCURRENCE_ID, LIFECYCLE_OPERATIONS_COLUMNS);
        encryptData(jdbcTemplate, APP_CLUSTER_CONFIG_FILE, ID, CLUSTER_CONFIG_FILES_COLUMNS);
    }

    private void encryptData(final JdbcTemplate jdbcTemplate, String tableName, String id, List<String> columns) {
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
                        val = cryptoService.encryptString(val);
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