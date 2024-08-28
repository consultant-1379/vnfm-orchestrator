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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import db.mapper.VnfInstanceRowMapper;
import db.queries.MigrateChartReplicaDetailsQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("squid:S00101")
public class V63_1__MigrateHelmChartReplicaDetails extends BaseJavaMigration {

    private static final String REPLICA_DETAILS_PARAM = "replicaDetails";
    private static final String VNF_ID_PARAM = "vnfId";

    @Override
    public void migrate(final Context context) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<VnfInstance> allVnfInstances = getAllVnfInstances(jdbcTemplate);

        for (VnfInstance vnfInstance : allVnfInstances) {
            Policies policies;
            Map<String, Integer> resourceDetails;
            try {
                policies = objectMapper.readValue(vnfInstance.getPolicies(), Policies.class);
                TypeReference<HashMap<String, Integer>> typeRef = new TypeReference<>() {
                };
                resourceDetails = objectMapper.readValue(vnfInstance.getResourceDetails(), typeRef);
            } catch (JsonProcessingException e) {
                throw new InternalRuntimeException("Couldn't parse policies or resource details", e);
            }

            if (policies != null && resourceDetails != null) {
                migrateReplicaDetails(resourceDetails,
                                      policies.getAllInitialDelta(),
                                      vnfInstance,
                                      namedParameterJdbcTemplate,
                                      objectMapper);
            }
        }
    }

    private static void migrateReplicaDetails(final Map<String, Integer> resourceDetails,
                                              final Map<String, InitialDelta> allInitialDelta,
                                              final VnfInstance vnfInstance,
                                              final NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                              final ObjectMapper objectMapper) {
        Map<String, ReplicaDetails> replicaDetails = new HashMap<>();
        for (Map.Entry<String, Integer> resourceDetail : resourceDetails.entrySet()) {
            for (Map.Entry<String, InitialDelta> initialDelta : allInitialDelta.entrySet()) {
                if (isInitialDeltaTargetsMatchResourceDetailsTarget(initialDelta.getValue().getTargets(), resourceDetail.getKey())) {
                    replicaDetails.put(resourceDetail.getKey(), prepareReplicaDetails(initialDelta.getKey(),
                                                                                      resourceDetail.getValue(),
                                                                                      vnfInstance.getManoControlledScaling()));
                }
            }
        }

        String replicaDetailsAsString;
        try {
            replicaDetailsAsString = objectMapper.writeValueAsString(replicaDetails);
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException("Couldn't transform replicaDetails into JSON", e);
        }

        updateHelmChartsWithReplicaDetails(namedParameterJdbcTemplate, vnfInstance.getVnfInstanceId(), replicaDetailsAsString);
    }

    private static boolean isInitialDeltaTargetsMatchResourceDetailsTarget(final String[] targets,
                                                                           final String resourceDetailTarget) {
        return Arrays.asList(targets).contains(resourceDetailTarget);
    }

    private static ReplicaDetails prepareReplicaDetails(final String parameterName,
                                                        final Integer currentReplicaCount,
                                                        final Boolean manoControlledScaling) {
        Boolean autoScalingEnabledValue = Boolean.FALSE.equals(manoControlledScaling) ? Boolean.TRUE : Boolean.FALSE;
        return ReplicaDetails.builder()
                .withMinReplicasParameterName(parameterName + ".minReplica")
                .withMinReplicasCount(currentReplicaCount)
                .withMaxReplicasParameterName(parameterName + ".maxReplica")
                .withMaxReplicasCount(currentReplicaCount)
                .withScalingParameterName(parameterName + ".replicaCount")
                .withCurrentReplicaCount(currentReplicaCount)
                .withAutoScalingEnabledValue(autoScalingEnabledValue)
                .build();
    }

    private static List<VnfInstance> getAllVnfInstances(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query(MigrateChartReplicaDetailsQuery.GET_ALL_VNF_INSTANCES, new VnfInstanceRowMapper());
    }

    private static void updateHelmChartsWithReplicaDetails(final NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                                           final String vnfId,
                                                           final String replicaDetails) {
        SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue(REPLICA_DETAILS_PARAM, replicaDetails)
                .addValue(VNF_ID_PARAM, vnfId);
        namedParameterJdbcTemplate.update(MigrateChartReplicaDetailsQuery.UPDATE_HELM_CHARTS, namedParameters);
    }
}
