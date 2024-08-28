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

import static java.util.stream.Collectors.groupingBy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V18_2__AddReleaseNameMappingToChartTable extends BaseJavaMigration {

    private static final String ID_COLUMN = "id";
    private static final String VNF_ID = "vnf_id";
    private static final String PRIORITY = "priority";

    private static void persistReleaseName(final Object vnfId, final List<Map<String, Object>> charts,
            final JdbcTemplate jdbcTemplate) {
        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", vnfId);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        String vnfInstanceName = namedParameterJdbcTemplate
                .queryForObject("SELECT vnf_instance_name from app_vnf_instance where vnf_id = :id", namedParameters,
                        String.class);
        if (charts.size() == 1) {
            jdbcTemplate.update("UPDATE helm_chart set release_name = ? where id = ?", vnfInstanceName,
                    charts.get(0).get("id"));
        } else {
            for (Map<String, Object> chart : charts) {
                String releaseName = vnfInstanceName + "-" + chart.get(PRIORITY);
                jdbcTemplate
                        .update("UPDATE helm_chart set release_name = ? where id = ?", releaseName, chart.get("id"));
            }
        }
    }

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllHelmCharts(jdbcTemplate).stream().collect(groupingBy(row -> row.get(VNF_ID)))
                .forEach((vnfId, charts) -> persistReleaseName(vnfId, charts, jdbcTemplate));
    }

    private List<Map<String, Object>> getAllHelmCharts(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM helm_chart", new ChartUrlsRowMapper());
    }

    private class ChartUrlsRowMapper implements RowMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put(ID_COLUMN, rs.getString(ID_COLUMN));
            row.put(PRIORITY, rs.getInt(PRIORITY));
            row.put(VNF_ID, rs.getString(VNF_ID));
            return row;
        }
    }
}
