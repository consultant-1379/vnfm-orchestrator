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

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.updateOperationOnRollingBack;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.resolveTimeOut;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BasicChangeOperationContextBuilder implements ChangeOperationContextBuilder {

    @Autowired
    private AdditionalParamsUtils additionalParamsUtils;

    @Override
    public ChangeOperationContext build(final VnfInstance sourceVnfInstance,
                                        final LifecycleOperation operation,
                                        final ChangeCurrentVnfPkgRequest request) {
        final VnfInstance tempVnfInstance = createTempInstance(operation, sourceVnfInstance);
        request.setVnfdId(tempVnfInstance.getVnfDescriptorId());

        ChangeOperationContext context = new ChangeOperationContext(sourceVnfInstance, request, operation);
        context.setTempInstance(tempVnfInstance);
        context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        final Map<String, Object> additionalParams = buildAdditionalParamsFromVnfInstanceAndOperation(tempVnfInstance, operation);
        context.setAdditionalParams(additionalParams);

        return context;
    }

    private boolean isDownsizeFailed(VnfInstance vnfInstance) {
        return vnfInstance
                .getHelmCharts()
                .stream()
                .anyMatch(chart -> LifecycleOperationState.FAILED.name().equals(chart.getDownsizeState()));
    }

    private VnfInstance createTempInstance(LifecycleOperation operation, final VnfInstance vnfInstance) {
        if (operation.isAutoRollbackAllowed() && isDownsizeFailed(vnfInstance)) {
            updateOperationOnRollingBack(operation);
            return InstanceUtils.createTempInstance(vnfInstance);
        } else {
            return parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        }
    }

    private Map<String, Object> buildAdditionalParamsFromVnfInstanceAndOperation(VnfInstance vnfInstance, LifecycleOperation operation) {
        final Map<String, Object> parsedAdditionalParams = additionalParamsUtils
                .convertAdditionalParamsToMap(vnfInstance.getCombinedAdditionalParams());
        Map<String, Object> additionalParams = new HashMap<>(parsedAdditionalParams);
        additionalParams.put(APPLICATION_TIME_OUT, resolveTimeOut(operation));
        return additionalParams;
    }
}
