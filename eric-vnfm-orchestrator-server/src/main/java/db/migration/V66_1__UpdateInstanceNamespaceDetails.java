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
package db.migration;

import static com.ericsson.vnfm.orchestrator.utils.Utility.addConfigExtension;

import static db.migration.MigrationUtilities.VNF_ID;
import static db.migration.MigrationUtilities.generateUUIDUniqueToTheTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@SuppressWarnings("squid:S00101")
public class V66_1__UpdateInstanceNamespaceDetails extends BaseJavaMigration {

    private static final String NAMESPACE = "namespace";
    private static final String CLUSTER_SERVER = "cluster_server";
    private static final String CLUSTER_NAME = "cluster_name";
    private static final String NAMESPACE_DELETION_IN_PROGESS = "namespace_deletion_in_progess";
    private static final String ID_COLUMN = "id";
    private static final String VNFINSTANCE_NAMESPACE_DETAILS_TABLE = "vnfinstance_namespace_details";

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        List<Map<String, Object>> allVnfInstances = getAllVnfInstances(jdbcTemplate);
        allVnfInstances.forEach(row -> updateRowWithUrl(row, jdbcTemplate));

    }

    public List<Map<String, Object>> getAllVnfInstances(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_vnf_instance WHERE instantiation_state = 'INSTANTIATED'",
                new VnfInstanceRowMapper());
    }

    private void updateRowWithUrl(final Map<String, Object> row, final JdbcTemplate jdbcTemplate) {
        String clusterName = addConfigExtension((String) row.get(CLUSTER_NAME));
        if (clusterName == null) {
            return;
        }
        String sql = "SELECT cluster_server FROM app_cluster_config_file where config_file_name = :clusterName";
        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("clusterName", clusterName);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        List<String> clusterServers = namedParameterJdbcTemplate.queryForList(sql, namedParameters, String.class);

        if (CollectionUtils.isNotEmpty(clusterServers) && clusterServers.size() == 1
                && StringUtils.isNotEmpty(clusterServers.get(0))) {
            String clusterServer = clusterServers.get(0);
            row.put(CLUSTER_SERVER, clusterServer);
            final List<String> listOfIds = jdbcTemplate
                    .query("SELECT * FROM " + VNFINSTANCE_NAMESPACE_DETAILS_TABLE, new InstanceNamespaceRowMapper());
            row.put(ID_COLUMN, generateUUIDUniqueToTheTable(listOfIds));
            row.put(NAMESPACE_DELETION_IN_PROGESS, false);
            SimpleJdbcInsert insertIntoNamespaceDetails = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName(VNFINSTANCE_NAMESPACE_DETAILS_TABLE);
            insertIntoNamespaceDetails.execute(row);
        }
    }

    private class VnfInstanceRowMapper implements RowMapper<Map<String, Object>> {
        @Override
        public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put(VNF_ID, rs.getString(VNF_ID));
            row.put(NAMESPACE, rs.getString(NAMESPACE));
            row.put(CLUSTER_NAME, rs.getString(CLUSTER_NAME));
            return row;
        }
    }

    private class InstanceNamespaceRowMapper implements RowMapper<String> {
        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString(ID_COLUMN);
        }
    }
}
