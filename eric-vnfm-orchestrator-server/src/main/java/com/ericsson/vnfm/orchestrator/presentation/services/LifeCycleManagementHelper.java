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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static java.lang.String.format;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CONFIG_EXTENSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.KUBE_NAMESPACES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DETAIL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEFAULT_TITLE_ERROR_FOR_LCM_OP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EVNFM_NAMESPACE_INSTANTIATION_ERROR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATUS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.TITLE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.EXTENSIONS_SHOULD_BE_KEY_VALUE_PAIR;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.BRO_ENDPOINT_URL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANO_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANCE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.UNEXPECTED_EXCEPTION_OCCURRED;
import static com.ericsson.vnfm.orchestrator.utils.Utility.addConfigExtension;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.isValidJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DuplicateCombinationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceDeletionInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LifeCycleManagementHelper {
    private static final Pattern REGEX_FOR_CATCHING_BASH_INJECTION = Pattern.compile(".*([;|'\"]|&&).*");
    private static final String BASH_INJECTION_ERROR_MESSAGE = "One of the following invalid sequences found in command ';' '|' '&&'";
    private static final String INVALID_BRO_URL = "The Url : %s is invalid due to %s. Please provide a valid URL.";
    public static final String WORKFLOW_ERROR_MSG = "errorDetails";
    public static final String INSTANTIATE_OPERATION = "instantiate";
    public static final String EMPTY_BRO_URL = "Bro URL cannot be empty. Please provide a valid BRO URL.";
    private static final int UPPER_BOUND_UI_APP_TIMEOUT = 1_000_000_000;

    @Value("${evnfm.namespace}")
    private String evnfmNamespace;

    @Value("${workflow.command.execute.defaultTimeOut}")
    private long defaultTimeout;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ClusterConfigService clusterConfigService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private HelmClientVersionValidator helmClientVersionValidator;

    public String getApplicationTimeout(final Map<String, Object> additionalParams) {
        if (MapUtils.isEmpty(additionalParams)) {
            return String.valueOf(defaultTimeout);
        }

        return additionalParams.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(APPLICATION_TIME_OUT) && isApplicationTimeoutValid(String.valueOf(e.getValue())))
                .map(e -> String.valueOf(e.getValue()))
                .findAny()
                .orElseGet(() -> String.valueOf(defaultTimeout));
    }

    public static boolean isApplicationTimeoutValid(String applicationTimeout) {
        if (!StringUtils.isNumeric(applicationTimeout)) {
            return false;
        }

        int appTimeout = Integer.parseInt(applicationTimeout);
        return Range.between(0, UPPER_BOUND_UI_APP_TIMEOUT).contains(appTimeout);
    }

    public static void checkVnfInstanceForBashInjection(VnfInstance vnfInstance) {
        String[] userInputtedValues = vnfInstance.getAllBashInjectionCheckedValuesAsArray();
        for (String value : userInputtedValues) {
            if (!StringUtils.isEmpty(value) && REGEX_FOR_CATCHING_BASH_INJECTION.matcher(value).matches()) {
                throw new InvalidInputException(BASH_INJECTION_ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void verifyNamespaceCanBeUsed(final InstantiateVnfRequest instantiateVnfRequest,
                                         final String vnfInstanceName, final String operation) {
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        String tempClusterName = instantiateVnfRequest.getClusterName();
        String clusterConfigFileCrdNamespace = databaseInteractionService
                .getClusterConfigCrdNamespaceByClusterName(tempClusterName);

        if (MapUtils.isEmpty(additionalParams) || additionalParams.get(NAMESPACE) == null
                || StringUtils.isEmpty(additionalParams.get(NAMESPACE).toString())) {

            String namespace = checkForInstanceName(vnfInstanceName, tempClusterName, clusterConfigFileCrdNamespace);

            if (additionalParams == null) {
                additionalParams = new HashMap();
                additionalParams.put(NAMESPACE, namespace);
                instantiateVnfRequest.setAdditionalParams(additionalParams);
            } else {
                additionalParams.put(NAMESPACE, namespace);
            }
        } else {
            String namespace = additionalParams.get(NAMESPACE).toString();
            verifyNamespaceForKubeNamespaces(operation, namespace);
            verifyNamespaceForCrdNamespace(namespace, clusterConfigFileCrdNamespace);
            verifyNamespaceForEvnfmNamespace(namespace, tempClusterName);
        }
    }

    public static boolean manoControlledScalingParameterIsPresent(final Map additionalParams) {
        return (additionalParams != null) && (additionalParams.get(MANO_CONTROLLED_SCALING) != null);
    }

    public static boolean vnfInstanceSupportsScaling(final VnfInstance instance) {
        return instance.getPolicies() != null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseExtensionsFromRequest(final Object extensions) {
        if (extensions == null) {
            return Collections.emptyMap();
        }

        try {
            return (Map<String, Object>) extensions;
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(EXTENSIONS_SHOULD_BE_KEY_VALUE_PAIR, ex);
        }
    }

    public static void setNamespace(final VnfInstance instance, final Map<String, Object> additionalParams) {
        Object name = additionalParams.get(NAMESPACE);
        instance.setNamespace(name.toString());
    }

    public static void setInstantiateOssTopology(final VnfInstance vnfInstance,
                                                 final Map<String, PropertiesModel> ossTopology) {
        vnfInstance.setInstantiateOssTopology(new JSONObject(ossTopology).toString());
    }

    @Transactional
    public void checkDuplicateConstraint(final VnfInstance vnfInstance,
                                         final InstantiateVnfRequest instantiateVnfRequest) {
        Map<String, Object> additionalParameters = checkAndCastObjectToMap(instantiateVnfRequest.getAdditionalParams());
        String namespace = (String) additionalParameters.get(NAMESPACE);
        String clusterName = addConfigExtension(vnfInstance.getClusterName());
        if (databaseInteractionService.getDuplicateInstances(vnfInstance.getVnfInstanceName(), namespace,
                                                             clusterName) > 0) {
            throw new DuplicateCombinationException("Duplicate combination of resource instance name, target cluster server and namespace.");
        }
    }

    public synchronized void persistLifecycleOperationInProgress(LifecycleOperation operation,
                                                                  VnfInstance vnfInstance,
                                                                  LifecycleOperationType type) {
        databaseInteractionService.persistLifecycleOperationInProgress(operation, vnfInstance, type);
    }

    public void setOperationErrorFor4xx(LifecycleOperation operation, String errorMsg) {
        final var problemDetails = new JSONObject();
        problemDetails.put(DETAIL, sanitizeWorkflowError(errorMsg));
        problemDetails.put(STATUS, HttpStatus.UNPROCESSABLE_ENTITY.value());

        setError(problemDetails.toString(), operation);
    }

    public void setOperationError(LifecycleOperation operation, String cause, String title, HttpStatus status) {
        String vnfInstanceId = operation.getVnfInstance().getVnfInstanceId();
        JSONObject problemDetails = createErrorJson(cause, title, status, vnfInstanceId);

        setError(problemDetails.toString(), operation);
    }

    public void addBroUrlIfPresentToInstance(final Map<String, Object> valuesYamlMap, final VnfInstance vnfInstance) {
        if (MapUtils.isNotEmpty(valuesYamlMap) && valuesYamlMap.containsKey(BRO_ENDPOINT_URL)) {
            addBroUrlEndpoint((String) valuesYamlMap.get(BRO_ENDPOINT_URL), vnfInstance);
        }
    }

    public static void setOperationErrorAndStateFailed(final LifecycleOperation operation,
                                                       final String message,
                                                       final HttpStatus status,
                                                       final String vnfInstanceId) {
        JSONObject problemDetails = createErrorJson(message, DEFAULT_TITLE_ERROR_FOR_LCM_OP, status, vnfInstanceId);
        setError(problemDetails.toString(), operation);
        updateOperationState(operation, LifecycleOperationState.FAILED);
    }

    private static JSONObject createErrorJson(final String cause, final String title, final HttpStatus status, final String vnfInstanceId) {
        final var errorJson = new JSONObject();
        errorJson.put(TITLE, title);
        errorJson.put(STATUS, status.value());
        errorJson.put(TYPE, status.getReasonPhrase());
        errorJson.put(DETAIL, cause);
        errorJson.put(INSTANCE, vnfInstanceId);

        return errorJson;
    }

    private static void addBroUrlEndpoint(final String broEndpointUrl, final VnfInstance vnfInstance) {
        LOGGER.info("Bro Endpoint config to validate: {}", broEndpointUrl);
        String validBroUrl = validateAndCreateBroUrlEndpoint(broEndpointUrl);
        vnfInstance.setBroEndpointUrl(validBroUrl);
    }

    private static String validateAndCreateBroUrlEndpoint(String broEndpointUrl) {
        try {
            if (StringUtils.isEmpty(broEndpointUrl)) {
                throw new InvalidInputException(EMPTY_BRO_URL);
            }
            return new URL(StringUtils.trim(broEndpointUrl)).toString();
        } catch (MalformedURLException e) {
            throw new InvalidInputException(String.format(INVALID_BRO_URL, broEndpointUrl.trim(), e.getMessage()), e);
        }
    }

    private String checkForInstanceName(final String vnfInstanceName, final String clusterName, String crdNamespace) {
        String tempInstanceName = vnfInstanceName;
        List<String> vnfInstanceNames = getDuplicateClusterNamespace(clusterName, vnfInstanceName);
        LOGGER.debug("duplicate Cluster Namespace: {}", vnfInstanceNames);
        tempInstanceName = (CollectionUtils.isNotEmpty(vnfInstanceNames)
                || isRestrictedNamespace(tempInstanceName, crdNamespace, clusterName)) ?
                appendRandomString(5, tempInstanceName) : tempInstanceName;
        return tempInstanceName;
    }

    private boolean isRestrictedNamespace(String namespace, String crdNamespace, String clusterName) {
        final boolean isEvnfmNamespaceAndCluster = namespace.equals(evnfmNamespace) &&
                workflowRoutingService.isEvnfmNamespaceAndCluster(namespace, clusterName);
        return namespace.equals(crdNamespace) || KUBE_NAMESPACES.contains(namespace) || isEvnfmNamespaceAndCluster;
    }

    private static String appendRandomString(int count, String tempInstanceName) {
        StringBuilder namespace = new StringBuilder();
        // Append a random String between a-z
        namespace.append(tempInstanceName).append("-").append(RandomStringUtils.random(count - 2, 97, 122, true, false)) // NOSONAR
                .append(RandomStringUtils.randomNumeric(2)); // NOSONAR
        return namespace.toString();
    }

    void verifyNamespaceForKubeNamespaces(final String operation, final String tempNamespace) {
        if (StringUtils.equals(operation, INSTANTIATE_OPERATION) && KUBE_NAMESPACES.contains(tempNamespace)) {
            throw new IllegalArgumentException(String.format("Cannot instantiate in any of the " +
                                                                     "Kubernetes initialized namespaces : %s", KUBE_NAMESPACES));
        }
    }

    void verifyNamespaceForCrdNamespacesByClusterName(final String namespace, final String clusterName) {
        String clusterConfigFileCrdNamespace = databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(clusterName);

        verifyNamespaceForCrdNamespace(namespace, clusterConfigFileCrdNamespace);
    }

    private void verifyNamespaceForCrdNamespace(final String namespace, final String clusterConfigFileCrdNamespace) {
        if (namespace.equals(clusterConfigFileCrdNamespace)) {
            throw new IllegalArgumentException(
                    String.format("%s is reserved for CRDs. Cannot instantiate CNFs in CRD namespace", namespace));
        }
    }

    void verifyNamespaceForEvnfmNamespace(final String namespace, final String clusterName) {
        if (namespace.equals(evnfmNamespace)) {
            final boolean isEvnfmNamespaceAndCluster = workflowRoutingService.isEvnfmNamespaceAndCluster(namespace, clusterName);
            if (isEvnfmNamespaceAndCluster) {
                throw new IllegalArgumentException(String.format(EVNFM_NAMESPACE_INSTANTIATION_ERROR, namespace));
            }
        }
    }

    private List<String> getDuplicateClusterNamespace(final String clusterName, final String tempNamespace) {
        String clusterWithConfig;
        String clusterWithoutConfig;
        if (clusterName.contains(CONFIG_EXTENSION)) {
            clusterWithConfig = clusterName;
            clusterWithoutConfig = clusterName.substring(0, clusterName.indexOf(CONFIG_EXTENSION));
        } else {
            clusterWithoutConfig = clusterName;
            clusterWithConfig = clusterName + CONFIG_EXTENSION;
        }

        return databaseInteractionService.getDuplicateClusterNamespace(clusterWithConfig, clusterWithoutConfig, tempNamespace);
    }

    public void setExpiredTimeoutAndPersist(LifecycleOperation operation, String applicationTimeout) {
        setExpiredTimeout(operation, applicationTimeout);
        databaseInteractionService.persistLifecycleOperation(operation);
    }

    public void setExpiredTimeout(LifecycleOperation operation, String applicationTimeout) {
        final long timeout = Integer.parseInt(applicationTimeout);
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(timeout + 120);
        LOGGER.debug("Expired application timeout was set to: {}", totalTimeout);
        operation.setExpiredApplicationTime(totalTimeout);
    }

    public void setTimeouts(LifecycleOperation operation, String applicationTimeout) {
        LOGGER.debug("Application timeout was set to: {}", applicationTimeout);
        operation.setApplicationTimeout(applicationTimeout);
        setExpiredTimeout(operation, applicationTimeout);
    }

    public static String sanitizeWorkflowError(String message) {
        return isValidJsonString(message) ? getWorkflowErrorDetails(message) : message;
    }

    public String validateAndGetHelmClientVersion(final Map<String, Object> additionalParams) {
        return helmClientVersionValidator.validateAndGetHelmClientVersion(additionalParams);
    }

    public boolean isNamespaceRestricted(List<VnfInstanceNamespaceDetails> namespaceDetails) {
        return namespaceDetails.stream()
                .anyMatch(VnfInstanceNamespaceDetails::isDeletionInProgress);
    }

    private static String getWorkflowErrorDetails(final String message) {
        final var errorJson = new JSONObject(message);

        return errorJson.isNull(WORKFLOW_ERROR_MSG)
                ? UNEXPECTED_EXCEPTION_OCCURRED
                : String.valueOf(errorJson.get(WORKFLOW_ERROR_MSG));
    }

    public static String getWorkflowErrorDetailsMessages(final String message) {
        if (Strings.isNullOrEmpty(message)) {
            return message;
        }

        try {
            List<Map<String, String>> errorDetailsMessages = parseJsonToGenericType(message, new TypeReference<>() { });
            return errorDetailsMessages.stream()
                    .flatMap(map -> map.entrySet().stream())
                    .filter(entry -> "message".equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.joining(", "));
        } catch (IllegalArgumentException ex) { // NOSONAR
            return message;
        }
    }

    public static void updateOperationState(final LifecycleOperation operation, final LifecycleOperationState targetState) {
        if (!targetState.equals(operation.getOperationState())) {
            LOGGER.info("Updating operation state to {}", targetState);
            operation.setOperationState(targetState);
            operation.setStateEnteredTime(LocalDateTime.now());
        }
    }

    public static void updateOperationStateToProcessing(final LifecycleOperation operation) {
        updateOperationState(operation, LifecycleOperationState.PROCESSING);
    }

    public synchronized void persistNamespaceDetails(final VnfInstance vnfInstance) {
        String clusterServer = databaseInteractionService.getClusterConfigServerByClusterName(vnfInstance.getClusterName());

        List<VnfInstanceNamespaceDetails> namespaceDetailsPresent = databaseInteractionService
                .getNamespaceDetailsPresent(vnfInstance.getNamespace(), clusterServer);

        if (isNamespaceRestricted(namespaceDetailsPresent)) {
            throw new NamespaceDeletionInProgressException(
                    format(NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE, vnfInstance.getNamespace(),
                           vnfInstance.getClusterName()));
        }

        final VnfInstanceNamespaceDetails details = getVnfInstanceNamespaceDetails(vnfInstance, namespaceDetailsPresent, clusterServer);
        databaseInteractionService.persistNamespaceDetails(details);
    }

    private VnfInstanceNamespaceDetails getVnfInstanceNamespaceDetails(final VnfInstance vnfInstance,
                                                                       final List<VnfInstanceNamespaceDetails> namespaceDetailsPresent,
                                                                       final String clusterServer) {
        Optional<VnfInstanceNamespaceDetails> namespaceDetailsByVnfInstanceId = databaseInteractionService
                .getNamespaceDetails(vnfInstance.getVnfInstanceId());
        final VnfInstanceNamespaceDetails details;
        if (namespaceDetailsByVnfInstanceId.isPresent()) {
            details = namespaceDetailsByVnfInstanceId.get();
            details.setDeletionInProgress(false);
            if (vnfInstance.isCleanUpResources() && namespaceDetailsPresent.size() == 1) {
                details.setDeletionInProgress(true);
            }
        } else {
            details = new VnfInstanceNamespaceDetails();
            details.setClusterServer(clusterServer);
            details.setNamespace(vnfInstance.getNamespace());
            details.setVnfId(vnfInstance.getVnfInstanceId());
            details.setDeletionInProgress(false);
            if (vnfInstance.isCleanUpResources() && namespaceDetailsPresent.isEmpty()) {
                details.setDeletionInProgress(true);
            }
            LOGGER.info("Creating namespace details for instance -  {}", vnfInstance.getVnfInstanceId());
        }
        return details;
    }
}
