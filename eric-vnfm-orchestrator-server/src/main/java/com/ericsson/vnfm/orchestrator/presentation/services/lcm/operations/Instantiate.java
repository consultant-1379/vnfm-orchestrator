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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.removeExcessAdditionalParams;
import static com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils.getUpgradeFailedAdditionalParams;

import java.nio.file.Path;
import java.util.Map;

import com.ericsson.am.shared.vnfd.ChangeVnfPackagePatternUtility;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Instantiate implements Command {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private AdditionalParamsUtils additionalParamsUtils;

    @Autowired
    private OperationsUtils operationsUtils;

    @Autowired
    private PackageService packageService;

    @Override
    public CommandType getType() {
        return CommandType.INSTANTIATE;
    }

    @Override
    public void execute(final LifecycleOperation operation, final HelmChart helmChart, final boolean isDowngradeOperation) {
        LOGGER.info("Starting Instantiate command");
        final VnfInstance originalInstance = operation.getVnfInstance();

        final Map<String, Object> instanceAdditionalParams =
                additionalParamsUtils.convertAdditionalParamsToMap(originalInstance.getCombinedAdditionalParams());
        final Map<String, Object> lastOperationAdditionalParams =
                operationsUtils.getAdditionalParamsFromLastOperation(originalInstance, isDowngradeOperation);
        final Map<String, Object> operationAdditionalParams = getOperationAdditionalParams(operation, isDowngradeOperation);
        final Map<String, Object> globalStaticParams = getGlobalStaticParams(originalInstance);
        final Map<String, Object> scaleParams = operationsUtils.getScaleParamsLastOperation(originalInstance, helmChart, isDowngradeOperation);

        final Map<String, Object> additionalParams = getAdditionalParams(instanceAdditionalParams,
                                                                         lastOperationAdditionalParams,
                                                                         operationAdditionalParams);
        final Path valuesFile = getValuesFile(instanceAdditionalParams,
                                              lastOperationAdditionalParams,
                                              operationAdditionalParams,
                                              globalStaticParams,
                                              scaleParams);

        triggerInstantiateOperation(operation, helmChart, additionalParams, valuesFile);
    }

    private Map<String, Object> getOperationAdditionalParams(final LifecycleOperation operation, final boolean isDowngradeOperation) {
        // Get params from current rollback operation or failed upgrade operation
        if (isDowngradeOperation) {
            return additionalParamsUtils.convertAdditionalParamsToMap(operation.getValuesFileParams());
        }

        return getUpgradeFailedAdditionalParams(operation);
    }

    private Map<String, Object> getGlobalStaticParams(final VnfInstance vnfInstance) {
        final VnfInstance tempInstance = Utility.parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        final JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());

        return ChangeVnfPackagePatternUtility.getGlobalStaticParams(vnfd, vnfInstance.getVnfDescriptorId(), tempInstance.getVnfDescriptorId());
    }

    private static Map<String, Object> getAdditionalParams(final Map<String, Object> instanceAdditionalParams,
                                                           final Map<String, Object> lastOperationAdditionalParams,
                                                           final Map<String, Object> operationAdditionalParams) {

        final Map<String, Object> mergedParameters = Utility.copyParametersMap(instanceAdditionalParams);
        mergedParameters.putAll(lastOperationAdditionalParams);
        mergedParameters.putAll(operationAdditionalParams);

        // remove non EVNFM params from merged params
        removeExcessAdditionalParams(mergedParameters);

        return mergedParameters;
    }

    @SafeVarargs
    private Path getValuesFile(final Map<String, Object>... additionalParamsToMerge) {
        return YamlUtility.writeStringToValuesFile(additionalParamsUtils.mergeAdditionalParams(additionalParamsToMerge));
    }

    private void triggerInstantiateOperation(LifecycleOperation operation,
                                             HelmChart helmChart,
                                             final Map<String, Object> additionalParams,
                                             final Path valuesFile) {

        final VnfInstance originalInstance = operation.getVnfInstance();

        final WorkflowRoutingResponse workflowRoutingResponse =
                workflowRoutingService.routeInstantiateRequest(originalInstance, operation, helmChart, additionalParams, valuesFile);

        if (workflowRoutingResponse.getHttpStatus().isError()) {
            OperationsUtils.updateOperationOnFailure(workflowRoutingResponse.getErrorMessage(),
                                                     operation,
                                                     originalInstance,
                                                     helmChart.getReleaseName());
        }
    }
}



