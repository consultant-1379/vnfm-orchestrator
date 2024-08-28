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
package db.queries;

public final class MigrateChartReplicaDetailsQuery {

    public static final String UPDATE_HELM_CHARTS = "UPDATE helm_chart SET replica_details = :replicaDetails "
            + "WHERE vnf_id = :vnfId";

    public static final String GET_ALL_VNF_INSTANCES = "SELECT * FROM app_vnf_instance "
            + "WHERE (resource_details IS NOT NULL AND trim(resource_details) != '')"
            + "AND (policies IS NOT NULL AND trim(policies) != '')";

    public static final String RETRIEVE_REPLICA_DETAILS = "SELECT * FROM %s WHERE replica_details IS NOT NULL " +
        "AND trim(replica_details) != ''";

    private MigrateChartReplicaDetailsQuery() {
    }
}
