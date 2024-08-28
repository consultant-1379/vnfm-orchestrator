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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.REPLICA_DETAILS_MAP_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MAX_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MIN_REPLICA_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.REPLICA_PARAMETER_NAME;

import static db.migration.MigrationUtilities.ID_COLUMN;
import static db.migration.MigrationUtilities.REPLICA_DETAILS;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import db.mapper.HelmChartRowMapper;
import db.queries.MigrateChartReplicaDetailsQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("squid:S00101")
public class V88__RectifyReplicaParameterNameInReplicaDetails extends BaseJavaMigration {

    private static final String HELM_CHART = "helm_chart";
    private static final String HELM_CHART_HISTORY = "helm_chart_history";

    @Override
    public void migrate(final Context context) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        final ObjectMapper objectMapper = new ObjectMapper();
        List<Pair<String, String>> allHelmChartReplicaDetails = getReplicaDetailsFromTable(HELM_CHART, jdbcTemplate);
        modifyReplicaDetailsAndUpdate(HELM_CHART, allHelmChartReplicaDetails, objectMapper, namedParameterJdbcTemplate);
        List<Pair<String, String>> allHelmChartHistoryReplicaDetails = getReplicaDetailsFromTable(HELM_CHART_HISTORY,
                                                                                                  jdbcTemplate);
        modifyReplicaDetailsAndUpdate(HELM_CHART_HISTORY, allHelmChartHistoryReplicaDetails, objectMapper,
                                      namedParameterJdbcTemplate);
    }

    private static List<Pair<String, String>> getReplicaDetailsFromTable(String tableName, final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query(String.format(MigrateChartReplicaDetailsQuery.RETRIEVE_REPLICA_DETAILS, tableName),
                                  new HelmChartRowMapper());
    }

    private void modifyReplicaDetailsAndUpdate(String tableName, List<Pair<String, String>> allReplicaDetails,
                                               ObjectMapper objectMapper,
                                               NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        for (Pair<String, String> helmReplicaDetails : allReplicaDetails) {
            Map<String, ReplicaDetails> replicaDetails;
            try {
                LOGGER.info("Table {} with id {} Replica Details before migration {}", tableName,
                            helmReplicaDetails.getKey(), helmReplicaDetails.getValue());
                replicaDetails = objectMapper.readValue(helmReplicaDetails.getValue(), REPLICA_DETAILS_MAP_TYPE);
                modifyReplicaDetails(replicaDetails);
                String modifiedReplicaDetails = objectMapper.writeValueAsString(replicaDetails);
                LOGGER.info("Table {} with id {} Replica Details after migration {}", tableName,
                            helmReplicaDetails.getKey(), modifiedReplicaDetails);
                updateReplicaDetailsInHelmChart(helmReplicaDetails.getKey(), modifiedReplicaDetails, tableName,
                                                namedParameterJdbcTemplate);
            } catch (JsonProcessingException e) {
                LOGGER.error("Please cast the details manually. Cannot cast replica details", e);
                return;
            }
        }

    }

    private static void updateReplicaDetailsInHelmChart(String id, String replicaDetails, String tableName,
                                                 NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue(ID_COLUMN, id);
        in.addValue(REPLICA_DETAILS, replicaDetails);
        String sqlString = String.format("UPDATE %1$s SET %2$s = :%2$s where %3$s = :%3$s",
                                         tableName, REPLICA_DETAILS, ID_COLUMN);
        namedParameterJdbcTemplate.update(sqlString, in);
    }

    private static void modifyReplicaDetails(Map<String, ReplicaDetails> replicaDetailsMap) {
        for (Map.Entry<String, ReplicaDetails> replicaDetails : replicaDetailsMap.entrySet()) {
            ReplicaDetails replica = replicaDetails.getValue();
            if (replica.getAutoScalingEnabledParameterName() == null) {
                correctTheReplicaParameter(replica);
            }
        }
    }

    private static void correctTheReplicaParameter(ReplicaDetails replica) {
        if (isReplicaParameterAreSame(replica) && checkMaxAndMinReplicaParameterNamePresent(replica)) {
            String replicaParamSubString = getReplicaSubString(replica.getScalingParameterName());
            String maxReplicaParamSubString = getReplicaSubString(replica.getMaxReplicasParameterName());
            String minReplicaParamSubString = getReplicaSubString(replica.getMinReplicasParameterName());
            if (replicaParamSubString.equals(maxReplicaParamSubString)
                && replicaParamSubString.equals(minReplicaParamSubString)) {
                replica.setScalingParameterName(String.format(REPLICA_PARAMETER_NAME, replicaParamSubString));
                replica.setMaxReplicasParameterName(String.format(MAX_REPLICA_PARAMETER_NAME,
                                                                  maxReplicaParamSubString));
                replica.setMinReplicasParameterName(String.format(MIN_REPLICA_PARAMETER_NAME,
                                                                  minReplicaParamSubString));
            }
        }
    }

    private static boolean isReplicaParameterAreSame(ReplicaDetails replica) {
        return replica.getCurrentReplicaCount().equals(replica.getMaxReplicasCount())
            && replica.getCurrentReplicaCount().equals(replica.getMinReplicasCount());
    }

    private static boolean checkMaxAndMinReplicaParameterNamePresent(ReplicaDetails replica) {
        return !Strings.isNullOrEmpty(replica.getMaxReplicasParameterName())
            && !Strings.isNullOrEmpty(replica.getMinReplicasParameterName());
    }

    private static String getReplicaSubString(String parameterName) {
        int lastIndexOfPeriod = parameterName.lastIndexOf('.');
        return parameterName.substring(0, lastIndexOfPeriod);
    }
}
