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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEFAULT_HIGHEST_PRIORITY_LEVEL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_MERGING_PREVIOUS_VALUES;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationStateToProcessing;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.parameterPresent;
import static com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils.parseJsonOperationAdditionalParams;
import static com.ericsson.vnfm.orchestrator.utils.BooleanUtils.getBooleanValue;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getEnabledNotCrdAndNotProcessedCnfChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getHelmChartByPriority;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getHelmChartWithHighestPriorityByDeployableModulesSupported;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.updateHelmChartsDeletePvcState;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.NULL_DEFAULT_STRING;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logMultiValueMap;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logWfsRequestWithDay0Configuration;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.getValuesString;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.writeMapToValuesFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.NamespaceValidationResponse;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoDeploymentStatefulSet;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DeleteNamespaceException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HelmVersionsException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceValidationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PodStatusException;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.LoggingUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.ericsson.workflow.orchestration.mgmt.model.v3.SecretInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("java:S2629")
@Service
@Slf4j
public final class WorkflowRoutingServicePassThrough implements WorkflowRoutingService {

    public static final String WORKFLOW_UNAVAILABLE = "Workflow service is unavailable or unable to process the request";
    private static final String RESOURCES_URI_V3 = "api/lcm/v3/resources";
    private static final String INTERNAL_GET_VALUES_URI = "api/internal/kubernetes/values/%s?namespace=%s&clusterName=%s";
    private static final String INTERNAL_RESOURCES_URI = "api/internal/pods?clusterName=%s";
    private static final String INTERNAL_NAMESPACES_VALIDATE = "api/internal/namespaces/%s/validate?clusterName=%s";
    private static final String INTERNAL_REL4_RESOURCES_URI = "api/internal/additionalResourceInfo?clusterName=%s"; // NOSONAR
    private static final String INTERNAL_NAMESPACE_URI = "api/internal/v2/namespaces/%s/delete?clusterName=%s&releaseName=%s&applicationTimeOut=%s"
            + "&lifecycleOperationId=%s";
    private static final String INTERNAL_DELETE_PVC_URI = "api/internal/kubernetes/pvcs";
    private static final String INTERNAL_DOWNSIZE_URI = "api/internal/kubernetes/pods/scale/down";
    private static final String EXCEPTION_FROM_WORKFLOW_SERVICE = "Exception from Workflow service {}";
    private static final String INTERNAL_GET_HELM_VERSIONS = "api/internal/helm/versions";
    private static final String BASE_URL = "http://%s/%s";
    private static final String POST_BASE_URL = BASE_URL + "/%s";
    private static final String DELETE_PVCS_URL = POST_BASE_URL + "/delete";
    private static final String TERMINATE_URL = POST_BASE_URL + "/terminate";
    private static final String URL_UPGRADE = POST_BASE_URL + "/upgrade";
    private static final String URL_INSTANTIATE = POST_BASE_URL + "/instantiate";
    private static final String URL_ROLLBACK = POST_BASE_URL + "/rollback";
    private static final String URL_SCALE = POST_BASE_URL + "/scale";
    private static final String INTERNAL_GET_SECRET_URI = "api/internal/kubernetes/secrets?clusterName=%s&namespace=%s";
    private static final String INTERNAL_PUT_SECRET_URI = "api/internal/kubernetes/secrets/%s";

    @Value("${workflow.host}")
    private String workflowHost;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ValuesFileService valuesService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private WorkflowRequestBodyBuilder workflowRequestBodyBuilder;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    @Qualifier("wfsRoutingRetryTemplate")
    private RetryTemplate retryTemplate;

    @SuppressWarnings("unchecked")
    private static void provideArgument(final StringBuilder requestBuilder, final Object key, final Object value) {
        if (value instanceof List) {
            String values = String.join(",", (List<String>) value);
            requestBuilder.append(key).append("=").append(values).append("&");
        } else {
            requestBuilder.append(key).append("=").append(value).append("&");
        }
    }

    private static WorkflowRoutingResponse createResponse(final ResponseEntity<ResourceResponse> response) {
        final WorkflowRoutingResponse workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus((HttpStatus) response.getStatusCode());
        ResourceResponse resourceResponse = response.getBody();
        if (resourceResponse != null) {
            workflowRoutingResponse.setLinks(resourceResponse.getLinks());
            workflowRoutingResponse.setInstanceId(resourceResponse.getInstanceId());
            workflowRoutingResponse.setErrorMessage(resourceResponse.getErrorDetails());
        }
        return workflowRoutingResponse;
    }

    private static HttpHeaders createHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString());
        return headers;
    }

    private static HttpHeaders createHeadersForGetRequest() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString());
        return headers;
    }

    private void addIdempotencyHeader(HttpHeaders httpHeaders, String lifecycleOpId) {
        String idempotencyKey = lifecycleOpId + "_" + UUID.randomUUID();
        httpHeaders.add("Idempotency-key", idempotencyKey);
    }

    private static WorkflowRoutingResponse getWorkflowServiceUnavailableResponse() {
        final WorkflowRoutingResponse workflowRoutingResponse;
        workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus(HttpStatus.SERVICE_UNAVAILABLE);
        workflowRoutingResponse.setErrorMessage(WORKFLOW_UNAVAILABLE);
        return workflowRoutingResponse;
    }

    private static WorkflowRoutingResponse getWorkflowRoutingErrorResponse(final HttpStatusCodeException e) {
        final WorkflowRoutingResponse workflowRoutingResponse;
        workflowRoutingResponse = new WorkflowRoutingResponse();
        workflowRoutingResponse.setHttpStatus((HttpStatus) e.getStatusCode());
        workflowRoutingResponse.setErrorMessage(e.getResponseBodyAsString());
        return workflowRoutingResponse;
    }

    private static void updateHelmChartState(final VnfInstance vnfInstance, final String releaseName) {
        vnfInstance.getHelmCharts().stream()
                .filter(chart -> !StringUtils.equals(LifecycleOperationState.FAILED.toString(), chart.getState()))
                .filter(chart -> StringUtils.equals(releaseName, chart.getReleaseName()))
                .findFirst().ifPresent(chart -> chart.setState(LifecycleOperationState.PROCESSING.toString()));
    }

    private static String getReleaseName(final VnfInstance vnfInstance, final int priority) {
        return getHelmChartByPriority(vnfInstance, priority).getReleaseName();
    }

    private static String getReleaseName(VnfInstance vnfInstance) {
        int priority = getHelmChartWithHighestPriorityByDeployableModulesSupported(vnfInstance).getPriority();
        return getHelmChartByPriority(vnfInstance, priority).getReleaseName();
    }

    @VisibleForTesting
    static EvnfmWorkFlowRollbackRequest createRollbackRequestBody(final VnfInstance vnfInstance,
                                                                  final LifecycleOperation operation, final String revisionNumber) {
        Map<String, Object> additionalParameter = getAdditionalParametersForRollback(operation);
        additionalParameter.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        String revisionToUse = Strings.isNullOrEmpty(revisionNumber) ? "0" : revisionNumber;

        final EvnfmWorkFlowRollbackRequest.EvnfmWorkFlowRollbackRequestBuilder requestBuilder =
                new EvnfmWorkFlowRollbackRequest
                        .EvnfmWorkFlowRollbackRequestBuilder(additionalParameter)
                        .resetToRevision(revisionToUse)
                        .withClusterName(vnfInstance.getClusterName())
                        .withLifecycleOperationId(operation.getOperationOccurrenceId())
                        .withLifecycleOperationState(operation.getOperationState().toString())
                        .withHelmClientVersion(operation.getHelmClientVersion())
                        .inNamespace(vnfInstance.getNamespace());
        final EvnfmWorkFlowRollbackRequest requestBody = requestBuilder.build();
        LOGGER.debug("Rollback request body {}", requestBody);
        return requestBody;
    }

    @Override
    public WorkflowSecretResponse routeToEvnfmWfsForGettingAllSecrets(String clusterName, String namespace) {
        String url = String.format(BASE_URL, workflowHost, String.format(INTERNAL_GET_SECRET_URI, clusterName,
                                                                         namespace));
        HttpHeaders headers = createHeaders();
        MultiValueMap<String, Object> bodyParameters = workflowRequestBodyBuilder.buildRequestBody(new HashMap<>(), clusterName, null);
        HttpEntity<Object> requestEntity = new HttpEntity<>(bodyParameters, headers);

        LOGGER.info("Getting all Secret using url {}", url);
        ResponseEntity<WorkflowSecretResponse> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, WorkflowSecretResponse.class);
        } catch (Exception ex) {
            throw new InternalRuntimeException("An error occurred during getting all Secret", ex);
        }

        LOGGER.info("Completed request to Workflow service. URI: {}, Response code: {}", url, response.getStatusCode());

        if (response != null && response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else if (response != null && response.getBody() != null) {
            throw new InternalRuntimeException("Failed due to " + response.getBody().toString()); // NOSONAR
        } else {
            throw new InternalRuntimeException("Failed due to response is null from the Workflow service");
        }
    }

    @Override
    public ResponseEntity<Object> routeToEvnfmWfsForPatchingSecrets(final String secretName, final String key, final String keyContent,
                                                                    final String clusterName, final String namespace) {
        SecretInfo secretInfo = new SecretInfo();
        secretInfo.setClusterName(clusterName);
        secretInfo.setKey(key);
        secretInfo.setValue(keyContent);
        secretInfo.setNamespace(namespace);

        String url = String.format(BASE_URL, workflowHost, String.format(INTERNAL_PUT_SECRET_URI, secretName));

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(secretInfo, clusterName, null);
        return sendWFSRequest(createHeaders(), requestBody, url, HttpMethod.PUT);
    }

    private static Map<String, Object> getAdditionalParametersForRollback(LifecycleOperation currentOperation) {
        if (List.of(LifecycleOperationType.CHANGE_VNFPKG, LifecycleOperationType.SCALE).contains(currentOperation.getLifecycleOperationType())) {
            Object additionalParams = parseJsonOperationAdditionalParams(currentOperation);
            if (additionalParams == null) {
                return new HashMap<>();
            }
            return Utility.copyParametersMap(additionalParams);
        }
        return new HashMap<>();
    }

    private EvnfmWorkFlowInstantiateRequest createInstantiateRequestBody(final VnfInstance vnfInstance,
                                                                         final LifecycleOperation operation,
                                                                         final Object additionalParameters,
                                                                         final String clusterName) {
        Map<String, Object> additionalParamsCopy = Utility.copyParametersMap(additionalParameters);
        Map<String, Object> day0ConfigurationParams = retrieveDay0ConfigurationParams(vnfInstance);
        int helmChartPriority = getHelmChartWithHighestPriorityByDeployableModulesSupported(vnfInstance).getPriority();
        return getEvnfmWorkFlowInstantiateRequest(vnfInstance, operation, helmChartPriority, additionalParamsCopy, day0ConfigurationParams,
                                                  clusterName);
    }

    private EvnfmWorkFlowInstantiateRequest createInstantiateRequestBody(final LifecycleOperation operation, final int priority) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> day0ConfigurationParams = new HashMap<>();

        if (LifecycleOperationType.HEAL.equals(operation.getLifecycleOperationType())) {
            Map<String, Object> operationAdditionalParametersForHeal = getOperationAdditionalParameters(operation);

            if (!MapUtils.isEmpty(operationAdditionalParametersForHeal)) {
                setParametersFromOperationalParamsForHeal(operationAdditionalParametersForHeal, day0ConfigurationParams, additionalParams);
            }
        } else {
            if (!Strings.isNullOrEmpty(operation.getCombinedAdditionalParams())) {
                additionalParams = getStringObjectMap(operation.getCombinedAdditionalParams());
            }

            day0ConfigurationParams = retrieveDay0ConfigurationParams(vnfInstance);
        }

        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        return getEvnfmWorkFlowInstantiateRequest(vnfInstance, operation, priority, additionalParams,
                                                  day0ConfigurationParams, vnfInstance.getClusterName());
    }

    @SuppressWarnings("unchecked")
    private static void setParametersFromOperationalParamsForHeal(Map<String, Object> operationAdditionalParametersForHeal,
                                                                  Map<String, Object> day0ConfigurationParams,
                                                                  Map<String, Object> additionalParams) {
        for (Map.Entry<String, Object> parameter : operationAdditionalParametersForHeal.entrySet()) {
            String parameterKey = parameter.getKey();
            if (parameterKey.startsWith(DAY0_CONFIGURATION_PREFIX)) {
                Map<String, Object> day0 = MapUtils.getMap(operationAdditionalParametersForHeal, parameterKey);
                day0ConfigurationParams.putAll(day0 != null ? day0 : new HashMap<>());
            } else {
                additionalParams.put(parameterKey, operationAdditionalParametersForHeal.get(parameterKey));
            }
        }
    }

    private static Map<String, Object> getOperationAdditionalParameters(final LifecycleOperation operation) {
        if (StringUtils.isNotEmpty(operation.getOperationParams())) {
            return MessageUtility.getAdditionalParams(operation);
        }
        return Collections.emptyMap();
    }

    public static String resolveTimeOut(final LifecycleOperation operation) {
        LocalDateTime expiredApplicationTime = operation.getExpiredApplicationTime();
        LocalDateTime currentTime = LocalDateTime.now();
        return Long.toString(ChronoUnit.SECONDS.between(currentTime, expiredApplicationTime.minusSeconds(120)));
    }

    private static EvnfmWorkFlowInstantiateRequest getEvnfmWorkFlowInstantiateRequest(final VnfInstance vnfInstance,
                                                                                      final LifecycleOperation operation,
                                                                                      final int priority,
                                                                                      final Map<String, Object> additionalParams,
                                                                                      final Map<String, Object> day0ConfigurationParams,
                                                                                      final String clusterName) {
        HelmChart helmChart = getHelmChartByPriority(vnfInstance, priority);
        helmChart.setState(operation.getOperationState().toString());
        LifecycleOperationState lifecycleOperationState =
                operation.getOperationState() != null ? operation.getOperationState() : LifecycleOperationState.PROCESSING;
        HelmChartType helmChartType = helmChart.getHelmChartType() != null
                ? helmChart.getHelmChartType()
                : HelmChartType.CNF;
        return new EvnfmWorkFlowInstantiateRequest.EvnfmWorkFlowInstantiateBuilder(helmChart.getHelmChartUrl(), additionalParams)
                .withClusterName(clusterName)
                .withLifecycleOperationId(vnfInstance.getOperationOccurrenceId())
                .withLifecycleOperationState(lifecycleOperationState.toString())
                .withOverrideGlobalRegistry(vnfInstance.isOverrideGlobalRegistry())
                .withDay0Configuration(day0ConfigurationParams)
                .withChartType(helmChartType)
                .withChartVersion(helmChart.getHelmChartVersion())
                .withHelmClientVersion(vnfInstance.getHelmClientVersion())
                .inNamespace(getNamespaceBasedOnChartType(vnfInstance, helmChartType))
                .build();
    }

    private Map<String, Object> retrieveDay0ConfigurationParams(final VnfInstance vnfInstance) {
        String decryptDetailsForKey = cryptoUtils.getDecryptDetailsForKey(DAY0_CONFIGURATION_PREFIX, vnfInstance);
        if (StringUtils.isEmpty(decryptDetailsForKey)) {
            return new HashMap<>();
        }
        return getStringObjectMap(decryptDetailsForKey);
    }

    private Map<String, Object> getStringObjectMap(final String string) {
        try {
            return mapper.readValue(string, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to convert decrypted information to map with the following error {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private static EvnfmWorkFlowUpgradeRequest createUpgradeRequestBody(ChangeOperationContext context, int priority) {
        final VnfInstance tempInstance = context.getTempInstance();
        final LifecycleOperation operation = context.getOperation();

        Map<String, Object> additionalParams = Utility.copyParametersMap(context.getAdditionalParams());
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));

        HelmChart helmChart = getHelmChartByPriority(tempInstance, priority);
        LOGGER.info("ChangeVnfPkg for helmChart with release name: {}", helmChart.getReleaseName());
        HelmChartType helmChartType = helmChart.getHelmChartType() != null
                ? helmChart.getHelmChartType()
                : HelmChartType.CNF;
        return new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder(
                helmChart.getHelmChartUrl(), additionalParams)
                .withClusterName(tempInstance.getClusterName())
                .withLifecycleOperationId(operation.getOperationOccurrenceId())
                .withLifecycleOperationState(operation.getOperationState().toString())
                .inNamespace(getNamespaceBasedOnChartType(tempInstance, helmChartType))
                .withOverrideGlobalRegistry(tempInstance.isOverrideGlobalRegistry())
                .withChartType(helmChartType)
                .withChartVersion(helmChart.getHelmChartVersion())
                .withHelmClientVersion(tempInstance.getHelmClientVersion())
                .build();
    }

    private static String getNamespaceBasedOnChartType(VnfInstance tempInstance, HelmChartType helmChartType) {
        return helmChartType == HelmChartType.CRD ? tempInstance.getCrdNamespace() : tempInstance.getNamespace();
    }

    @Override
    public WorkflowRoutingResponse routeInstantiateRequest(final VnfInstance vnfInstance,
                                                           final LifecycleOperation operation,
                                                           final InstantiateVnfRequest instantiateVnfRequest) {
        return routeToEvnfmWFSInstantiate(vnfInstance, operation, instantiateVnfRequest);
    }

    @Override
    public WorkflowRoutingResponse routeInstantiateRequest(final VnfInstance vnfInstance,
                                                           final LifecycleOperation operation,
                                                           final InstantiateVnfRequest instantiateVnfRequest,
                                                           final Path toValuesFile) {
        return routeToEvnfmWFSInstantiate(vnfInstance, operation, instantiateVnfRequest, toValuesFile);
    }

    @Override
    public WorkflowRoutingResponse routeInstantiateRequest(final int priority, final LifecycleOperation operation,
                                                           final VnfInstance vnfInstance) {
        return routeToEvnfmWFSInstantiate(priority, operation, vnfInstance);
    }

    @Override
    public WorkflowRoutingResponse routeInstantiateRequest(final VnfInstance vnfInstance,
                                                           final LifecycleOperation operation,
                                                           final HelmChart helmChart,
                                                           final Map<String, Object> additionalParams,
                                                           final Path toValuesFile) {
        LOGGER.info("Routing LCM instantiate request. Helm chart release name: {}", helmChart.getReleaseName());
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        HelmChartType helmChartType = helmChart.getHelmChartType() != null
                ? helmChart.getHelmChartType()
                : HelmChartType.CNF;

        final EvnfmWorkFlowInstantiateRequest instantiateRequest =
                new EvnfmWorkFlowInstantiateRequest
                        .EvnfmWorkFlowInstantiateBuilder(helmChart.getHelmChartUrl(), additionalParams)
                        .withClusterName(vnfInstance.getClusterName())
                        .withLifecycleOperationId(operation.getOperationOccurrenceId())
                        .withLifecycleOperationState(operation.getOperationState().toString())
                        .withOverrideGlobalRegistry(vnfInstance.isOverrideGlobalRegistry())
                        .withDay0Configuration(retrieveDay0ConfigurationParams(vnfInstance))
                        .withChartType(helmChartType)
                        .withChartVersion(helmChart.getHelmChartVersion())
                        .inNamespace(getNamespaceBasedOnChartType(vnfInstance, helmChartType))
                        .withHelmClientVersion(operation.getHelmClientVersion())
                        .build();

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        Path toSecondValuesFile = getSecondValuesFile(tempInstance, helmChart);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(instantiateRequest,
                instantiateRequest.getClusterName(), toValuesFile, toSecondValuesFile);
        final String instantiateUrl = String.format(URL_INSTANTIATE, workflowHost, RESOURCES_URI_V3, helmChart.getReleaseName());

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, instantiateUrl, HttpMethod.POST);

        deleteFile(toSecondValuesFile);
        deleteFile(toValuesFile);
        return response;
    }

    @Override
    public WorkflowRoutingResponse routeInstantiateRequest(final VnfInstance vnfInstance,
                                                           final LifecycleOperation operation,
                                                           final HelmChart helmChart) {
        final VnfInstance upgradedInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        String combinedValuesFile = upgradedInstance.getCombinedValuesFile();
        Map<String, Object> values = new HashMap<>();
        if (StringUtils.isNotEmpty(combinedValuesFile)) {
            values.putAll(convertStringToJSONObj(combinedValuesFile));
        }
        valuesService.updateValuesYamlMapWithReplicaDetails(values, helmChart);
        Path valuesFilePath = workflowRequestBodyBuilder.buildValuesPathFile(values);

        Map<String, Object> additionalParams = new HashMap<>();
        if (!Strings.isNullOrEmpty(operation.getCombinedAdditionalParams())) {
            additionalParams.putAll(getStringObjectMap(operation.getCombinedAdditionalParams()));
        }
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));

        return routeInstantiateRequest(vnfInstance, operation, helmChart, additionalParams, valuesFilePath);
    }

    @Override
    public WorkflowRoutingResponse routeTerminateRequest(final VnfInstance vnfInstance,
                                                         final LifecycleOperation operation,
                                                         final Map<String, Object> additionalParams,
                                                         String releaseName) {
        return routeToEvnfmWFSTerminate(vnfInstance, operation, additionalParams, releaseName);
    }

    @Override
    public WorkflowRoutingResponse routeTerminateRequest(final VnfInstance vnfInstance,
                                                         final LifecycleOperation operation,
                                                         String releaseName) {
        return routeToEvnfmWFSTerminate(vnfInstance, operation, releaseName);
    }

    @Override
    public WorkflowRoutingResponse routeScaleRequest(final VnfInstance vnfInstance,
                                                     final LifecycleOperation operation, final ScaleVnfRequest scaleVnfRequest) {
        return routeToEvnfmWFSScale(vnfInstance, operation);
    }

    @Override
    public WorkflowRoutingResponse routeScaleRequest(final int priority, final LifecycleOperation operation,
                                                     final VnfInstance vnfInstance) {
        return routeToEvnfmWFSScale(priority, operation, vnfInstance);
    }

    @Override
    public WorkflowRoutingResponse routeChangePackageInfoRequest(final ChangeOperationContext context) {
        return routeToEvnfmWfsUpgrade(context, DEFAULT_HIGHEST_PRIORITY_LEVEL);
    }

    @Override
    public WorkflowRoutingResponse routeChangePackageInfoRequest(final ChangeOperationContext context, final int priority) {
        return routeToEvnfmWfsUpgrade(context, priority);
    }

    @Override
    public WorkflowRoutingResponse routeChangePackageInfoRequest(final ChangeOperationContext context, final Path toValuesFile, final int priority) {
        return routeToEvnfmWfsUpgrade(context, toValuesFile, priority);
    }

    @Override
    public WorkflowRoutingResponse routeChangePackageInfoRequest(final int priority,
                                                                 final LifecycleOperation operation,
                                                                 final VnfInstance upgradedInstance) {

        return routeToEvnfmWfsUpgrade(priority, operation, upgradedInstance);
    }

    @Override
    public WorkflowRoutingResponse routeRollbackRequest(final VnfInstance vnfInstance,
                                                        final LifecycleOperation operation, final String releaseName, final String revisionNumber) {
        return routeToEvnfmWFSRollback(vnfInstance, operation, releaseName, revisionNumber);
    }

    @Override
    public ComponentStatusResponse getComponentStatusRequest(final VnfInstance vnfInstance) {
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        try {
            componentStatusResponse = vnfInstance.isRel4() ?
                    routeToEvnfmWfsComponentStatus(vnfInstance, INTERNAL_REL4_RESOURCES_URI) :
                    routeToEvnfmWfsComponentStatus(vnfInstance, INTERNAL_RESOURCES_URI);
        } catch (Exception e) {
            LOGGER.debug("Failed to pull pod information for vnfInstance {}", vnfInstance.getVnfInstanceId(), e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Completed components status response. Response: {}", LoggingUtils.logComponentStatusResponse(componentStatusResponse));
        }
        return componentStatusResponse;
    }

    @Override
    public List<ComponentStatusResponse> getComponentStatusRequest(final List<VnfInstance> vnfInstances) {
        List<ComponentStatusResponse> combinedComponentStatutes = new ArrayList<>();
        try {
            final List<VnfInstance> legacyVnfInstances = vnfInstances.stream()
                    .filter(vnfInstance -> !vnfInstance.isRel4())
                    .collect(Collectors.toList());
            final List<VnfInstance> rel4VnfInstances = vnfInstances.stream()
                    .filter(VnfInstance::isRel4)
                    .collect(Collectors.toList());

            List<ComponentStatusResponse> legacyComponentStatuses = routeToEvnfmWfsComponentStatus(legacyVnfInstances, INTERNAL_RESOURCES_URI);
            List<ComponentStatusResponse> rel4ComponentStatuses = routeToEvnfmWfsComponentStatus(rel4VnfInstances, INTERNAL_REL4_RESOURCES_URI);

            combinedComponentStatutes.addAll(legacyComponentStatuses);
            combinedComponentStatutes.addAll(rel4ComponentStatuses);
        } catch (Exception e) {
            LOGGER.debug("Failed to pull pod information for vnfInstances", e);
        }

        return combinedComponentStatutes;
    }

    @Override
    public boolean isEvnfmNamespaceAndCluster(String namespace, String clusterName) {
        String validateNamespaceUrl = String.format(INTERNAL_NAMESPACES_VALIDATE, namespace, clusterName);
        final String validateNamespaceUri = String.format(BASE_URL, workflowHost, validateNamespaceUrl);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(null, clusterName, null);
        HttpHeaders httpHeaders = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
        try {
            final NamespaceValidationResponse namespaceValidationResponse = restTemplate.exchange(validateNamespaceUri,
                                                                                                  HttpMethod.POST, httpEntity,
                                                                                                  NamespaceValidationResponse.class).getBody();
            return Optional.ofNullable(namespaceValidationResponse)
                    .map(NamespaceValidationResponse::isEvnfmAndClusterNamespace)
                    .orElse(Boolean.FALSE);
        } catch (final HttpStatusCodeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            throw new NamespaceValidationException(WORKFLOW_UNAVAILABLE, e, (HttpStatus) e.getStatusCode());
        } catch (final Exception e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            throw new NamespaceValidationException(WORKFLOW_UNAVAILABLE, e, HttpStatus.BAD_REQUEST);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ResponseEntity<Map> getChartValuesRequest(final VnfInstance vnfInstance, final String releaseName) {
        return routeToEvnfmWFSChartValues(vnfInstance, releaseName);
    }

    @Override
    public void routeDeleteNamespace(final String namespace, final String clusterName, final String releaseName, final String applicationTimeout,
                                     final String lifecycleOperationId) {
        routeToEvnfmWFSDeleteNamespace(namespace, clusterName, releaseName, applicationTimeout, lifecycleOperationId);
    }

    @Override
    public ResponseEntity<Object> routeDeletePvcRequest(final VnfInstance vnfInstance, String releaseName, String lifecycleOperationId,
                                                        String... labels) {
        return routeToEvnfmWFSDeletePvc(vnfInstance, releaseName, lifecycleOperationId, labels);
    }

    @Override
    public ResponseEntity<Object> routeDownsizeRequest(final VnfInstance vnfInstance, final LifecycleOperation operation, final String releaseName) {
        LOGGER.info("Routing LCM downsize request. Release name: {}", releaseName);

        final String downsizeUrl = String.format(BASE_URL, workflowHost, INTERNAL_DOWNSIZE_URI);
        final String operationOccurrenceId = operation.getOperationOccurrenceId();

        EvnfmWorkFlowDownsizeRequest downsizeRequest = new EvnfmWorkFlowDownsizeRequest.EvnfmWorkFlowDownsizeBuilder()
                .withClusterName(vnfInstance.getClusterName())
                .withLifecycleOperationId(operationOccurrenceId)
                .withApplicationTimeout(resolveTimeOut(operation))
                .withReleaseName(releaseName)
                .inNamespace(vnfInstance.getNamespace())
                .build();

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(downsizeRequest,
                                                                                                vnfInstance.getClusterName(),
                                                                                                null);

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operationOccurrenceId);
        return sendWFSRequest(headers, requestBody, downsizeUrl, HttpMethod.POST);
    }

    @Override
    public HelmVersionsResponse getHelmVersionsRequest() {
        String helmVersionsUrl = String.format(BASE_URL, workflowHost, INTERNAL_GET_HELM_VERSIONS);
        ResponseEntity<HelmVersionsResponse> helmVersionsResponse;

        HttpHeaders httpHeaders = createHeadersForGetRequest();

        HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
        try {
            helmVersionsResponse = retryTemplate.execute(context -> restTemplate
                    .exchange(helmVersionsUrl, HttpMethod.GET, httpEntity, HelmVersionsResponse.class));
        } catch (final HttpStatusCodeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e);
            throw new HelmVersionsException("No helm versions found", e,
                                            HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e);
            throw new HelmVersionsException(WORKFLOW_UNAVAILABLE, e, HttpStatus.SERVICE_UNAVAILABLE);
        }

        return helmVersionsResponse.getBody();
    }

    private ComponentStatusResponse routeToEvnfmWfsComponentStatus(final VnfInstance vnfInstance, String uri) {
        ComponentStatusResponse allCombinedComponentResponse = new ComponentStatusResponse();
        allCombinedComponentResponse.setPods(new ArrayList<>());

        String clusterName = vnfInstance.getClusterName();
        String vnfInstanceId = vnfInstance.getVnfInstanceId();
        if (clusterName == null) {
            LOGGER.warn("Cannot get Pods for vnfInstanceId {} as cluster is null", vnfInstanceId);
            return allCombinedComponentResponse;
        }
        if (vnfInstance.getNamespace() == null) {
            LOGGER.warn("Cannot get Pods for vnfInstanceId {} as namespace is null", vnfInstanceId);
            return allCombinedComponentResponse;
        }
        String internalUri = String.format(uri, clusterName);

        List<HelmChart> allCharts = vnfInstance.getHelmCharts();
        List<String> releaseNames = allCharts.stream()
                .map(HelmChartBaseEntity::getReleaseName)
                .collect(Collectors.toList());

        Optional<List<ComponentStatusResponse>> componentStatusResponses =
                routeToEvnfmWfsComponentStatus(releaseNames, clusterName, internalUri);
        componentStatusResponses
                .ifPresent(componentStatusResponseList -> {
                    removeResourcesFromAnotherNamespace(componentStatusResponseList, vnfInstance);
                    flatComponentStatusResponses(allCombinedComponentResponse, componentStatusResponseList);
                });

        return allCombinedComponentResponse;
    }

    private List<ComponentStatusResponse> routeToEvnfmWfsComponentStatus(final List<VnfInstance> vnfInstances, String uri) {
        return vnfInstances.stream()
                .filter(this::isVnfInstanceContainsClusterName)
                .collect(groupingBy(VnfInstance::getClusterName))
                .entrySet()
                .stream()
                .map(clusterAndInstances -> routeToEvnfmWfsComponentStatus(clusterAndInstances.getKey(), clusterAndInstances.getValue(), uri))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<ComponentStatusResponse> routeToEvnfmWfsComponentStatus(final String clusterName,
                                                                         final List<VnfInstance> vnfInstanceList,
                                                                         final String uri) {

        final String internalUri = String.format(uri, clusterName);

        return vnfInstanceList.stream()
                .map(vnfInstance -> routeToEvnfmWfsComponentStatus(vnfInstance, clusterName, internalUri))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<ComponentStatusResponse> routeToEvnfmWfsComponentStatus(final VnfInstance vnfInstance,
                                                                         final String clusterName,
                                                                         final String internalUri) {

        if (vnfInstance.getNamespace() == null) {
            return emptyList();
        }

        List<String> releaseNames = getDistinctReleaseNamesFromVnfInstance(vnfInstance);
        try {
            final List<ComponentStatusResponse> componentStatusResponses =
                    routeToEvnfmWfsComponentStatus(releaseNames, clusterName, internalUri)
                            .orElse(emptyList());
            removeResourcesFromAnotherNamespace(componentStatusResponses, vnfInstance);

            return componentStatusResponses;
        } catch (PodStatusException e) {
            LOGGER.error("Unable to retrieve pod information for instances in cluster {}", clusterName, e);

            return emptyList();
        }
    }

    private void removeResourcesFromAnotherNamespace(List<ComponentStatusResponse> responses, VnfInstance vnfInstance) {
        Objects.requireNonNull(vnfInstance.getNamespace());
        final List<String> instanceNamespaces = List.of(vnfInstance.getNamespace(), vnfInstance.getCrdNamespace());
        for (ComponentStatusResponse response : responses) {
            List<VimLevelAdditionalResourceInfo> pods = response.getPods();
            if (!CollectionUtils.isEmpty(pods)) {
                pods.removeIf(pod -> !instanceNamespaces.contains(pod.getNamespace()));
            }
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> deployments = response.getDeployments();
            if (!CollectionUtils.isEmpty(deployments)) {
                deployments.removeIf(deployment -> !instanceNamespaces.contains(deployment.getNamespace()));
            }
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> statefulSets = response.getStatefulSets();
            if (!CollectionUtils.isEmpty(statefulSets)) {
                statefulSets.removeIf(statefulSet -> !instanceNamespaces.contains(statefulSet.getNamespace()));
            }
            response.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        }
    }

    private void flatComponentStatusResponses(ComponentStatusResponse allCombinedComponentResponse,
                                              List<ComponentStatusResponse> componentStatusResponses) {
        for (ComponentStatusResponse componentStatusResponse : componentStatusResponses) {
            allCombinedComponentResponse.getPods().addAll(componentStatusResponse.getPods());
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> deployments = componentStatusResponse.getDeployments();
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> statefulSets = componentStatusResponse.getStatefulSets();
            if (deployments != null) {
                deployments.forEach(allCombinedComponentResponse::addDeploymentsItem);
            }
            if (statefulSets != null) {
                statefulSets.forEach(allCombinedComponentResponse::addStatefulSetsItem);
            }
        }
    }

    private List<String> getDistinctReleaseNamesFromVnfInstance(VnfInstance vnfInstances) {
        return vnfInstances.getHelmCharts().stream()
                .map(HelmChartBaseEntity::getReleaseName)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isVnfInstanceContainsClusterName(VnfInstance vnfInstance) {
        String clusterName = vnfInstance.getClusterName();
        String vnfInstanceId = vnfInstance.getVnfInstanceId();
        if (clusterName == null) {
            LOGGER.warn("Cannot get Pods for vnfInstanceId {} as cluster is null", vnfInstanceId);
            return false;
        }

        return true;
    }

    private Optional<List<ComponentStatusResponse>> routeToEvnfmWfsComponentStatus(final List<String> releaseNames,
                                                                                   final String clusterName,
                                                                                   final String internalURI) {
        final String podStatusUrl = String.format(BASE_URL, workflowHost, internalURI);
        ResponseEntity<ComponentStatusResponseList> podStatusResponse;

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(releaseNames, clusterName, null);
        HttpHeaders httpHeaders = createHeaders();
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
        try {
            podStatusResponse = restTemplate
                    .exchange(podStatusUrl, HttpMethod.POST, httpEntity, ComponentStatusResponseList.class);
        } catch (final HttpStatusCodeException e) {
            throw new PodStatusException(String.format("No Kubernetes resources found for release names: %s", releaseNames), e,
                                         HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            throw new PodStatusException(WORKFLOW_UNAVAILABLE, e, HttpStatus.SERVICE_UNAVAILABLE);
        }
        LOGGER.info("Completed request to Workflow service. URI: {}, Response code: {}", podStatusUrl, podStatusResponse.getStatusCode());
        final ComponentStatusResponseList componentStatusResponseList = podStatusResponse.getBody();
        Objects.requireNonNull(componentStatusResponseList);
        final List<ComponentStatusResponse> componentStatusResponses = componentStatusResponseList.getComponentStatusResponses();
        return Optional.ofNullable(componentStatusResponses);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> routeToEvnfmWFSChartValues(final VnfInstance vnfInstance, final String releaseName) {
        String clusterName = vnfInstance.getClusterName();
        String namespace = vnfInstance.getNamespace();

        return routeToEvnfmWFSChartValues(releaseName, namespace, clusterName);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> routeToEvnfmWFSChartValues(final String releaseName,
                                                           final String namespace,
                                                           final String clusterName) {
        String internalURI = String.format(INTERNAL_GET_VALUES_URI, releaseName, namespace, clusterName);
        final String getValuesUrl = String.format(BASE_URL, workflowHost, internalURI);
        ResponseEntity<Map> chartValuesResponse;

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(new LinkedMultiValueMap<>(),
                                                                                                clusterName, null);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(requestBody, createHeaders());

        chartValuesResponse = retryTemplate.execute(context -> restTemplate.exchange(getValuesUrl, HttpMethod.POST, httpEntity, Map.class));

        return chartValuesResponse;
    }

    private WorkflowRoutingResponse routeToEvnfmWFSInstantiate(final VnfInstance vnfInstance,
                                                               final LifecycleOperation operation,
                                                               final InstantiateVnfRequest instantiateVnfRequest) {
        LOGGER.info("Routing LCM instantiate request");
        updateOperationStateToProcessing(operation);
        String releaseName = getReleaseName(vnfInstance);
        final EvnfmWorkFlowInstantiateRequest requestBodyDto =
                createInstantiateRequestBody(vnfInstance, operation, instantiateVnfRequest.getAdditionalParams(),
                                             instantiateVnfRequest.getClusterName());
        vnfInstance.setCombinedAdditionalParams(convertObjectToJsonString(requestBodyDto.getAdditionalParams()));
        operation.setCombinedAdditionalParams(convertObjectToJsonString(instantiateVnfRequest.getAdditionalParams()));
        final String instantiateUrl = String.format(URL_INSTANTIATE, workflowHost, RESOURCES_URI_V3, releaseName);
        Path toSecondValuesFile = getSecondValuesFile(vnfInstance, 1);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto,
                instantiateVnfRequest.getClusterName(), null, toSecondValuesFile);

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());

        deleteFile(toSecondValuesFile);
        return sendEvnfmWFSRequest(headers, requestBody, instantiateUrl, HttpMethod.POST);
    }

    private WorkflowRoutingResponse routeToEvnfmWFSInstantiate(final VnfInstance vnfInstance,
                                                               final LifecycleOperation operation,
                                                               final InstantiateVnfRequest instantiateVnfRequest,
                                                               final Path toValuesFile) {
        LOGGER.info("Routing LCM instantiate request with values file");
        updateOperationStateToProcessing(operation);
        String releaseName = getReleaseName(vnfInstance);
        final EvnfmWorkFlowInstantiateRequest requestBodyDto = createInstantiateRequestBody(vnfInstance, operation, instantiateVnfRequest
                .getAdditionalParams(), vnfInstance.getClusterName());
        vnfInstance.setCombinedAdditionalParams(convertObjectToJsonString(requestBodyDto.getAdditionalParams()));
        operation.setCombinedAdditionalParams(convertObjectToJsonString(instantiateVnfRequest.getAdditionalParams()));
        vnfInstance.setCombinedValuesFile(operation.getValuesFileParams());
        final String instantiateUrl = String.format(URL_INSTANTIATE, workflowHost, RESOURCES_URI_V3, releaseName);
        Path toSecondValuesFile = getSecondValuesFile(vnfInstance, 1);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto, requestBodyDto.getClusterName(),
                                                                                                toValuesFile, toSecondValuesFile);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());

        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, instantiateUrl, HttpMethod.POST);

        deleteFile(toValuesFile);
        deleteFile(toSecondValuesFile);
        return response;
    }

    private WorkflowRoutingResponse routeToEvnfmWFSInstantiate(final int priority,
                                                               final LifecycleOperation operation,
                                                               final VnfInstance vnfInstance) {
        LOGGER.info("Routing LCM instantiate request. Helm chart priority: {}", priority);
        final EvnfmWorkFlowInstantiateRequest requestBodyDto = createInstantiateRequestBody(operation, priority);
        final String instantiateUrl = String.format(URL_INSTANTIATE, workflowHost, RESOURCES_URI_V3, getReleaseName(vnfInstance, priority));
        String combinedValuesFile = operation.getValuesFileParams();
        Map<String, Object> valuesParameter = new HashMap<>();
        if (StringUtils.isNotEmpty(combinedValuesFile)) {
            valuesParameter.putAll(convertStringToJSONObj(combinedValuesFile));
        }
        if (!Strings.isNullOrEmpty(vnfInstance.getTempInstance())) {
            VnfInstance tempVnfInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            valuesService.updateValuesMapWithReplicaDetailsFromTempInstance(valuesParameter, tempVnfInstance, priority);
        }

        Path valuesFilePath = workflowRequestBodyBuilder.buildValuesPathFile(valuesParameter);
        Path toSecondValuesFile = getSecondValuesFile(vnfInstance, priority);
        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto,
                requestBodyDto.getClusterName(), valuesFilePath, toSecondValuesFile);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());

        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, instantiateUrl, HttpMethod.POST);
        deleteFile(valuesFilePath);
        deleteFile(toSecondValuesFile);
        return response;
    }

    private void routeToEvnfmWFSDeleteNamespace(final String namespace, final String clusterName, final String releaseName,
                                                final String applicationTimeOut, final String lifecycleOperationId) {
        LOGGER.info("Routing LCM delete namespace request. Namespace: {}, cluster name: {}, release name: {}",
                    namespace, clusterName, releaseName);
        final String internalURI = String.format(INTERNAL_NAMESPACE_URI,
                                                 namespace,
                                                 clusterName,
                                                 releaseName,
                                                 applicationTimeOut,
                                                 lifecycleOperationId);
        final String deleteNamespaceUrl = String.format(BASE_URL, workflowHost, internalURI);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(Collections.emptyMap(),
                                                                                                clusterName, null);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, lifecycleOperationId);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            restTemplate.postForEntity(deleteNamespaceUrl, requestEntity, Object.class, Collections.emptyMap());
        } catch (final HttpStatusCodeException e) {
            throw new DeleteNamespaceException(e.getMessage(), e, (HttpStatus) e.getStatusCode());
        } catch (final Exception e) {
            throw new DeleteNamespaceException(WORKFLOW_UNAVAILABLE, e, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private WorkflowRoutingResponse routeToEvnfmWFSTerminate(final VnfInstance vnfInstance,
                                                             final LifecycleOperation operation,
                                                             final Map<String, Object> additionalParams,
                                                             final String releaseName) {
        LOGGER.info("Routing LCM Terminate request with additional params. Release name: {}", releaseName);
        String terminateUrl = createTerminateRequest(vnfInstance, operation, additionalParams, releaseName);

        MultiValueMap<String, Object> body = workflowRequestBodyBuilder.buildRequestBody(Collections.emptyMap(),
                                                                                         vnfInstance.getClusterName(), null);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        return sendEvnfmWFSRequest(headers, body, terminateUrl, HttpMethod.POST);
    }

    private WorkflowRoutingResponse routeToEvnfmWFSTerminate(final VnfInstance vnfInstance,
                                                             final LifecycleOperation operation,
                                                             final String releaseName) {
        LOGGER.info("Routing LCM Terminate request by LCM operation. Release name: {}", releaseName);
        Map<String, Object> additionalParams = new HashMap<>();
        if (!Strings.isNullOrEmpty(operation.getCombinedAdditionalParams())) {
            additionalParams.putAll(getStringObjectMap(operation.getCombinedAdditionalParams()));
        }
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));

        String terminateUrl = createTerminateRequest(vnfInstance, operation, additionalParams, releaseName);

        MultiValueMap<String, Object> body = workflowRequestBodyBuilder.buildRequestBody(Collections.emptyMap(),
                                                                                         vnfInstance.getClusterName(), null);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        return sendEvnfmWFSRequest(headers, body, terminateUrl, HttpMethod.POST);
    }

    private ResponseEntity<Object> routeToEvnfmWFSDeletePvc(final VnfInstance vnfInstance, final String releaseName, String lifecycleOperationId,
                                                            final String... labels) {
        LOGGER.info("Routing LCM delete PVC request. Release name: {}", releaseName);
        String request = getDeletePvcRequest(vnfInstance, releaseName, labels);
        MultiValueMap<String, Object> body = workflowRequestBodyBuilder.buildRequestBody(Collections.emptyMap(),
                                                                                         vnfInstance.getClusterName(), null);

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, lifecycleOperationId);
        return sendWFSRequest(headers, body, request, HttpMethod.POST);
    }

    private String createTerminateRequest(final VnfInstance vnfInstance, final LifecycleOperation operation,
                                          final Map<String, Object> additionalParams, final String releaseName) {
        return getTerminateRequest(vnfInstance, operation, additionalParams, releaseName);
    }

    @VisibleForTesting
    String getTerminateRequest(final VnfInstance vnfInstance, final LifecycleOperation operation,
                               final Map<String, Object> additionalParams, final String releaseName) {
        StringBuilder requestBuilder = new StringBuilder(
                String.format(TERMINATE_URL, workflowHost, RESOURCES_URI_V3, releaseName));
        updateHelmChartState(vnfInstance, releaseName);
        EvnfmWorkFlowTerminateRequest terminateRequest =
                new EvnfmWorkFlowTerminateRequest.EvnfmWorkFlowTerminateBuilder(
                        additionalParams).withClusterName(vnfInstance.getClusterName())
                        .withLifecycleOperationId(operation.getOperationOccurrenceId())
                        .withState(operation.getOperationState().toString())
                        .withHelmClientVersion(operation.getHelmClientVersion())
                        .inNamespace(vnfInstance.getNamespace())
                        .build();
        Map<String, Object> additionalParamsMap = terminateRequest.getAdditionalParams();
        return getRequestParamsAsString(requestBuilder, additionalParamsMap);
    }

    @VisibleForTesting
    String getDeletePvcRequest(final VnfInstance vnfInstance, final String releaseName, String... labels) {
        StringBuilder requestBuilder = new StringBuilder(
                String.format(DELETE_PVCS_URL, workflowHost, INTERNAL_DELETE_PVC_URI, releaseName));
        LifecycleOperation operation = databaseInteractionService
                .getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
        updateHelmChartsDeletePvcState(vnfInstance, operation, releaseName, LifecycleOperationState.PROCESSING.toString());
        EvnfmWorkFlowDeletePvcRequest deletePvcRequest = new EvnfmWorkFlowDeletePvcRequest
                .EvnfmWorkFlowDeletePvcBuilder(resolveTimeOut(operation))
                .withClusterName(vnfInstance.getClusterName())
                .withLifecycleOperationId(operation.getOperationOccurrenceId())
                .withLabels(labels)
                .withState(operation.getOperationState().toString())
                .inNamespace(vnfInstance.getNamespace()).build();
        Map<String, Object> additionalParamsMap = deletePvcRequest.getAdditionalParams();
        return getRequestParamsAsString(requestBuilder, additionalParamsMap);
    }

    private static String getRequestParamsAsString(final StringBuilder requestBuilder,
                                                   final Map<String, Object> additionalParamsMap) {
        if (additionalParamsMap.isEmpty()) {
            return requestBuilder.toString();
        }
        requestBuilder.append("?");
        additionalParamsMap.forEach(((key, value) -> provideArgument(requestBuilder, key, value)));
        String request = requestBuilder.toString();
        return request.substring(0, request.length() - 1);
    }

    private WorkflowRoutingResponse routeToEvnfmWFSScale(final VnfInstance vnfInstance,
                                                         final LifecycleOperation operation) {
        try {
            LOGGER.info("Routing LCM Scale request");
            updateOperationStateToProcessing(operation);
            HelmChart helmChart = getEnabledNotCrdAndNotProcessedCnfChartWithHighestPriority(parseJson(vnfInstance.getTempInstance(),
                                                                           VnfInstance.class));
            final EvnfmWorkflowScaleRequest requestBody = createScaleRequestBody(vnfInstance, helmChart.getPriority(), operation);
            return getScaleWorkflowRoutingResponse(vnfInstance, requestBody, operation.getOperationOccurrenceId(), helmChart.getPriority());
        } catch (IllegalArgumentException | InternalRuntimeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            return getWorkflowResponseForException(e.getMessage());
        }
    }

    private WorkflowRoutingResponse routeToEvnfmWFSScale(final int priority, final LifecycleOperation operation,
                                                         final VnfInstance scaledInstance) {
        try {
            LOGGER.info("Routing LCM Scale request. Helm chart priority: {}", priority);
            final EvnfmWorkflowScaleRequest requestBody = createScaleRequestBody(scaledInstance, priority, operation);
            return getScaleWorkflowRoutingResponse(scaledInstance, requestBody, operation.getOperationOccurrenceId(), priority);
        } catch (IllegalArgumentException | InternalRuntimeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            return getWorkflowResponseForException(e.getMessage());
        }
    }

    private static WorkflowRoutingResponse getWorkflowResponseForException(String errorMessage) {
        WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage(errorMessage);
        response.setHttpStatus(HttpStatus.BAD_REQUEST);
        return response;
    }

    private WorkflowRoutingResponse getScaleWorkflowRoutingResponse(final VnfInstance vnfInstance,
                                                                    final EvnfmWorkflowScaleRequest requestBodyDto,
                                                                    final String lifecycleOperationId,
                                                                    final int priority) {
        LOGGER.debug("Scale Workflow Request :: {}", logWfsRequest(requestBodyDto));
        Path toValuesFile = getCombinedAdditionalValues(vnfInstance);
        Path toSecondValuesFile = getSecondValuesFile(vnfInstance, priority);
        final String scaleUrl = String.format(URL_SCALE, workflowHost, RESOURCES_URI_V3, getReleaseName(vnfInstance, priority));
        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto, vnfInstance.getClusterName(),
                                                                                                toValuesFile, toSecondValuesFile);

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, lifecycleOperationId);
        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, scaleUrl, HttpMethod.POST);
        deleteFile(toValuesFile);
        deleteFile(toSecondValuesFile);
        return response;
    }

    private EvnfmWorkflowScaleRequest createScaleRequestBody(final VnfInstance vnfInstance,
                                                             final int priority,
                                                             final LifecycleOperation operation) {
        return getScaleRequest(priority, operation, vnfInstance);
    }

    private Map<String, Object> getAdditionalParameter(VnfInstance instance) {
        Map<String, Object> additionalParam = new HashMap<>();
        if (!Strings.isNullOrEmpty(instance.getCombinedAdditionalParams())) {
            try {
                additionalParam = mapper.readValue(instance.getCombinedAdditionalParams(), new TypeReference<>() {
                });
            } catch (JsonProcessingException jpe) {
                throw new IllegalArgumentException("Invalid additional parameter, Failed while casting additional " +
                                                           "parameter to map ", jpe);
            }
        }
        return additionalParam;
    }

    private EvnfmWorkflowScaleRequest getScaleRequest(final int priority, final LifecycleOperation operation, final VnfInstance vnfInstance) {
        String tempInstanceString = operation.getVnfInstance().getTempInstance();
        VnfInstance tempInstance = parseJson(tempInstanceString, VnfInstance.class);
        Map<String, Object> additionalParams = getAdditionalParameter(tempInstance);
        HelmChart helmChart = getHelmChartByPriority(tempInstance, priority);
        helmChart.setState(operation.getOperationState().toString());
        vnfInstance.setTempInstance(convertObjectToJsonString(tempInstance));
        Map<String, Object> autoScalingEnabledParameter = scaleService.getAutoScalingEnabledParameter(helmChart);
        if (!CollectionUtils.isEmpty(autoScalingEnabledParameter)) {
            additionalParams.putAll(autoScalingEnabledParameter);
        }
        Map<String, Map<String, Integer>> scaleResources = scaleService.getScaleResourcesFromChart(helmChart);
        return new EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder(additionalParams,
                                                                       helmChart.getHelmChartUrl())
                .withClusterName(vnfInstance.getClusterName()).withScaleResources(scaleResources)
                .withLifecycleOperationId(operation.getOperationOccurrenceId())
                .withLifecycleOperationState(operation.getOperationState().toString())
                .withHelmClientVersion(vnfInstance.getHelmClientVersion())
                .inNamespace(vnfInstance.getNamespace())
                .withOverrideGlobalRegistry(vnfInstance.isOverrideGlobalRegistry()).build();
    }

    public WorkflowRoutingResponse routeToEvnfmWfsUpgrade(final ChangeOperationContext context, final int priority) {
        LOGGER.info("Routing LCM CCVP request");

        final Path combinedValuesFile = getCombinedAdditionalValues(context.getTempInstance());

        final WorkflowRoutingResponse response = routeToEvnfmWFSUpgradeWithCombinedValuesFile(context, combinedValuesFile, priority);

        deleteFile(combinedValuesFile);

        return response;
    }

    public WorkflowRoutingResponse routeToEvnfmWfsUpgrade(final ChangeOperationContext context, final Path valuesFile, final int priority) {
        LOGGER.info("Routing LCM CCVP request with values file");

        final Path combinedValuesFile = mergeValuesWithPreviousIfRequired(context.getTempInstance(), valuesFile, context.getAdditionalParams());

        final WorkflowRoutingResponse response = routeToEvnfmWFSUpgradeWithCombinedValuesFile(context, combinedValuesFile, priority);

        deleteFile(combinedValuesFile);
        deleteFile(valuesFile);

        return response;
    }

    private WorkflowRoutingResponse routeToEvnfmWfsUpgrade(final int priority, final LifecycleOperation operation,
                                                           final VnfInstance upgradedInstance) {
        LOGGER.info("Routing LCM CCVP request. Helm chart priority: {}", priority);
        if (!LifecycleOperationState.ROLLING_BACK.equals(operation.getOperationState())) {
            updateOperationStateToProcessing(operation);
        }
        final EvnfmWorkFlowUpgradeRequest requestBodyDto = getEvnfmWorkFlowUpgradeRequest(operation, priority, upgradedInstance);
        String combinedValuesFile = upgradedInstance.getCombinedValuesFile();
        Map<String, Object> values = new HashMap<>();
        if (StringUtils.isNotEmpty(combinedValuesFile)) {
            values.putAll(convertStringToJSONObj(combinedValuesFile));
        }
        valuesService.updateValuesMapWithReplicaDetailsFromTempInstance(values, upgradedInstance, priority);

        Path valuesFilePath = workflowRequestBodyBuilder.buildValuesPathFile(values);
        Path toSecondValuesFile = getSecondValuesFile(upgradedInstance, priority);
        final String upgradeUrl = String.format(URL_UPGRADE, workflowHost, RESOURCES_URI_V3,
                                                getReleaseName(upgradedInstance, priority));

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto, requestBodyDto.getClusterName(),
                                                                                                valuesFilePath, toSecondValuesFile);

        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, upgradeUrl, HttpMethod.POST);

        deleteFile(valuesFilePath);
        deleteFile(toSecondValuesFile);
        return response;
    }

    public WorkflowRoutingResponse routeToEvnfmWfsUpgrade(LifecycleOperation operation,
                                                          HelmChart currentHelmChart,
                                                          Map<String, Object> additionalParams,
                                                          Path valuesFile) {
        LOGGER.info("Routing LCM CCVP request with values file");

        if (!LifecycleOperationState.ROLLING_BACK.equals(operation.getOperationState())) {
            updateOperationStateToProcessing(operation);
        }
        operation.setCombinedAdditionalParams(convertObjectToJsonString(additionalParams));

        final HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        HelmChartType helmChartType = currentHelmChart.getHelmChartType() != null
                ? currentHelmChart.getHelmChartType()
                : HelmChartType.CNF;

        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));

        VnfInstance originalInstance = operation.getVnfInstance();
        final VnfInstance tempInstance = parseJson(originalInstance.getTempInstance(), VnfInstance.class);

        Map<String, Object> valuesYamlMap = YamlUtility.convertYamlFileIntoMap(valuesFile);
        valuesService.updateValuesMapWithReplicaDetailsFromTempInstance(valuesYamlMap, currentHelmChart);
        Path valuesFileWithReplicaDetails = YamlUtility.writeMapToValuesFile(valuesYamlMap);

        final Path values = mergeValuesWithPreviousIfRequired(tempInstance, valuesFileWithReplicaDetails, additionalParams);
        getValuesString(values).ifPresent(tempInstance::setCombinedValuesFile);

        final EvnfmWorkFlowUpgradeRequest wfsUpgradeRequest = new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder(
                currentHelmChart.getHelmChartUrl(), additionalParams)
                .withClusterName(tempInstance.getClusterName())
                .withLifecycleOperationId(operation.getOperationOccurrenceId())
                .withLifecycleOperationState(operation.getOperationState().toString())
                .inNamespace(getNamespaceBasedOnChartType(tempInstance, helmChartType))
                .withOverrideGlobalRegistry(tempInstance.isOverrideGlobalRegistry())
                .withChartType(helmChartType)
                .withChartVersion(currentHelmChart.getHelmChartVersion())
                .withHelmClientVersion(operation.getHelmClientVersion())
                .build();

        Path secondValuesFile = getSecondValuesFile(tempInstance, currentHelmChart.getPriority());
        tempInstance.setCombinedAdditionalParams(convertObjectToJsonString(additionalParams));
        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(wfsUpgradeRequest, wfsUpgradeRequest.getClusterName(),
                values, secondValuesFile);

        final String upgradeUrl = String.format(URL_UPGRADE, workflowHost, RESOURCES_URI_V3, currentHelmChart.getReleaseName());
        WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, upgradeUrl, HttpMethod.POST);
        originalInstance.setTempInstance(convertObjectToJsonString(tempInstance));

        deleteFile(valuesFile);
        deleteFile(valuesFileWithReplicaDetails);
        deleteFile(secondValuesFile);
        deleteFile(values);

        return response;
    }

    public WorkflowRoutingResponse routeToEvnfmWfsFakeUpgrade(final VnfInstance originalInstance,
                                                              final LifecycleOperation operation,
                                                              final HelmChart currentHelmChart,
                                                              final Map<String, Object> additionalParams,
                                                              final Path toValuesFile) {
        LOGGER.info("Routing LCM fake upgrade request with values file. Helm chart release name: {}", currentHelmChart.getReleaseName());

        WorkflowRoutingResponse response;
        final HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());
        HelmChartType helmChartType = currentHelmChart.getHelmChartType() != null
                ? currentHelmChart.getHelmChartType()
                : HelmChartType.CNF;
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        final EvnfmWorkFlowUpgradeRequest wfsUpgradeRequest = new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder(
                currentHelmChart.getHelmChartUrl(), additionalParams)
                .withClusterName(originalInstance.getClusterName())
                .withLifecycleOperationId(operation.getOperationOccurrenceId())
                .withLifecycleOperationState(operation.getOperationState().toString())
                .inNamespace(getNamespaceBasedOnChartType(originalInstance, helmChartType))
                .withOverrideGlobalRegistry(originalInstance.isOverrideGlobalRegistry())
                .withChartType(helmChartType)
                .withChartVersion(currentHelmChart.getHelmChartVersion())
                .withHelmClientVersion(operation.getHelmClientVersion())
                .build();
        Path toSecondValuesFile = getSecondValuesFile(originalInstance, currentHelmChart.getPriority());
        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(wfsUpgradeRequest, wfsUpgradeRequest.getClusterName(),
                                                                                                toValuesFile, toSecondValuesFile);
        final String fakeUpgradeUrl = String.format(URL_UPGRADE, workflowHost, RESOURCES_URI_V3, currentHelmChart.getReleaseName());
        response = sendEvnfmWFSRequest(headers, requestBody, fakeUpgradeUrl, HttpMethod.POST);
        deleteFile(toValuesFile);
        deleteFile(toSecondValuesFile);
        return response;
    }

    private WorkflowRoutingResponse routeToEvnfmWFSUpgradeWithCombinedValuesFile(final ChangeOperationContext context,
                                                                                 final Path combinedValuesFile,
                                                                                 final int priority) {

        final VnfInstance vnfInstance = context.getTempInstance();

        getValuesString(combinedValuesFile).ifPresent(vnfInstance::setCombinedValuesFile);

        final LifecycleOperation operation = context.getOperation();

        if (!LifecycleOperationState.ROLLING_BACK.equals(operation.getOperationState())) {
            updateOperationStateToProcessing(operation);
        }

        final EvnfmWorkFlowUpgradeRequest requestBodyDto = createUpgradeRequestBody(context, priority);

        final String upgradeUrl = String.format(URL_UPGRADE, workflowHost, RESOURCES_URI_V3, getReleaseName(vnfInstance, priority));

        vnfInstance.setCombinedAdditionalParams(convertObjectToJsonString(requestBodyDto.getAdditionalParams()));
        operation.setCombinedAdditionalParams(convertObjectToJsonString(context.getAdditionalParams()));

        final Path secondValuesFile = getSecondValuesFile(vnfInstance, priority);

        final MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto,
                                                                                                      requestBodyDto.getClusterName(),
                                                                                                      combinedValuesFile,
                                                                                                      secondValuesFile);

        final HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());

        final WorkflowRoutingResponse response = sendEvnfmWFSRequest(headers, requestBody, upgradeUrl, HttpMethod.POST);

        deleteFile(secondValuesFile);

        return response;
    }

    @VisibleForTesting
    EvnfmWorkFlowUpgradeRequest getEvnfmWorkFlowUpgradeRequest(final LifecycleOperation operation,
                                                               final int priority,
                                                               final VnfInstance tempInstance) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        if (!Strings.isNullOrEmpty(operation.getCombinedAdditionalParams())) {
            additionalParams.putAll(getStringObjectMap(operation.getCombinedAdditionalParams()));
        }
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));

        HelmChart helmChart = getHelmChartByPriority(tempInstance, priority);
        helmChart.setState(operation
                                   .getOperationState()
                                   .toString());
        vnfInstance.setTempInstance(convertObjectToJsonString(tempInstance));
        HelmChartType helmChartType = helmChart.getHelmChartType() != null
                ? helmChart.getHelmChartType()
                : HelmChartType.CNF;
        return new EvnfmWorkFlowUpgradeRequest.EvnfmWorkFlowUpgradeBuilder(helmChart.getHelmChartUrl(),
                                                                           additionalParams)
                .withClusterName(vnfInstance.getClusterName())
                .withLifecycleOperationId(vnfInstance.getOperationOccurrenceId())
                .withLifecycleOperationState(operation.getOperationState().toString())
                .inNamespace(getNamespaceBasedOnChartType(tempInstance, helmChartType))
                .withOverrideGlobalRegistry(vnfInstance.isOverrideGlobalRegistry())
                .withChartType(helmChartType)
                .withChartVersion(helmChart.getHelmChartVersion())
                .withHelmClientVersion(tempInstance.getHelmClientVersion())
                .build();
    }

    private WorkflowRoutingResponse routeToEvnfmWFSRollback(final VnfInstance vnfInstance,
                                                            final LifecycleOperation operation,
                                                            final String releaseName,
                                                            final String revisionNumber) {
        LOGGER.info("Routing LCM rollback request. Release name: {}, revision number: {}", releaseName, revisionNumber);
        updateOperationStateToProcessing(operation);
        final EvnfmWorkFlowRollbackRequest requestBodyDto = createRollbackRequestBody(vnfInstance, operation, revisionNumber);
        final String rollBackUrl = String
                .format(URL_ROLLBACK, workflowHost, RESOURCES_URI_V3, releaseName);

        MultiValueMap<String, Object> requestBody = workflowRequestBodyBuilder.buildRequestBody(requestBodyDto,
                                                                                                requestBodyDto.getClusterName(), null);
        HttpHeaders headers = createHeaders();
        addIdempotencyHeader(headers, operation.getOperationOccurrenceId());

        return sendEvnfmWFSRequest(headers, requestBody, rollBackUrl, HttpMethod.POST);
    }

    private <T> WorkflowRoutingResponse sendEvnfmWFSRequest(final HttpHeaders headers, final T requestBody,
                                                            final String url, final HttpMethod httpMethod) {
        final HttpEntity<T> request = new HttpEntity<>(requestBody, headers);
        WorkflowRoutingResponse workflowRoutingResponse;
        try {
            LOGGER.info("Request to Workflow service url {}, method {} request: <{},{}>",
                        url, httpMethod, logWfsRequest(requestBody), headers);

            final ResponseEntity<ResourceResponse> response = restTemplate
                    .exchange(url, httpMethod, request, ResourceResponse.class);
            workflowRoutingResponse = createResponse(response);
        } catch (final HttpStatusCodeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            workflowRoutingResponse = getWorkflowRoutingErrorResponse(e);
        } catch (final Exception e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            workflowRoutingResponse = getWorkflowServiceUnavailableResponse();
        }
        LOGGER.info("Completed request to Workflow service. URI: {}, Response code: {}", url, workflowRoutingResponse.getHttpStatus());
        return workflowRoutingResponse;
    }

    private <T> ResponseEntity<Object> sendWFSRequest(final HttpHeaders headers, final T requestBody,
                                                      final String url, final HttpMethod httpMethod) {
        final HttpEntity<T> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Object> response;
        try {
            LOGGER.info("Request to Workflow service. URI: {}, method: {} request: <{},{}>",
                        url, httpMethod, logWfsRequest(requestBody), headers);
            response = restTemplate
                    .exchange(url, httpMethod, request, Object.class);
        } catch (final HttpStatusCodeException e) {
            LOGGER.error(EXCEPTION_FROM_WORKFLOW_SERVICE, e.getMessage(), e);
            response = new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
        LOGGER.info("Completed request to Workflow service. URI: {}, Response code: {}", url, response.getStatusCode());
        return response;
    }

    private static <T> String logWfsRequest(@Nullable T request) {
        if (request instanceof EvnfmWorkFlowInstantiateRequest) {
            EvnfmWorkFlowInstantiateRequest instantiateRequest = (EvnfmWorkFlowInstantiateRequest) request;
            return logWfsRequestWithDay0Configuration(request, checkAndCastObjectToMap(instantiateRequest.getDay0Configuration()));
        } else if (request instanceof EvnfmWorkflowScaleRequest) {
            EvnfmWorkflowScaleRequest scaleRequest = (EvnfmWorkflowScaleRequest) request;
            return logWfsRequestWithDay0Configuration(request, checkAndCastObjectToMap(scaleRequest.getAdditionalParams()));
        } else if (request instanceof EvnfmWorkFlowTerminateRequest) {
            EvnfmWorkFlowTerminateRequest terminateRequest = (EvnfmWorkFlowTerminateRequest) request;
            return logWfsRequestWithDay0Configuration(request, terminateRequest.getAdditionalParams());
        } else if (request instanceof EvnfmWorkFlowUpgradeRequest) {
            EvnfmWorkFlowUpgradeRequest upgradeRequest = (EvnfmWorkFlowUpgradeRequest) request;
            return logWfsRequestWithDay0Configuration(request, upgradeRequest.getAdditionalParams());
        } else if (request instanceof MultiValueMap) {
            MultiValueMap<String, Object> requestMap = (MultiValueMap<String, Object>) request;
            return logMultiValueMap(requestMap);
        }
        return Objects.toString(request, NULL_DEFAULT_STRING);
    }

    public String convertObjectToJsonString(Object map) {
        try {
            return map != null ? mapper.writeValueAsString(map) : null;
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to convert the java object {} to json. Failed with exception {} ", map,
                        e.getMessage());
        }
        return null;
    }

    private Path getCombinedAdditionalValues(VnfInstance vnfInstance) {
        try {
            return valuesService.getCombinedAdditionalValuesFile(vnfInstance);
        } catch (Exception e) {
            saveOperationErrorDetails(vnfInstance, e.getMessage());
            throw new InvalidInputException(String.format("Failed to get combined additional values file due to: %s", e.getMessage()), e);
        }
    }

    private Path getSecondValuesFile(VnfInstance vnfInstance, int priority) {
        try {
            return valuesService.getChartSpecificValues(vnfInstance, priority);
        } catch (Exception e) {
            saveOperationErrorDetails(vnfInstance, e.getMessage());
            throw new InvalidInputException(String.format("failed to load values: %s", e.getMessage()), e);
        }
    }


    private Path getSecondValuesFile(VnfInstance vnfInstance, HelmChart chart) {
        try {
            return valuesService.getChartSpecificValues(vnfInstance, chart);
        } catch (Exception e) {
            saveOperationErrorDetails(vnfInstance, e.getMessage());
            throw new InvalidInputException(String.format("failed to load values: %s", e.getMessage()), e);
        }
    }

    private void saveOperationErrorDetails(VnfInstance vnfInstance, String message) {
        LifecycleOperation operation = databaseInteractionService
                .getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
        updateOperationState(operation, LifecycleOperationState.FAILED);
        setError(message, operation);
        databaseInteractionService.persistLifecycleOperation(operation);
    }

    private Path mergeValuesWithPreviousIfRequired(final VnfInstance vnfInstance,
                                                   final Path toValuesFile,
                                                   final Map<String, Object> additionalParams) {

        final boolean skipMergingPreviousValues = parameterPresent(additionalParams, SKIP_MERGING_PREVIOUS_VALUES)
                && getBooleanValue(additionalParams.get(SKIP_MERGING_PREVIOUS_VALUES));
        LOGGER.info("Skipping merging previous values: {}", skipMergingPreviousValues);

        // getting params from previous instance
        final Path toCombinedValuesFile = getCombinedAdditionalValues(vnfInstance);

        final Path path = toCombinedValuesFile != null && !skipMergingPreviousValues
                ? getCombinedValuesPath(toCombinedValuesFile, toValuesFile, vnfInstance)
                : toValuesFile;

        deleteFile(toCombinedValuesFile);

        return path;
    }

    private Path getCombinedValuesPath(Path toCombinedValuesFile, Path toValuesFile, VnfInstance vnfInstance) {
        try {
            return mergeValuesFilesAndSave(toCombinedValuesFile, toValuesFile);
        } catch (Exception e) {
            saveOperationErrorDetails(vnfInstance, e.getMessage());
            throw new InvalidInputException(String.format("Failed to get combined additional values file due to: %s", e.getMessage()), e);
        }
    }

    private static Path mergeValuesFilesAndSave(final Path toCombinedValuesFile, final Path toValuesFile) {
        FileSystemResource combinedResource = new FileSystemResource(toCombinedValuesFile.toString());
        FileSystemResource requestResource = new FileSystemResource(toValuesFile.toString());
        YamlMapFactoryBean factoryBean = new YamlMapFactoryBean();
        factoryBean.setResolutionMethod(YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE);
        factoryBean.setSingleton(false);
        factoryBean.setResources(combinedResource, requestResource);
        Map<String, Object> combinedFiles = factoryBean.getObject();

        return writeMapToValuesFile(combinedFiles);
    }
}
