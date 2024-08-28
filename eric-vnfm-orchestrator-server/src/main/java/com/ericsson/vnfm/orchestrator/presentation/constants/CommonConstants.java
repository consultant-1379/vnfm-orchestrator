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

import java.util.regex.Pattern;

public final class CommonConstants {
    public static final String DEFAULT = "default";
    public static final String VNF_INSTANCE = "vnfInstance";
    public static final String VALUES = "values";
    public static final String JSON_FILE_ENDING = ".json";
    public static final String SKIP_IMAGE_UPLOAD = "skipImageUpload";
    public static final String DOWNSIZE_VNFD_KEY = "upgrade.downsize.allowed";
    public static final String ADDING_NODE_TO_OSS = "Adding node to OSS";
    public static final String DELETING_NODE_FROM_OSS = "Deleting node from OSS";
    public static final String ENABLE_SUPERVISIONS = "Enable supervisions for ENM node";
    public static final String RESTORING_FROM_FILE_BACKUP = "Restoring from file backup reference";
    public static final String TOSCA_ARTIFACTS_FILE = "tosca.artifacts.File";
    public static final String TGZ = ".tgz";
    public static final String ETSI_STREAM_KEY = "Global:Streams:etsi-notification-events";
    public static final String SITEBASIC_FILE = "sitebasic_file";
    public static final String STATE_ENTERED_TIME = "stateEnteredTime";
    public static final String START_TIME = "startTime";
    public static final String CANCEL_MODE = "cancelMode";
    public static final String IS_AUTOMATIC_INVOCATION = "isAutomaticInvocation";
    public static final String IS_CANCEL_PENDING = "isCancelPending";
    public static final String VNF_DESCRIPTOR_ID = "vnfDescriptorId";
    public static final String OPERATION_OCCURRENCE_ID = "operation_occurrence_id";
    public static final String SCOPE = "scope";
    public static final String REMOTE = "remote";
    public static final String REMOTE_HOST = "host";
    public static final String PASSWORD = "password";
    public static final String SOFTWARE_PACKAGE = "softwarePackage";
    public static final String DETAIL = "detail";
    public static final String STATUS = "status";
    public static final String TITLE = "title";
    public static final String OPERATION_WITHOUT_LICENSE_ATTRIBUTE = "isOperationWithoutLicense";
    public static final String LCM_OPERATIONS_LICENSE_TYPE = "LCM operations";
    public static final String ENM_INTEGRATION_LICENSE_TYPE = "ENM integration";
    public static final String CLUSTER_MANAGEMENT_LICENSE_TYPE = "Cluster management";

    public static final Pattern NUMBER_EXPRESSION = Pattern.compile("\\d+(\\.\\d+)?");
    public static final Pattern REGEX_BOOLEAN_EXPRESSION = Pattern.compile("(\\.*==|\\.*NULL|\\.*\\&|\\.*\\|\\|)");
    public static final int ALLOWED_NUMBER_OF_INSTANCES = 5;
    public static final int ALLOWED_NUMBER_OF_CLUSTERS = 2;



    private CommonConstants() {
    }

    public static final class Keycloack {
        public static final String TOKEN_PARSE_ERROR = "Error occur while parsing Bearer token: %s";
        public static final String NO_TOKEN_ERROR = "Barer token is not found in request";

        private Keycloack() {
        }
    }

    public static final class URL {
        public static final String WORKFLOW_URI = "http://%s/api/lcm/v3/resources/%s?instanceId=%s";
        public static final String WORKFLOW_VALIDATE_CONFIG_FILE_URI = "http://%s/api/internal/cluster/validate";
        public static final String URI_PATH_SEPARATOR = "/";
        public static final String USAGE_STATE_API = "update_usage_state";

        private URL() {
        }
    }

    public static final class Errors {
        public static final String PACKAGE_NOT_FOUND_ERROR_MESSAGE = "Package with id: \\\"%s\\\" not found";
        public static final String GET_PACKAGE_ERROR_MESSAGE = "Unable to get package with id %s due to: ";
        public static final String INVALID_PARAMETER_NAME_PROVIDED_ERROR_MESSAGE = "Invalid parameter name provided %s";
        public static final String FTL_EXCEPTION = "Failed to %s with following parameters %s due to the following error %s";
        public static final String ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE =
                "The request has been declined due to exceeding the limit of " + ALLOWED_NUMBER_OF_INSTANCES +
                        " instances that can be instantiated without the license permission for %s or without the NeLs service.";
        public static final String ILLEGAL_NUMBER_OF_CLUSTERS_ERROR_MESSAGE =
                "The request has been declined due to exceeding the limit of " + ALLOWED_NUMBER_OF_CLUSTERS +
                        " clusters that can be registered without the license permission for %s or without the NeLs service.";
        public static final String EXT_CP_NOT_FOUND = "ExtCp %s is not defined in the VNFD";
        public static final String EXT_CP_DUPLICATED_NAMES = "Duplicated extCp(s) with name(s) %s in the request";
        public static final String VDU_CP_NAD_NOT_FOUND = "VduCp %s nad(s) %s are not defined in the VNFD";
        public static final String EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE = "ExtCp's %s layer protocol has to be %s";
        public static final String EXT_CP_MISSED_PROTOCOL_DATA_FIELD = "ExtCp's %s field %s has to be defined";
        public static final String LEVEL_ID_NOT_PRESENT_IN_VNFD = "InstantiationLevelId: %s not present in VNFD.";
        public static final String NAD_PARAMETER_SHOULD_NOT_BE_PRESENT_FOR_VIRTUAL_CP =
                "NAD parameter (netAttDefResourceId) should not be present for VirtualCp";
        public static final String UNABLE_TO_PARSE_JSON_CAUSE_FORMAT = "Unable to parse json: [%s], because of %s";

        public static final String UNABLE_TO_OBJECT_TO_JSON_CAUSE_FORMAT = "Unable to convert object to json: [%s], because of %s";
        public static final String DEFAULT_TITLE_ERROR_FOR_LCM_OP = "Exception occurred";
        public static final String EVNFM_NAMESPACE_INSTANTIATION_ERROR = "Cannot instantiate in the same namespace which "
                + "EVNFM is deployed in: %s. Use a different Namespace.";
        public static final String DEPLOYABLE_MODULES_NOT_PRESENT_IN_VNFD_ERROR = "Deployable modules %s are not present in VNFD ID %s";
        public static final String DEPLOYABLE_MODULE_VALUES_INVALID_ERROR = "Deployable modules in request extensions contain invalid values: %s. "
                + "Only these values are allowed: %s";

        private Errors() {
        }
    }

    public static final class Request {
        public static final String SOFTWARE_VERSION = "softwareVersion";
        public static final String PACKAGE_VERSION = "packageVersion";
        public static final String SOURCE_PACKAGE = "sourcePackage";
        public static final String PAGE_NUMBER = "pageNumber";
        public static final String PAGE_SIZE = "pageSize";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String USERNAME = "username";
        public static final String PAYLOAD = "payload";
        public static final String VNF_ID = "vnf_id";
        public static final String TYPE = "type";
        public static final String TYPE_BLANK =  "about:blank";
        public static final String CONSUMER_GROUP_NAME = "eric-vnfm-orchestrator-service";
        public static final String WFS_STREAM_KEY = "CVNFM:Streams:workflow-events";
        public static final String TYPE_ID = "TypeId";
        public static final String IDEMPOTENCY_KEY = "Idempotency-key";
        public static final String TRACING = "tracing";
        public static final String GET_PACKAGE_INFO_CONTENT  = "PackageResponse %s";
        public static final int DEFAULT_PAGE_SIZE = 15;
        public static final long  WFS_STREAM_MAX_SIZE = 300L;
        public static final long  ETSI_NOTIFICATIONS_STREAM_MAX_SIZE = 300L;

        public static final String REDIS_KEY_PREFIX = "CVNFM:orchestrator:";
        public static final String LOCAL_WORKING_QUEUE = REDIS_KEY_PREFIX + "{vnfm-orchestrator-service}:working";
        public static final String LOCAL_INCOMING_QUEUE = REDIS_KEY_PREFIX + "{vnfm-orchestrator-service}:incoming";
        public static final String MESSAGE_POINTER = REDIS_KEY_PREFIX + "message-pointer-%s";
        public static final String WORKING_QUEUE_TIMEOUTS = REDIS_KEY_PREFIX + "operation-timeouts";
        public static final String MUTEX_STAGE = REDIS_KEY_PREFIX + "mutex:expired-stage:%s";
        public static final String MUTEX_WORKING_QUEUE = REDIS_KEY_PREFIX + "mutex:working-queue-timeouts";
        public static final String MUTEX_RESTORE = REDIS_KEY_PREFIX + "mutex-restore";
        public static final String MUTEX_WORKFLOW_MESSAGE_DEDUPLICATION = REDIS_KEY_PREFIX + "mutex:idempotency-key:%s";
        public static final String MUTEX_ENM_OPERATION_DEDUPLICATION = REDIS_KEY_PREFIX + "mutex:enm-operations:%s";


        private Request() {
        }
    }

    public static final class MDC {
        public static final String VNF_INSTANCE_KEY = "vnfInstanceId";
        public static final String LIFECYCLE_OPERATION_TYPE_KEY = "lifecycleOperationType";
        public static final String LIFECYCLE_OPERATION_OCCURRENCE_ID_KEY = "operationOccurrenceId";

        private MDC() {
        }
    }
}
