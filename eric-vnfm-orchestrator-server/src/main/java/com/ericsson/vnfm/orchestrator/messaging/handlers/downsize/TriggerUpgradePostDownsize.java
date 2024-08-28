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
package com.ericsson.vnfm.orchestrator.messaging.handlers.downsize;

import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.createTempInstance;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.writeStringToValuesFile;

import java.nio.file.Path;
import java.util.Objects;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeOperationContextBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TriggerUpgradePostDownsize extends MessageHandler<HelmReleaseLifecycleMessage> {

    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;
    private LifeCycleManagementHelper lifeCycleManagementHelper;
    private ChangeOperationContextBuilder changeOperationContextBuilder;
    private HelmChartHelper helmChartHelper;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Handling triggering upgrade post downsize");

        LifecycleOperation lifecycleOperation = context.getOperation();
        VnfInstance sourceVnfInstance = context.getVnfInstance();

        processInstanceBeforeUpgrade(sourceVnfInstance, lifecycleOperation);

        Path valuesFile = createValuesYamlFile(lifecycleOperation, sourceVnfInstance, context);
        ChangeOperationContext changeOperationContext = changeOperationContextBuilder
                .build(sourceVnfInstance, lifecycleOperation, new ChangeCurrentVnfPkgRequest());
        VnfInstance tempInstance = changeOperationContext.getTempInstance();
        helmChartHelper.completeDisabledHelmCharts(tempInstance.getHelmCharts());
        changeVnfPackageRequestHandler.sendPostDownsizeRequest(changeOperationContext, valuesFile);

        passToSuccessor(getSuccessor(), context);
    }

    private void processInstanceBeforeUpgrade(final VnfInstance vnfInstance, final LifecycleOperation operation) {
        if (isDownsizeDuringRollbackAfterCCVPFailure(operation)) {
            final VnfInstance oldInstanceWithFailedChart = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            final VnfInstance tempInstance = createTempInstance(vnfInstance);
            vnfInstance.setVnfPackageId(oldInstanceWithFailedChart.getVnfPackageId());
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        }
    }

    private Path createValuesYamlFile(LifecycleOperation lifecycleOperation, VnfInstance vnfInstance,
                                      MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        try {
            String yamlContent;
            if (isDownsizeDuringRollbackAfterCCVPFailure(lifecycleOperation)) {
                yamlContent = YamlUtility.convertEscapedJsonToYaml(vnfInstance.getCombinedValuesFile());
            } else {
                yamlContent = YamlUtility.convertEscapedJsonToYaml(lifecycleOperation.getValuesFileParams());
            }
            return writeStringToValuesFile(yamlContent);
        } catch (Exception e) {
            String errorMessage = String.format("Failed creating values file due to %s", e.getMessage());
            LOGGER.error(errorMessage, e);
            lifeCycleManagementHelper.setOperationErrorFor4xx(lifecycleOperation, errorMessage);
            passToSuccessor(getSuccessor(), context);
        }

        return null;
    }

    private static boolean isDownsizeDuringRollbackAfterCCVPFailure(final LifecycleOperation operation) {
        return Objects.equals(operation.getOperationState(), LifecycleOperationState.ROLLING_BACK);
    }
}
