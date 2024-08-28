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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLING_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.STARTING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.VNF_INSTANCE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.fasterxml.jackson.core.type.TypeReference;

public final class OperationsConstants {

    private OperationsConstants() {
    }

    public static final class Common {
        public static final String LIFECYCLE_OPERATION_TYPE = "lifecycleOperationType";
        public static final String OPERATION_STATE = "operationState";
        public static final String VNF_LCM_OP_OCC_ID = "vnfLcmOpOccId";
        public static final String OPERATION_OCCURRENCE_ID = "operationOccurrenceId";
        public static final String OCCURRENCE_ID = "occurrenceId";
        public static final String OPERATION = "operation";
        public static final String USERNAME = "username";
        public static final String OPERATION_RESPONSE = "operationResponse";
        public static final String SUCCESS = "success";
        public static final String VALUES_FILE_PREFIX = "values";
        public static final String VALUES_YAML_ADDITIONAL_PARAMETER = "values.yaml";
        public static final String APP_LIFECYCLE_OPERATIONS = "app_lifecycle_operations";
        public static final String COMBINED_VALUES_FILE = "combined_values_file";
        public static final String OPERATION_PARAMS = "operation_params";
        public static final String TEMP_INSTANCE = "temp_instance";
        public static final String LIFECYCLE_OPERATIONS_JOIN_PREFIX = "allOperations.";
        public static final String LIFECYCLE_OPERATIONS = "allOperations";
        public static final String CURRENT_OPERATION_JOIN_PREFIX = "allOperations2.";
        public static final String CURRENT_OPERATION = "allOperations2";
        public static final String LAST_OPERATION_JOIN_KEY = "lastLifecycleOperation";
        public static final String LAST_OPERATION_JOIN_PREFIX = LAST_OPERATION_JOIN_KEY + ".";
        public static final String CANCEL_PENDING = "cancelPending";
        public static final String AUTOMATIC_INVOCATION = "automaticInvocation";
        public static final String VALUES_FILE_PARAMS = "values_file_params";
        public static final String ENABLED_MODULE = "enabled";
        public static final String DISABLED_MODULE = "disabled";
        public static final String DEPLOYABLE_MODULES = "deployableModules";
        public static final List<String> VALID_DEPLOYABLE_MODULE_VALUES = List.of(ENABLED_MODULE, DISABLED_MODULE);
        public static final int DEFAULT_HIGHEST_PRIORITY_LEVEL = 1;
        public static final String RUNNING_LIFECYCLE_OPERATIONS_COUNT_DESCRIPTION = "Number of running operations";
        public static final String RUNNING_LIFECYCLE_OPERATIONS_COUNT = "cvnfm.running.lcm.operations.count";
        public static final String CONNECTIONS_TO_ENM_METRIC_TAGS = "cvnfm.enm.running.metrics";
        public static final String CONNECTIONS_TO_ENM_METRIC_TAGS_DESCRIPTION = "Metric that shows operations with ENM";
        public static final String RUNNING_LIFECYCLE_OPERATION_TAG = "operation";
        public static final String ENM_METRIC_NAME = "enm_metric_name";
        public static final String OPEN_SSH_TO_ENM_METRIC_TAG = "open_ssh_to_enm";
        public static final String CLOSE_SSH_TO_ENM_METRIC_TAG = "close_ssh_to_enm";
        public static final String STARTED_ADD_NODE_METRIC_TAG = "started_add_node_operations";
        public static final String COMPLETED_ADD_NODE_METRIC_TAG = "completed_add_node_operations";
        public static final String INSTANTIATE_ADD_NODE_METRIC_TAG = "instantiate_add_node_to_oss";
        public static final String INSTANTIATE_ENROLLMENT_METRIC_TAG = "instantiate_enrollment";



        private Common() {
        }
    }

    public static final class LifecycleOperations {
        // Defaults sort values (if Direction is not provided, equals to ASC)
        public static final Sort STATE_ENTERED_TIME = Sort.by(Sort.Direction.DESC, CommonConstants.STATE_ENTERED_TIME);
        public static final Sort START_TIME = Sort.by(Sort.Direction.DESC, CommonConstants.START_TIME);
        public static final Sort OPERATION_STATE = Sort.by(Common.OPERATION_STATE);
        public static final Sort OPERATION = Sort.by(Common.LIFECYCLE_OPERATION_TYPE);
        public static final Sort VNF_INSTANCE_ID = Sort.by(VNF_INSTANCE);
        public static final List<Sort> SORT_COLUMNS = List.of(STATE_ENTERED_TIME, OPERATION_STATE,
                                                              START_TIME,
                                                              OPERATION,
                                                              VNF_INSTANCE_ID);
        public static final List<LifecycleOperationState> ONGOING_OPERATION_STATES = List.of(STARTING, PROCESSING, ROLLING_BACK);
        public static final List<LifecycleOperationState> NOT_FAILED_OPERATION_STATES = List.of(STARTING, PROCESSING, COMPLETED);
        public static final List<LifecycleOperationState> TERMINAL_OPERATION_STATES = List.of(ROLLED_BACK, COMPLETED, FAILED, FAILED_TEMP);

        private LifecycleOperations() {
        }
    }

    public static final class Operations {
        // Defaults sort values (if Direction is not provided, equals to ASC)
        public static final Sort OPERATION_STATE = Sort.by(Common.OPERATION_STATE);
        public static final Sort LIFECYCLE_OPERATION_TYPE = Sort.by(Common.LIFECYCLE_OPERATION_TYPE);
        public static final Sort VNF_PRODUCT_NAME = Sort.by(VnfInstanceConstants.VnfResources.VNF_PRODUCT_NAME);
        public static final Sort VNF_SOFTWARE_VERSION = Sort.by(VnfInstanceConstants.VnfResources.VNF_SOFTWARE_VERSION);
        public static final Sort VNF_INSTANCE_NAME = Sort.by(VnfInstanceConstants.VNF_INSTANCE_NAME);
        public static final Sort CLUSTER_NAME = Sort.by(ClusterConstants.Request.CLUSTER_NAME);
        public static final Sort NAMESPACE = Sort.by(ClusterConstants.Request.NAMESPACE);
        public static final Sort START_TIME = Sort.by(Sort.Direction.DESC, CommonConstants.START_TIME);
        public static final Sort STATE_ENTERED_TIME = Sort.by(Sort.Direction.DESC, CommonConstants.STATE_ENTERED_TIME);
        public static final Sort USERNAME = Sort.by(Common.USERNAME);
        public static final List<Sort> SORT_COLUMNS = List.of(OPERATION_STATE, LIFECYCLE_OPERATION_TYPE, START_TIME,
                                                              STATE_ENTERED_TIME, VNF_PRODUCT_NAME, VNF_SOFTWARE_VERSION,
                                                              VNF_INSTANCE_NAME, CLUSTER_NAME, NAMESPACE, USERNAME
        );

        private Operations() {
        }
    }

    public static final class Instantiate {
        public static final String DAY0_CONFIGURATION_PREFIX = "day0.configuration.";
        public static final String OSS_TOPOLOGY_PREFIX = "ossTopology";
        public static final String SITEBASIC_XML = "sitebasic.xml";
        public static final String RETRIEVE_UNSEAL_KEY = "retrieveUnsealKey";
        public static final String HELM_RELEASE_NAME_SEPARATOR = "-";
        public static final String INSTANTIATE_VNF_REQUEST_PARAM = "instantiateVnfRequest";
        public static final String KMS_UNSEAL_KEY_POST_STRING = "eric-sec-key-management-unseal-key";
        public static final String KMS_UNSEAL_KEY = "kms-unseal-key";
        public static final String EXTERNAL_LDAP_SECRET_NAME = "eric-sec-admin-user-management-day0-external-ldap";
        public static final String EXTERNAL_LDAP_SECRET_KEY = "ldap-configuration.json";
        public static final String ENROLLMENT_CERTM_SECRET_NAME = "eric-sec-certm-deployment-configuration";
        public static final String ENROLLMENT_CERTM_SECRET_KEY = "eric-sec-certm-deployment-configuration.json";
        private Instantiate() {
        }
    }

    public static final class Heal {
        public static final String RESTORE_BACKUP_FILE_REFERENCE = "restore.backupFileReference";
        public static final String RESTORE_PASSWORD = "restore.password";
        public static final String RESTORE_BACKUP_NAME = "restore.backupName";
        public static final String RESTORE_SCOPE = "restore.scope";
        public static final String DAY0_CONFIGURATION_SECRETNAME_KEY = "day0.configuration.secretname";
        public static final String DAY0_CONFIGURATION_SECRETS = "day0.configuration.secrets";
        public static final String DAY0_CONFIGURATION_SECRETNAME_VALUE = "oss-node-protocol-secret";
        public static final String DAY0_CONFIGURATION_KEY = "day0.configuration.param%d.key";
        public static final String DAY0_CONFIGURATION_VALUE = "day0.configuration.param%d.value";
        public static final String SECRET_PARAM_PREFIX = "day0.configuration.param";
        public static final String KEY_SUFFIX = ".key";
        public static final String VALUE_SUFFIX = ".value";
        public static final String OSS_NODE_PROTOCOL_FILE_PARAM = "ossNodeProtocol";
        public static final String OSS_NODE_PROTOCOL_FILE_KEY_NAME = "ossNodeProtocolFile";
        public static final String IP_VERSION = "ipVersion";
        public static final String IP_V4 = "ipV4";
        public static final String IP_V6 = "ipV6";

        private Heal() {
        }
    }

    public static final class LifecycleOperationsFilters {
        public static final String OPERATION_OCCURRENCE_ID_FILTER = "lcmOperationDetails/operationOccurrenceId";
        public static final String OPERATION_STATE_FILTER = "lcmOperationDetails/operationState";
        public static final String STATE_ENTERED_TIME_FILTER = "lcmOperationDetails/stateEnteredTime";
        public static final String START_TIME_FILTER = "lcmOperationDetails/startTime";
        public static final String VNF_INSTANCE_ID_FILTER = "lcmOperationDetails/vnfInstanceId";
        public static final String OPERATION_FILTER = "lcmOperationDetails/lifecycleOperationType";
        public static final String CANCEL_MODE_FILTER = "lcmOperationDetails/cancelMode";
        public static final String GRANT_ID_FILTER = "lcmOperationDetails/grantId";
        public static final String IS_AUTOMATIC_INVOCATION_FILTER = "lcmOperationDetails/automaticInvocation";
        public static final String IS_CANCEL_PENDING_FILTER = "lcmOperationDetails/cancelPending";
        public static final String CURRENT_OPERATION_STATE_FILTER = "lcmOperationDetails2/operationState";
        public static final String LAST_OPERATION_TYPE_FILTER = "lastLifecycleOperation/lifecycleOperationType";
        public static final String LAST_OPERATION_STATE_FILTER = "lastLifecycleOperation/operationState";
        public static final String LAST_OPERATION_STATE_ENTERED_TYPE_FILTER = "lastLifecycleOperation/stateEnteredTime";

        private LifecycleOperationsFilters() {
        }
    }

    public static final class Rollback {
        public static final String DELETE = "delete";
        public static final String INSTALL = "install";
        public static final String UPGRADE = "upgrade";
        public static final String DELETE_PVC = "delete_pvc";
        public static final String ROLLBACK = "rollback"; // NOSONAR
        public static final String IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY = "isAutoRollbackAllowed";

        private Rollback() {
        }
    }

    public static final class Scale {
        public static final String REPLICA_PARAMETER_NAME = "%s.replicaCount";
        public static final String MIN_REPLICA_PARAMETER_NAME = "%s.minReplicas";
        public static final String MAX_REPLICA_PARAMETER_NAME = "%s.maxReplicas";
        public static final String MANO_CONTROLLED_SCALING = "manoControlledScaling";
        public static final String ASPECT_ID = "instantiatedVnfInfo/scaleStatus/aspectId";
        public static final String SCALE_LEVEL = "instantiatedVnfInfo/scaleStatus/scaleLevel";
        public static final String MANUAL_CONTROLLED = "ManualControlled";
        public static final String CISM_CONTROLLED = "CISMControlled";
        public static final String VNF_CONTROLLED_SCALING = "vnfControlledScaling";
        public static final String SCALE_INFO_ENTITY_ASPECT_ID = "scaleInfoEntity.aspectId";
        public static final String SCALE_INFO_ENTITY_SCALE_LEVEL = "scaleInfoEntity.scaleLevel";

        private Scale() {
        }
    }

    public static final class Request {
        public static final String CLEAN_UP_RESOURCES = "cleanUpResources";
        public static final String APPLICATION_TIME_OUT = "applicationTimeOut";
        public static final String SKIP_VERIFICATION = "skipVerification";
        public static final String STATE = "state";
        public static final String LIFECYCLE_OPERATION_ID = "lifecycleOperationId";
        public static final String OVERRIDE_GLOBAL_REGISTRY = "overrideGlobalRegistry";
        public static final String DELETE_IDENTIFIER = "deleteIdentifier";
        public static final String ADD_NODE_TO_OSS = "addNodeToOSS";
        public static final String CMP_V2_ENROLLMENT = "CMPv2Enrollment";
        public static final String SKIP_JOB_VERIFICATION = "skipJobVerification";
        public static final String RELEASE_NAME = "releaseName";
        public static final String ADDITIONAL_PARAMETERS = "additionalParams";
        public static final String DISABLE_OPENAPI_VALIDATION = "disableOpenapiValidation";
        public static final String HELM_WAIT = "helmWait";
        public static final String HELM_NO_HOOKS = "helmNoHooks";
        public static final String PERSIST_SCALE_INFO = "persistScaleInfo";
        public static final String PERSIST_DM_CONFIG = "persistDMConfig";
        public static final String PAYLOAD = "Payload";
        public static final String LABELS = "labels";
        public static final String EOCM_USERNAME = "eo-cm-user";
        public static final String EVNFM_USERNAME_KEY = "userName";
        public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-key";
        public static final String HELM_CLIENT_VERSION = "helmClientVersion";
        public static final String BRO_ENDPOINT_URL = "bro_endpoint_url";
        public static final String BRO_GET_BACKUP_MANAGERS = "%s/v1/backup-manager";
        public static final String BRO_GET_BACKUPS = "%s/v1/backup-manager/%s/backup";
        public static final String BACKUP_ACTION = "%s/v1/backup-manager/%s/action";
        public static final String BACKUP_NAME = "backupName";
        public static final String LCM_OP_OCCS = "/vnflcm/v1/vnf_lcm_op_occs/";
        public static final String LCM_VNF_INSTANCES = "/vnflcm/v1/vnf_instances/";
        public static final String HELM_CLIENT_VERSION_YAML = "helm_client_version";
        public static final String NOTIFICATION_TYPE = "notificationType";
        public static final String SKIP_MERGING_PREVIOUS_VALUES = "skipMergingPreviousValues";
        public static final TypeReference<HashMap<String, ReplicaDetails>> REPLICA_DETAILS_MAP_TYPE = new TypeReference<>() {
        };

        public static final List<String> EVNFM_PARAMS = Collections.unmodifiableList(
                Arrays.asList(PERSIST_SCALE_INFO, PERSIST_DM_CONFIG,
                              Scale.MANO_CONTROLLED_SCALING,
                              APPLICATION_TIME_OUT, SKIP_JOB_VERIFICATION, SKIP_VERIFICATION,
                              CLEAN_UP_RESOURCES, ClusterConstants.Request.NAMESPACE, RELEASE_NAME, OVERRIDE_GLOBAL_REGISTRY,
                              Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY, SKIP_MERGING_PREVIOUS_VALUES));

        public static final List<String> EVNFM_PARAMS_FOR_WFS = Collections.unmodifiableList(
                Arrays.asList(HELM_WAIT, HELM_NO_HOOKS, DISABLE_OPENAPI_VALIDATION)
        );

        private Request() {
        }
    }

    public static final class GrantingConstants {
        public static final String GRANT_ID = "grantId";
        public static final String OS_CONTAINER_REQUIREMENT_KEY = "container";
        public static final String VB_STORAGE_REQUIREMENT_KEY = "virtual_storage";

        private GrantingConstants() {
        }
    }

    public static final class Errors {
        public static final String NO_LIFE_CYCLE_OPERATION_FOUND = " vnf resource has no life cycle operation found";
        public static final String UPGRADE_FAILED_VNFD_KEY = "upgrade_failed_parameters";
        public static final String NOT_AUTHORIZED_FOR_NODE_TYPE_EXCEPTION =
                "User is not authorized to perform LCM operations on VNF with %s node type";
        public static final String FAILED_OPERATION = "Mark operation with id : %s as failed due to %s";
        public static final String SCALE_LEVEL_MISMATCH = "Scale level for %s should be equal to %s, but it is equal to %s.%n";
        public static final String SCALE_LEVEL_EXCEEDS_MAX_SCALE_LEVEL = "Scale level for %s is %s which exceeds max scale level %s.%n";
        public static final String MIN_REPLICA_COUNT_MISMATCH =
                "Min replica count for %s should be equal to initial delta:  %s, but it is equal to %s.%n";
        public static final String MIN_REPLICA_COUNT_MISSING = "Min replica count for %s is missing.%n";
        public static final String MAX_REPLICA_COUNT_MISSING = "Max replica count for %s is missing.%n";
        public static final String AUTOSCALING_PARAM_MISMATCH = "Autoscaling for %s should be the same.";
        public static final String AUTOSCALING_PARAM_CHANGED_FOR_MANO_CONTROLLED_SCALING = "%s is %s, but autoscaling for %s is %s.%n";
        public static final String SYNC_OPERATION_TIMED_OUT = "SYNC operation failed due to timeout.";
        public static final String SCALE_DISABLED_FOR_NON_SCALABLE_CHART = "%s is not scalable, can not perform scale operation.%n";
        public static final String SCALE_DISABLED_FOR_NON_SCALABLE_CHART_CANNOT_ENABLE_AUTOSCALING = "%s is not scalable, can not enable "
                + "autoscaling.%n";
        public static final String GRANTING_FAILED = "NFVO rejected a Granting request for VNF instance %s , Package ID %s "
                + "based on policies and available capacity";
        public static final String VNF_CONTROLLED_SCALING_SHOULD_BE_KEY_VALUE_PAIR = "vnfControlledScaling should be key value pair.";
        public static final String ASPECTS_SHOULD_BE_PRESENT_IN_REPLICA_DETAILS = "Aspect %s should be present in ReplicaDetails ";
        public static final String VDU_INITIAL_DELTA_SHOULD_BE_PRESENT = "VduInitialDelta should be present";
        public static final String LEVEL_ID_NOT_PRESENT_IN_VNFD = "InstantiationLevelId: %s not present in VNFD.";
        public static final String ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY =
                "Aspects specified in the request are not defined in the policy";
        public static final String VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE =
                "vnfControlledScaling can be only ManualControlled or CISMControlled.";
        public static final String EXTENSIONS_SHOULD_BE_KEY_VALUE_PAIR = "Extensions should be key value pair.";
        public static final String OPERATION_IN_PROGRESS_ERROR_MESSAGE =
                "Lifecycle operation %s is in progress for vnf instance %s, hence cannot perform operation";
        public static final String DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE = "Downgrade not supported for instance id %s";
        public static final String NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE = "Namespace %s for cluster %s is marked for deletion";
        public static final Pattern REGEX_RELEASE_NOT_FOUND = Pattern.compile("(.)*Error:(.)* release:(.)* not found(.)*");

        private Errors() {
        }
    }

    public static final class Messages {
        public static final String OPERATION_PERFORMED_TEXT = "Operation {} with {} {} has been performed by user {}";
        public static final String VNF_OPERATION_PERFORMED_TEXT = "Operation {} with VNF Instance ID {} has been performed by user {}";
        public static final String VNF_OPERATION_PERFORMED_TEXT_WITHOUT_USERNAME = "Operation {} with VNF Instance ID {} has been performed";
        public static final String VNF_OPERATION_INSTANTIATED_TEXT = "Instantiate VNF Instance";
        public static final String OPERATION_IS_FINISHED_TEXT = "Operation {} with {} {} has been finished";
        public static final String BACKUP_OPERATION_IS_FINISHED_TEXT = "Operation Backup with name {} and VNF Instance ID {} has been finished";
        public static final String BACKUP_OPERATION_PERFORMED_TEXT =
                "Operation Backup with name {} and VNF Instance ID {} has been performed by user {}";
        public static final String VNF_OPERATION_IS_FINISHED_TEXT = "Operation {} with VNF Instance ID {} has been finished";

        private Messages() {
        }
    }

    public static final class Notification {
        public static final String VNF_LCM_OPERATION_OCCURRENCE_NOTIFICATION = "VnfLcmOperationOccurrenceNotification";
        public static final String VNF_IDENTIFIER_DELETION_NOTIFICATION = "VnfIdentifierDeletionNotification";
        public static final String VNF_IDENTIFIER_CREATION_NOTIFICATION = "VnfIdentifierCreationNotification";
        public static final String VNF_ENM_NODE_ADDING_NOTIFICATION = "VnfEnmNodeAddingNotification";
        public static final String VNF_ENM_NODE_DELETION_NOTIFICATION = "VnfEnmNodeDeletionNotification";

        private Notification() {

        }
    }
}
