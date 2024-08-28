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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.createNotSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.createNotSupportedOperationsWithError;
import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.lcm.GrantingResourceDefinitionCalculationImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpAdditionalParamsProcessorImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleLevelCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleParametersService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ScaleRequestHandler.class,
        ObjectMapper.class,
        ScaleServiceImpl.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        InstantiateVnfRequestValidatingServiceImpl.class,
        HelmChartHelper.class})
@MockBean({
        WorkflowRoutingService.class,
        LifeCycleManagementHelper.class,
        ClusterConfigService.class,
        GrantingResourceDefinitionCalculationImpl.class,
        DatabaseInteractionService.class,
        OssNodeService.class,
        LcmOpSearchServiceImpl.class,
        LcmOpAdditionalParamsProcessorImpl.class,
        ValuesFileComposer.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        UsernameCalculationService.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        MappingFileService.class,
        VnfInstanceService.class,
        ScaleParametersService.class,
        PackageService.class,
        ScaleLevelCalculationService.class,
        LifecycleOperationHelper.class,
        NetworkDataTypeValidationService.class,
        ReplicaCountCalculationService.class,
        HelmChartRepository.class
})
public class ScaleRequestHandlerNegativeTest {

    private static final String INSTANCE_ID = "instance-id";

    @Autowired
    private ScaleRequestHandler scaleRequestHandler;

    @MockBean
    private InstanceService instanceService;

    @Test
    public void testSpecificValidationForInvalidScaleParameters() {
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createSupportedOperations(LCMOperationsEnum.SCALE));

        assertThatThrownBy(() -> scaleRequestHandler.specificValidation(vnfInstance, createScaleVnfRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scale not supported as policies not present for instance already-instantiated");
    }

    @Test
    public void testSpecificValidationForNotSupportedScale() {
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createNotSupportedOperations(LCMOperationsEnum.SCALE));

        assertThatThrownBy(() -> scaleRequestHandler.specificValidation(vnfInstance, createScaleVnfRequest()))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessage("Operation scale is not supported for package test");
    }

    @Test
    public void testSpecificValidationForNotSupportedScaleWithError() {
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createNotSupportedOperationsWithError(
                        Map.of(LCMOperationsEnum.SCALE, "Scale validation error message")));

        assertThatThrownBy(() -> scaleRequestHandler.specificValidation(vnfInstance, createScaleVnfRequest()))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessage("Operation scale is not supported for package test due to cause: Scale validation error message");
    }

    private static ScaleVnfRequest createScaleVnfRequest() {
        ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAspectId("Payload");
        request.setType(ScaleVnfRequest.TypeEnum.IN);
        request.setNumberOfSteps(4);
        return request;
    }

    private static VnfInstance createVnfInstance(String instanceId, List<OperationDetail> operationDetails) {
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.INSTANTIATED);
        value.setVnfInstanceId(instanceId);
        value.setVnfPackageId("test");
        value.setSupportedOperations(operationDetails);
        return value;
    }
}

