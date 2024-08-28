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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.SyncVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.sync.SyncService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        SyncRequestHandler.class,
        VnfInstanceRepository.class,
        InstanceService.class,
        PackageService.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class,
        InstantiateVnfRequestValidatingServiceImpl.class })
@MockBean({
        MappingFileServiceImpl.class,
        OperationsInProgressRepository.class,
        HelmChartRepository.class,
        ScaleInfoRepository.class,
        ChangePackageOperationDetailsRepository.class,
        VnfInstanceService.class,
        YamlMapFactoryBean.class,
        ExtensionsService.class,
        InstantiationLevelService.class,
        PackageService.class,
        ValuesFileService.class,
        NotificationService.class,
        InstanceUtils.class,
        VnfInstanceMapper.class,
        SyncService.class,
        DatabaseInteractionService.class,
        ObjectMapper.class,
        OssNodeService.class,
        ClusterConfigService.class,
        ValuesFileComposer.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        UsernameCalculationService.class,
        GrantingNotificationsConfig.class,
        NfvoConfig.class,
        GrantingService.class,
        GrantingResourceDefinitionCalculation.class,
        OnboardingConfig.class,
        RestTemplate.class,
        VnfInstanceQuery.class,
        VnfInstanceRepository.class,
        LifecycleOperationHelper.class,
        LifeCycleManagementHelper.class,
        AdditionalAttributesHelper.class,
        LcmOpSearchService.class,
        NetworkDataTypeValidationService.class,
        ReplicaCountCalculationService.class,
        ChangePackageOperationDetailsService.class,
        ChangeVnfPackageService.class
})
public class SyncRequestHandlerTest {

    @Autowired
    private SyncRequestHandler syncRequestHandler;

    @MockBean
    private VnfInstanceRepository instanceRepository;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @Test
    public void testSpecificValidationForVnfIsNotInstantiated() {
        SyncVnfRequest request = new SyncVnfRequest();
        VnfInstance vnf = TestUtils.getVnfInstance();
        vnf.setInstantiationState(InstantiationState.NOT_INSTANTIATED);

        when(databaseInteractionService.getVnfInstance(any())).thenReturn(vnf);

        assertThatThrownBy(() -> syncRequestHandler
                .specificValidation(vnf, request))
                .isInstanceOf(NotInstantiatedException.class);
    }

    @Test
    public void testSpecificValidationForVnfIsPresentAndInstantiated() {
        SyncVnfRequest request = new SyncVnfRequest();
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);

        when(databaseInteractionService.getVnfInstance(any())).thenReturn(vnfInstance);

        syncRequestHandler.specificValidation(vnfInstance, request);
    }
}
