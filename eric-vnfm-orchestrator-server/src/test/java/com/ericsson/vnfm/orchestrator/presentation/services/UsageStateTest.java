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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
      InstanceService.class,
      PackageServiceImpl.class
})
@MockBean({
      NfvoConfig.class,
      OnboardingConfig.class,
      RestTemplate.class,
      VnfInstanceQuery.class,
      OperationsInProgressRepository.class,
      HelmChartRepository.class,
      ScaleInfoRepository.class,
      ChangePackageOperationDetailsRepository.class,
      VnfInstanceService.class,
      ObjectMapper.class,
      YamlMapFactoryBean.class,
      ExtensionsService.class,
      InstantiationLevelService.class,
      ValuesFileService.class,
      NotificationService.class,
      InstanceUtils.class,
      VnfInstanceMapper.class,
      AdditionalAttributesHelper.class,
      RetryTemplate.class,
      LifecycleOperationHelper.class,
      LcmOpSearchService.class,
      LifeCycleManagementHelper.class,
      ReplicaCountCalculationService.class,
      ChangePackageOperationDetailsService.class,
      ChangeVnfPackageService.class
})
public class UsageStateTest {

    private final String TEST_APP_PKG_ID = "testAppPkgId";

    private final String TEST_INSTANCE_ID = "testInstanceId";

    @Autowired
    private InstanceService instanceService;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OnboardingClient onboardingClient;

    @MockBean
    private OnboardingUriProvider onboardingUriProvider;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @Test
    public void shouldErrorForUpdateUsageStateWithEmptyPkgId() {
        assertThatThrownBy(() ->
                instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance("", TEST_INSTANCE_ID, true))
                .isInstanceOf(InternalRuntimeException.class);
        verify(databaseInteractionService, times(1)).deleteByVnfInstanceId(TEST_INSTANCE_ID);
    }

    @Test
    public void shouldErrorForUpdateUsageStateWithNullPkgId() {
        assertThatThrownBy(() ->
                instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(null, TEST_INSTANCE_ID, true))
                .isInstanceOf(InternalRuntimeException.class);
        verify(databaseInteractionService, times(1)).deleteByVnfInstanceId(TEST_INSTANCE_ID);
    }

    @Test
    public void shouldErrorForUpdateUsageStateWithEmptyInstanceId() {
        assertThatThrownBy(() ->
                instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(TEST_APP_PKG_ID, "", true))
                .isInstanceOf(InternalRuntimeException.class);
        verify(databaseInteractionService, times(1)).deleteByVnfInstanceId("");
    }

    @Test
    public void shouldErrorForUpdateUsageStateWithNullInstanceId() {
        assertThatThrownBy(() ->
                instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(TEST_APP_PKG_ID, null, true))
                .isInstanceOf(InternalRuntimeException.class);
        verify(databaseInteractionService, times(1)).deleteByVnfInstanceId(null);
    }

    @Test
    public void shouldUpdateUsageStateForCreateVnf() {
        when(onboardingUriProvider.getUpdateUsageStateUri(any())).thenReturn(URI.create("http://someHost/somePath"));

        instanceService.
                createAndSaveAssociationBetweenPackageAndVnfInstance(TEST_APP_PKG_ID, TEST_INSTANCE_ID, true);
        verify(onboardingClient, times(1)).put(any(), any());
    }
}
