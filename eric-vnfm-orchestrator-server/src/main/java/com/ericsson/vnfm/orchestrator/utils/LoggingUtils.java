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
package com.ericsson.vnfm.orchestrator.utils;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder.WorkflowRequestBodyBuilder.JSON_REQUEST_PARAMETER_NAME;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.util.MultiValueMap;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageInfoVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoDeploymentStatefulSet;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import jakarta.annotation.Nullable;

/**
 * Utility class for logging helping utility methods.
 * <br><br>
 * Please consider using methods of this class instead of toString()
 * when log complex data structures like {@link LifecycleOperation}.
 */
public final class LoggingUtils {

    public static final String NULL_DEFAULT_STRING = "null";

    private static final String SENSITIVE_DATA_REPLACEMENT = "******";
    private static final String OPERATION_PARAMS_FIELD = "operationParams";
    private static final String VNF_INSTANCE_FIELD = "vnfInstance";
    private static final String ADDITIONAL_PARAMS_FIELD = "additionalParams";
    private static final String DAY0_CONFIG_PARAMS_VALUE_SUFFIX = ".value";
    private static final String DAY0_CONFIGURATION_FIELD = "day0Configuration";
    private static final String SENSITIVE_PARAM_CRITERIA = "password";
    private static final String VNF_INSTANCE_OPERATIONS_FIELD = "allOperations";

    private LoggingUtils() {
    }

    public static String logLifecycleOperation(@Nullable LifecycleOperation lifecycleOperation) {
        if (Objects.isNull(lifecycleOperation)) {
            return NULL_DEFAULT_STRING;
        }
        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(lifecycleOperation, SHORT_PREFIX_STYLE);
        toStringBuilder.setExcludeFieldNames(OPERATION_PARAMS_FIELD, VNF_INSTANCE_FIELD);
        toStringBuilder.append(OPERATION_PARAMS_FIELD, getLoggableOperationParams(lifecycleOperation.getOperationParams()));

        VnfInstance vnfInstance = lifecycleOperation.getVnfInstance();
        String loggableVnfInstance = Objects.isNull(vnfInstance) ? NULL_DEFAULT_STRING : vnfInstance.getVnfInstanceId();
        toStringBuilder.append(VNF_INSTANCE_FIELD, loggableVnfInstance);
        return toStringBuilder.build();
    }

    public static String logLifecycleOperations(@Nullable List<LifecycleOperation> lifecycleOperations) {
        if (Objects.isNull(lifecycleOperations)) {
            return NULL_DEFAULT_STRING;
        }
        StringBuilder stringBuilder = new StringBuilder();
        lifecycleOperations.forEach(lo -> stringBuilder.append(logLifecycleOperation(lo)).append(", "));
        return stringBuilder.toString();
    }

    private static Object getLoggableOperationParams(String operationParams) {
        if (operationParams != null) {
            Map<String, Object> loggableOperationParams = convertStringToJSONObj(operationParams);
            if (loggableOperationParams != null) {
                extractAdditionalParams(loggableOperationParams).entrySet().forEach(LoggingUtils::hideSensitiveData);
                return loggableOperationParams;
            }
        }
        return NULL_DEFAULT_STRING;
    }

    public static String logVnfInstance(@Nullable VnfInstance vnfInstance) {
        if (Objects.isNull(vnfInstance)) {
            return NULL_DEFAULT_STRING;
        }

        if (vnfInstance.getAllOperations() == null || vnfInstance.getAllOperations().isEmpty()) {
            return vnfInstance.toString();
        }

        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(vnfInstance, SHORT_PREFIX_STYLE);
        toStringBuilder.setExcludeFieldNames(VNF_INSTANCE_OPERATIONS_FIELD);
        toStringBuilder.append(VNF_INSTANCE_OPERATIONS_FIELD, logLifecycleOperations(vnfInstance.getAllOperations()));
        return toStringBuilder.build();
    }

    public static <T> String logLifecycleOperationRequest(@Nullable T request) {
        if (request == null) {
            return NULL_DEFAULT_STRING;
        }

        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(request, SHORT_PREFIX_STYLE);
        toStringBuilder.setExcludeFieldNames(ADDITIONAL_PARAMS_FIELD);
        toStringBuilder.append(ADDITIONAL_PARAMS_FIELD, extractAdditionalParams(request));
        return toStringBuilder.build();
    }

    public static String logWfsRequestWithDay0Configuration(@Nullable Object request, Map<String, Object> day0Configuration) {
        if (Objects.isNull(request)) {
            return NULL_DEFAULT_STRING;
        }
        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(request, SHORT_PREFIX_STYLE);
        toStringBuilder.setExcludeFieldNames(DAY0_CONFIGURATION_FIELD);
        if (MapUtils.isNotEmpty(day0Configuration)) {
            toStringBuilder.append(DAY0_CONFIGURATION_FIELD, SENSITIVE_DATA_REPLACEMENT);
        }
        return toStringBuilder.build();
    }

    public static String logHelmCharts(@Nullable List<HelmChart> helmCharts) {
        if (Objects.isNull(helmCharts)) {
            return NULL_DEFAULT_STRING;
        }
        StringBuilder stringBuilder = new StringBuilder();
        helmCharts.forEach(chart -> stringBuilder.append(chart).append(", "));
        return stringBuilder.toString();
    }

    @SuppressWarnings("unchecked")
    public static String logAdditionalParameters(Object additionalParameters) {
        if (additionalParameters instanceof Map) {
            Map<String, Object> loggableAdditionalParams = new HashMap<>((Map<String, Object>) additionalParameters);
            loggableAdditionalParams.entrySet().forEach(LoggingUtils::hideSensitiveDataAdditionalParameters);
            return loggableAdditionalParams.toString();
        }
        return Objects.toString(additionalParameters, NULL_DEFAULT_STRING);
    }

    public static String logMultiValueMap(MultiValueMap<String, Object> request) {
        if (request.containsKey(JSON_REQUEST_PARAMETER_NAME)) {
            ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(request, SHORT_PREFIX_STYLE);
            toStringBuilder.setExcludeFieldNames(JSON_REQUEST_PARAMETER_NAME);
            Map<String, Object> jsonRequestParameters = convertStringToJSONObj((String) request.get(JSON_REQUEST_PARAMETER_NAME).get(0));
            if (!MapUtils.isEmpty(jsonRequestParameters)) {
                jsonRequestParameters.entrySet().forEach(LoggingUtils::hideSensitiveData);
            }
            toStringBuilder.append(JSON_REQUEST_PARAMETER_NAME, jsonRequestParameters);
            return toStringBuilder.build();
        }
        return Objects.toString(request, NULL_DEFAULT_STRING);
    }

    public static <T> String logoOssTopologyMap(Map<String, T> ossTopology) {
        String ossTopologyString = ossTopology.keySet().stream()
                .map(key -> String.format("%s : %s, ", key, ossTopology.get(key)))
                .collect(Collectors.joining(", "));
        return "OSSTopology values: " + ossTopologyString;
    }

    public static String logComponentStatusResponse(@Nullable ComponentStatusResponse componentStatusResponse) {
        if (Objects.isNull(componentStatusResponse)) {
            return NULL_DEFAULT_STRING;
        }
        StringBuilder stringBuilder = new StringBuilder();

        if (componentStatusResponse.getPods() != null) {
            stringBuilder.append(String.format("Pods size %s, ", componentStatusResponse.getPods().size()));
        }
        if (CollectionUtils.isNotEmpty(componentStatusResponse.getDeployments())) {
            componentStatusResponse.getDeployments()
                    .forEach(resource -> stringBuilder.append(logVimLevelAdditionalResourceInfo(resource)).append(", "));
        }
        if (CollectionUtils.isNotEmpty(componentStatusResponse.getStatefulSets())) {
            componentStatusResponse.getStatefulSets()
                    .forEach(resource -> stringBuilder.append(logVimLevelAdditionalResourceInfo(resource)).append(", "));
        }
        return stringBuilder.toString();
    }

    private static String logVimLevelAdditionalResourceInfo(VimLevelAdditionalResourceInfoDeploymentStatefulSet resource) {
        if (resource == null) {
            return NULL_DEFAULT_STRING;
        }

        ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(resource, SHORT_PREFIX_STYLE);
        toStringBuilder.setExcludeFieldNames("labels", "annotations", "ownerReferences");
        return toStringBuilder.build();
    }

    private static void hideSensitiveData(Map.Entry<String, Object> keyValuePair) {
        final String key = keyValuePair.getKey();

        final boolean isSensitiveParam = key.toLowerCase().contains(SENSITIVE_PARAM_CRITERIA);
        final boolean isDay0Param = key.startsWith(DAY0_CONFIGURATION_FIELD)
                || (key.startsWith(DAY0_CONFIGURATION_PREFIX) && key.endsWith(DAY0_CONFIG_PARAMS_VALUE_SUFFIX));
        final boolean isAdditionalParamField = key.startsWith(ADDITIONAL_PARAMS_FIELD);

        if (isSensitiveParam || isDay0Param || isAdditionalParamField) {
            keyValuePair.setValue(SENSITIVE_DATA_REPLACEMENT);
        }
    }

    private static void hideSensitiveDataAdditionalParameters(Map.Entry<String, Object> keyValuePair) {
        keyValuePair.setValue(SENSITIVE_DATA_REPLACEMENT);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractAdditionalParams(final Map<String, Object> operationParams) {
        if (operationParams.containsKey(ADDITIONAL_PARAMS_FIELD)) {
            Map<String, Object> additionalParams = (Map<String, Object>) operationParams.get(ADDITIONAL_PARAMS_FIELD);
            return additionalParams == null ? Collections.emptyMap() : additionalParams;
        }
        return operationParams;
    }

    private static <T> Object extractAdditionalParams(final T operationRequest) {
        Object additionalParams = Collections.emptyMap();
        if (operationRequest instanceof InstantiateVnfRequest) {
            additionalParams = logAdditionalParameters(((InstantiateVnfRequest) operationRequest).getAdditionalParams());
        } else if (operationRequest instanceof TerminateVnfRequest) {
            additionalParams = logAdditionalParameters(((TerminateVnfRequest) operationRequest).getAdditionalParams());
        } else if (operationRequest instanceof ScaleVnfRequest) {
            additionalParams = logAdditionalParameters(((ScaleVnfRequest) operationRequest).getAdditionalParams());
        } else if (operationRequest instanceof ChangePackageInfoVnfRequest) {
            additionalParams = logAdditionalParameters(((ChangePackageInfoVnfRequest) operationRequest).getAdditionalParams());
        } else if (operationRequest instanceof ChangeCurrentVnfPkgRequest) {
            additionalParams = logAdditionalParameters(((ChangeCurrentVnfPkgRequest) operationRequest).getAdditionalParams());
        }
        return additionalParams;
    }
}
