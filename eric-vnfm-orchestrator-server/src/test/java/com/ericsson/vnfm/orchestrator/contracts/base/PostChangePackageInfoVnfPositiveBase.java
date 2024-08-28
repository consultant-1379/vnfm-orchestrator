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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifeCycleRequestFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClientImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostChangePackageInfoVnfPositiveBase extends ContractTestRunner {

    private static final String VNF_INSTANCE_ID = "positive-test-id";
    private static final String CHART_URL = "https://helm-repository.evnfm01.eccd01.eccd:443/onboarded/charts/spider-app-label-verification-2.193.100.tgz";
    private static final String VNFD_RESPONSE_FILE_NAME = "contracts/api/postChangePackageInfoVnf/positive/vnfd-response-from-nfvo.json";
    private static final String OPERATON_NAME = "change_package";
    private static final String PACKAGE_RESPONSE_ID = "ResponseId";
    private static final String PACKAGE_RESPONSE_VNFD_ID = "vnfdId";
    private static final String PACKAGE_RESPONSE_SOFTWARE_VERSION = "vnfSoftwareVersion";
    private static final String OPERATION_OCCURRENCE_ID = "d807978b-13e2-478e-8694-5bedbf2145e2";

    @Autowired
    private LifeCycleManagementService lifeCycleManagementService;

    @MockBean
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private LifeCycleRequestFactory lifeCycleRequestFactory;

    @SpyBean
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @Autowired
    private InstantiateVnfRequestValidatingService instantiateVnfRequestValidatingService;

    @MockBean
    private OnboardingClientImpl onboardingClient;

    @MockBean
    private ChangeVnfPackageService changeVnfPackageService;

    @BeforeEach
    public void setUp() {
        given(operationsInProgressRepository.save(any(OperationInProgress.class))).willReturn(new OperationInProgress());

        given(lifeCycleRequestFactory.getService(any(LifecycleOperationType.class)))
                .willReturn(changeVnfPackageRequestHandler);

        given(changeVnfPackageService.isSelfUpgrade(any(), any())).willReturn(true);

        PackageResponse[] packageResponses = new PackageResponse[1];
        final HelmPackage helmPackage = new HelmPackage();
        helmPackage.setChartUrl(CHART_URL);
        helmPackage.setPriority(1);
        final PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId(PACKAGE_RESPONSE_ID);
        packageResponse.setHelmPackageUrls(List.of(helmPackage));
        packageResponse.setVnfdId(PACKAGE_RESPONSE_VNFD_ID);
        packageResponse.setVnfSoftwareVersion(PACKAGE_RESPONSE_SOFTWARE_VERSION);
        packageResponses[0] = packageResponse;

        String vnfdDetails;
        try {
            vnfdDetails = getFile(VNFD_RESPONSE_FILE_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        when(onboardingClient.get(any(URI.class), eq(MediaType.TEXT_PLAIN_VALUE), any()))
                .thenReturn(Optional.of(vnfdDetails));

        given(onboardingClient.get(any(URI.class), eq(MediaType.APPLICATION_JSON_VALUE), any()))
                .willReturn(Optional.ofNullable(packageResponses));

        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(createVnfInstanceForTest()));
        when(lifecycleOperationRepository.save(any(LifecycleOperation.class)))
                .thenAnswer(invocationOnMock -> {
                    final Object[] arguments = invocationOnMock.getArguments();
                    LifecycleOperation operation = (LifecycleOperation) arguments[0];
                    operation.setOperationOccurrenceId(OPERATION_OCCURRENCE_ID);
                    return operation;
                });

       doNothing().when(changeVnfPackageRequestHandler).updateInstance(any(), any(), any(), any(), any());

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    @BeforeAll
    public static void beforeAll() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterAll
    public static void afterAll() {
        TransactionSynchronizationManager.clear();
    }

    private VnfInstance createVnfInstanceForTest() {
        final HelmChart helmChart = new HelmChart();
        helmChart.setId("1");

        final VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(VNF_INSTANCE_ID);
        vnfInstance.setHelmCharts(List.of(helmChart));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);

        final List<OperationDetail> operationDetailsList = new ArrayList<>();
        final OperationDetail operation = OperationDetail.ofSupportedOperation(OPERATON_NAME);
        operationDetailsList.add(operation);
        vnfInstance.setSupportedOperations(operationDetailsList);

        return vnfInstance;
    }

    private String getFile(final String fileName) throws IOException, URISyntaxException {
        return readDataFromFile(fileName);
    }
}
