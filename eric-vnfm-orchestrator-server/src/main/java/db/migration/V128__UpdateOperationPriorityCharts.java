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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class V128__UpdateOperationPriorityCharts extends BaseJavaMigration {

    private static final String ID = "id";
    private static final String VNF_INSTANCE_ID = "vnf_instance_id";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(
                context.getConnection(), true));
        List<String> vnfInstances = getAllVnfInstances(jdbcTemplate);
        for (String vnfId : vnfInstances) {
            List<HelmChart> helmCharts = getAllSortedHelmChartsByVnfInstanceId(jdbcTemplate, vnfId);

            List<LifecycleOperationType> lifecycleOperationTypes =
                    getAllLifecycleOperationsByVnfInstanceId(jdbcTemplate, vnfId)
                            .stream()
                            .map(LifecycleOperation::getLifecycleOperationType)
                            .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(helmCharts) && CollectionUtils.isNotEmpty(lifecycleOperationTypes)) {
                joinLifecycleOperationTypeAndPriority(jdbcTemplate, lifecycleOperationTypes, helmCharts);
            }
        }
    }

    private List<String> getAllVnfInstances(final JdbcTemplate jdbcTemplate) {
        NamedParameterJdbcTemplate jdbcTemplateObject = new NamedParameterJdbcTemplate(jdbcTemplate);
        return jdbcTemplateObject.query("SELECT * FROM app_vnf_instance", new VnfInstanceRowMapper());
    }

    private List<HelmChart> getAllSortedHelmChartsByVnfInstanceId(final JdbcTemplate jdbcTemplate,
                                                                  final String vnfInstanceId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue(VNF_INSTANCE_ID, vnfInstanceId);
        NamedParameterJdbcTemplate jdbcTemplateObject = new NamedParameterJdbcTemplate(jdbcTemplate);
        return jdbcTemplateObject.query(
                "SELECT * FROM helm_chart where vnf_id=:vnf_instance_id AND helm_chart_artifact_key is not null " +
                        "ORDER BY helm_chart_type, priority DESC",
                parameters,
                new HelmChartRowMapper());
    }

    private List<LifecycleOperation> getAllLifecycleOperationsByVnfInstanceId(final JdbcTemplate jdbcTemplate,
                                                                              final String vnfInstanceId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue(VNF_INSTANCE_ID, vnfInstanceId);
        NamedParameterJdbcTemplate jdbcTemplateObject = new NamedParameterJdbcTemplate(jdbcTemplate);
        return jdbcTemplateObject.query(
                "SELECT * FROM app_lifecycle_operations " +
                        "WHERE vnf_instance_id = :vnf_instance_id AND lifecycle_operation_type is not null",
                parameters,
                new LifecycleOperationRowMapper());
    }

    private void joinLifecycleOperationTypeAndPriority(final JdbcTemplate jdbcTemplate,
                                                       List<LifecycleOperationType> lifecycleOperationTypes,
                                                       List<HelmChart> helmCharts) {
        if (lifecycleOperationTypes.contains(LifecycleOperationType.INSTANTIATE)) {
            AtomicInteger terminatePriority = new AtomicInteger(1);
            AtomicInteger scalePriority = new AtomicInteger(getCnfChartsCount(helmCharts));
            helmCharts.forEach(helmChart -> {
                Map<LifecycleOperationType, Integer> chartsPriorities = helmChart.getOperationChartsPriority();
                chartsPriorities.putAll(Map.ofEntries(
                        Map.entry(LifecycleOperationType.INSTANTIATE, helmChart.getPriority()),
                        Map.entry(LifecycleOperationType.CHANGE_VNFPKG, helmChart.getPriority())));
                if (HelmChartType.CNF.equals(helmChart.getHelmChartType())) {
                    chartsPriorities.putAll(Map.ofEntries(
                            Map.entry(LifecycleOperationType.SCALE, scalePriority.getAndDecrement()),
                            Map.entry(LifecycleOperationType.TERMINATE, terminatePriority.getAndIncrement())));
                }
            });
            updateAllHelmCharts(jdbcTemplate, helmCharts);
        }
    }

    private int getCnfChartsCount(List<HelmChart> helmCharts) {
        return (int) helmCharts.stream()
                .filter(helmChart -> HelmChartType.CNF.equals(helmChart.getHelmChartType()))
                .count();
    }

    private void updateAllHelmCharts(JdbcTemplate jdbcTemplate,
                                     List<HelmChart> helmCharts) {
        NamedParameterJdbcTemplate jdbcTemplateObject = new NamedParameterJdbcTemplate(jdbcTemplate);
        jdbcTemplateObject.batchUpdate(
                "UPDATE helm_chart SET operation_charts_priority = :operation_charts_priority WHERE id = :id",
                helmCharts.stream()
                        .map(this::buildSqlProperties)
                        .toArray(SqlParameterSource[]::new));
    }

    private SqlParameterSource buildSqlProperties(HelmChart helmChart) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("operation_charts_priority",
                convertToJson(helmChart.getOperationChartsPriority()));
        parameters.addValue(ID, helmChart.getId());
        return parameters;
    }

    private String convertToJson(Map<LifecycleOperationType, Integer> operationProirityMap) {
        try {
            return objectMapper.writeValueAsString(operationProirityMap);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable convert Map to operationChartsPriority : {}", operationProirityMap.toString(), e);
            return "";
        }
    }

    private static class VnfInstanceRowMapper implements RowMapper<String> {
        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString("vnf_id");
        }
    }

    private static class HelmChartRowMapper implements RowMapper<HelmChart> {
        @Override
        public HelmChart mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            HelmChart helmChart = new HelmChart();
            helmChart.setId(rs.getString(ID));
            helmChart.setPriority(rs.getInt("priority"));
            String helmChartType = rs.getString("helm_chart_type");
            if (helmChartType == null) {
                helmChartType = rs.getString("helm_chart_artifact_key").toLowerCase().contains("crd") ?
                        HelmChartType.CRD.name() : HelmChartType.CNF.name();
            }
            helmChart.setHelmChartType(HelmChartType.valueOf(helmChartType));
            return helmChart;
        }
    }

    private static class LifecycleOperationRowMapper implements RowMapper<LifecycleOperation> {
        @Override
        public LifecycleOperation mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            LifecycleOperation lifecycleOperation = new LifecycleOperation();
            lifecycleOperation.setOperationOccurrenceId(rs.getString("operation_occurrence_id"));
            lifecycleOperation.setLifecycleOperationType(LifecycleOperationType
                    .valueOf(rs.getString("lifecycle_operation_type")));
            return lifecycleOperation;
        }
    }
}
