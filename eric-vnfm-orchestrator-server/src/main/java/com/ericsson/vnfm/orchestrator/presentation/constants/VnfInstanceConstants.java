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
package com.ericsson.vnfm.orchestrator.presentation.constants;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.LAST_OPERATION_STATE_ENTERED_TYPE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.LAST_OPERATION_STATE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.LAST_OPERATION_TYPE_FILTER;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;

public final class VnfInstanceConstants {
    public static final String VNF_INSTANCE_ID = "vnfInstanceId";
    public static final String VNF_INSTANCE_WITH_VNF_INSTANCE_ID = "vnfInstance.vnfInstanceId";
    public static final String VNF_INSTANCE_NAME = "vnfInstanceName";
    public static final String VNF_INSTANCE_ID_PATTERN = "[a-z0-9]+(-[a-z0-9]+)*";
    public static final int DEFAULT_INSTANTIATION_LEVEL = 0;
    public static final String INSTANTIATION_STATE = "instantiationState";
    public static final String APP_VNF_INSTANCE = "app_vnf_instance";
    public static final String VNF_PROVIDER_NAME = "vnfProviderName";
    public static final String INSTANCE = "instance";
    public static final String TARGET_TYPE_NAME = "Target Type %s";

    private VnfInstanceConstants() {
    }

    public static final class VnfInstances {
        // Defaults sort values (if Direction is not provided, equals to ASC)
        public static final Sort VNF_INSTANCE_NAME = Sort.by(VnfInstanceConstants.VNF_INSTANCE_NAME);
        public static final Sort VNF_PROVIDER = Sort.by(VNF_PROVIDER_NAME);
        public static final Sort VNF_PRODUCT_NAME = Sort.by(VnfResources.VNF_PRODUCT_NAME);
        public static final Sort VNF_SOFTWARE_VERSION = Sort.by(VnfResources.VNF_SOFTWARE_VERSION);
        public static final Sort VNFD_VERSION = Sort.by(VnfResources.VNFD_VERSION);
        public static final Sort CLUSTER_NAME = Sort.by(ClusterConstants.Request.CLUSTER_NAME);
        public static final Sort INSTANTIATION_STATE = Sort.by(VnfInstanceConstants.INSTANTIATION_STATE);
        public static final List<Sort> SORT_COLUMNS = List.of(VNF_INSTANCE_NAME,
                                                              VNF_PROVIDER,
                                                              VNF_PRODUCT_NAME,
                                                              VNF_SOFTWARE_VERSION,
                                                              VNFD_VERSION,
                                                              CLUSTER_NAME,
                                                              INSTANTIATION_STATE);

        private VnfInstances() {
        }
    }

    public static final class VnfResources {
        public static final String INSTANCE_ID = "instanceId";
        public static final String VNF_INSTANCE_DESCRIPTION = "vnfInstanceDescription";
        public static final String VNFD_ID = "vnfdId";
        public static final String VNF_PROVIDER = "vnfProvider";
        public static final String VNF_PRODUCT_NAME = "vnfProductName";
        public static final String VNF_SOFTWARE_VERSION = "vnfSoftwareVersion";
        public static final String VNFD_VERSION = "vnfdVersion";
        public static final String VNF_PKG_ID = "vnfPkgId";
        public static final String VNF_PACKAGE_ID = "vnfPackageId";
        public static final String ADDED_TO_OSS = "addedToOss";
        public static final String LAST_STATE_CHANGE = "lastStateChanged";


        public static final Set<String> SORT_COLUMNS = Set.of(
                VNF_INSTANCE_NAME, VNF_PROVIDER, VNF_PRODUCT_NAME, VNF_SOFTWARE_VERSION,
                VNFD_VERSION, CLUSTER_NAME, INSTANTIATION_STATE,
                LIFECYCLE_OPERATION_TYPE, LAST_OPERATION_TYPE_FILTER,
                OPERATION_STATE, LAST_OPERATION_STATE_FILTER,
                LAST_STATE_CHANGE, LAST_OPERATION_STATE_ENTERED_TYPE_FILTER);
        public static final Map<String, String> SORT_COLUMN_MAPPINGS = Map.of(
                LAST_OPERATION_TYPE_FILTER, LIFECYCLE_OPERATION_TYPE,
                LAST_OPERATION_STATE_FILTER, OPERATION_STATE,
                LAST_OPERATION_STATE_ENTERED_TYPE_FILTER, LAST_STATE_CHANGE
        );

        private VnfResources() {
        }
    }

    public static final class Errors {
        public static final String VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE = " vnf resource not present";
        public static final String VNF_INSTANCE_IS_BEING_PROCESSED = "VNF instance with id %s is already being processed";
        public static final String VNF_INSTANCE_IS_ALREADY_INSTANTIATED = "VNF instance ID %s is already in the INSTANTIATED state";
        public static final String VNF_INSTANCE_IS_NOT_INSTANTIATED = "VNF instance ID %s is not in the INSTANTIATED state";
        public static final String VNF_INSTANCE_NOT_ADDED_TO_OSS = "The resource %s has not been added to OSS";
        public static final String MISSING_VNF_INSTANCE_PARAMS_MESSAGE =
                "bro_endpoint_url is not defined in VNFD for instance. Please see documentation";
        public static final String INVALID_REMOTE_OBJ = "Remote object must be a key/value pair. Please check the documentation.";

        private Errors() {
        }
    }

    public static final class Messages {
        public static final String VALIDATION_STARTED_LOG = "Started validating: %s";
        public static final String DRAC_DISABLED = "DRAC service not enabled";
        public static final String ROLES_AND_USERNAME_INFO_MESSAGE = "List of Roles for Username %s is %s ";
        public static final String EMPTY_NODE_TYPE = "NodeType is empty.";

        private Messages() {
        }
    }
}
