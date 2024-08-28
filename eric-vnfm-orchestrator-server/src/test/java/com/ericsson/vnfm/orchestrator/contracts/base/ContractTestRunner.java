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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.ApplicationServer;
import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartHistoryRepository;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.TaskRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.CustomLifecycleOperationRepositoryImpl;
import com.ericsson.vnfm.orchestrator.repositories.impl.CustomVnfInstanceRepositoryImpl;
import com.ericsson.vnfm.orchestrator.repositories.impl.CustomVnfResourceViewRepositoryImpl;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.TupleMapperFactory;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.FullSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;

@SpringBootTest(classes = ApplicationServer.class,
        properties = {
                "spring.cloud.kubernetes.enabled = false",
                "spring.main.cloud-platform = NONE",
                "spring.flyway.enabled = false",
                "management.endpoint.health.group.readiness.include = ping, diskSpace",
                "management.endpoint.health.group.liveness.include = diskSpace, ping"
        })
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@MockBean(classes = {
        LifecycleOperationRepository.class,
        ClusterConfigFileRepository.class,
        ClusterConfigInstanceRepository.class,
        HelmChartRepository.class,
        HelmChartHistoryRepository.class,
        ChangedInfoRepository.class,
        ChangePackageOperationDetailsRepository.class,
        LifecycleOperationViewRepository.class,
        OperationsInProgressRepository.class,
        ScaleInfoRepository.class,
        VnfInstanceNamespaceDetailsRepository.class,
        VnfInstanceViewRepository.class,
        VnfResourceViewRepository.class,
        VnfInstanceRepository.class,
        CustomVnfInstanceRepositoryImpl.class,
        CustomLifecycleOperationRepositoryImpl.class,
        CustomVnfResourceViewRepositoryImpl.class,
        FullSelectionQueryExecutor.class,
        PartialSelectionQueryExecutor.class,
        TaskRepository.class,
        LifecycleOperationStageRepository.class,
        RequestProcessingDetailsRepository.class,
        LifecycleOperationStageRepository.class,
        TupleMapperFactory.class
})
@TestPropertySource(properties = {
        "spring.flyway.enabled = false"
})
@AutoConfigureObservability
public class ContractTestRunner {

    @MockBean
    private UsernameCalculationService usernameCalculationService;

    @MockBean
    private LicenseConsumerService licenseConsumerService;

    @BeforeEach
    public void mockServices() {
        mockLicenseConsumerService();
        mockUsernameCalculationService();
    }

    private void mockUsernameCalculationService() {
        when(usernameCalculationService.calculateUsername()).thenReturn("E2E_USERNAME");
    }

    private void mockLicenseConsumerService(){
        given(licenseConsumerService.getPermissions()).willReturn(EnumSet.allOf(Permission.class));
    }
}
