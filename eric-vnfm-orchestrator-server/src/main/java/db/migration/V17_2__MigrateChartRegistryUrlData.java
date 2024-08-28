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

import static db.migration.MigrationUtilities.generateUUIDUniqueToTheTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V17_2__MigrateChartRegistryUrlData extends BaseJavaMigration {

    private static final String HELM_CHART_URL = "helm_chart_url";
    private static final String VNF_ID_COLUMN = "vnf_id";
    private static final String HELM_CHART = "Helm_chart";
    private static final String ID_COLUMN = "id";
    private static final String PRIORITY_COLUMN = "priority";

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllVnfInstances(jdbcTemplate).stream().filter(row -> Objects.nonNull(row.get(HELM_CHART_URL)))
                .filter(row -> StringUtils.isNotEmpty(row.get(HELM_CHART_URL).toString()))
                .forEach(row -> persistChartUrls(row, jdbcTemplate));
    }

    private List<Map<String, Object>> getAllVnfInstances(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_vnf_instance", new VnfInstanceRowMapper());
    }

    private void persistChartUrls(final Map<String, Object> row, final JdbcTemplate jdbcTemplate) {
        SimpleJdbcInsert insertIntoChartsUrl = new SimpleJdbcInsert(jdbcTemplate).withTableName(HELM_CHART);
        final List<String> listOfIds = jdbcTemplate
                .query("SELECT * FROM " + HELM_CHART, new ChartUrlsRowMapper());
        Map<String, Object> chartUrlParams = new HashMap<>();
        chartUrlParams.put(ID_COLUMN, generateUUIDUniqueToTheTable(listOfIds));
        chartUrlParams.put(VNF_ID_COLUMN, row.get(VNF_ID_COLUMN));
        chartUrlParams.put(HELM_CHART_URL, row.get(HELM_CHART_URL));
        chartUrlParams.put(PRIORITY_COLUMN, 1);
        insertIntoChartsUrl.execute(chartUrlParams);
    }

    private class VnfInstanceRowMapper implements RowMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put(VNF_ID_COLUMN, rs.getString(VNF_ID_COLUMN));
            row.put(HELM_CHART_URL, rs.getString(HELM_CHART_URL));
            return row;
        }
    }

    private class ChartUrlsRowMapper implements RowMapper<String> {

        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString(ID_COLUMN);
        }
    }
}
