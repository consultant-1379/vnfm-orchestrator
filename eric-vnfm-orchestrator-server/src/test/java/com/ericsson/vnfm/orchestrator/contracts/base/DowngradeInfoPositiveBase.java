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

import static java.time.LocalDateTime.now;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class DowngradeInfoPositiveBase extends ContractTestRunner{

    private static final String TEST_VNF_ID = "test";

    @MockBean
    private OnboardingClient onboardingClient;
    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;
    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;
    @MockBean
    private ChangePackageOperationDetailsService changePackageOperationDetailsService;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        VnfInstance sourceVnfInstance = generateInstance();
        List<LifecycleOperation> operations = getLifecycleOperations(sourceVnfInstance);
        sourceVnfInstance.setAllOperations(operations);
        final List<ChangePackageOperationDetails> operationDetails = operations.stream()
                .map(LifecycleOperation::getOperationOccurrenceId)
                .map(this::generateChangeVnfPackageOperationDetails)
                .collect(Collectors.toList());
        final PackageResponse[] packageResponses = getPackageResponses(getHelmPackages());

        when(vnfInstanceRepository.findById(TEST_VNF_ID)).thenReturn(Optional.of(sourceVnfInstance));
        when(changePackageOperationDetailsService.findAllByVnfInstance(any(VnfInstance.class))).thenReturn(operationDetails);
        when(onboardingClient.get(any(),eq(MediaType.APPLICATION_JSON_VALUE),eq(PackageResponse[].class))).thenReturn(Optional.of(packageResponses));
        when(onboardingClient.get(any(), eq(MediaType.TEXT_PLAIN_VALUE), eq(String.class)))
            .thenReturn(Optional.of(readDataFromFile(Path.of("src/test/resources/yaml/vnfd-for-containerized-vnf.yaml"))));
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(sourceVnfInstance);
        when(databaseInteractionService.saveVnfInstanceToDB(any(VnfInstance.class))).thenReturn(sourceVnfInstance);

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<LifecycleOperation> getLifecycleOperations(final VnfInstance sourceVnfInstance) {
        List<LifecycleOperation> operations = Lists.newArrayList(
                generateLifecycleOperation(sourceVnfInstance, LifecycleOperationType.INSTANTIATE, now().minusHours(6),
                                           "b1bb0ce7-ebca-4fa7-95ed-4840d70a1177", null),
                generateLifecycleOperation(sourceVnfInstance, LifecycleOperationType.CHANGE_VNFPKG, now().minusHours(5),
                                           "secondValidVnfd", "b1bb0ce7-ebca-4fa7-95ed-4840d70a1177"),
                generateLifecycleOperation(sourceVnfInstance, LifecycleOperationType.CHANGE_VNFPKG, now().minusHours(4),
                                           "b1bb0ce7-ebca-4fa7-95ed-4840d70a1177", "ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5"));
        return operations;
    }

    private static PackageResponse[] getPackageResponses(final List<HelmPackage> helmPackages) {
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId("d3def1ce-4cf4-477c-aab3-21cb04e6a379");
        packageResponse.setVnfdId("b1bb0ce7-ebca-4fa7-95ed-4840d70a1177");
        packageResponse.setVnfdVersion("cxp9025898_4r81e08");
        packageResponse.setHelmPackageUrls(helmPackages);

        PackageResponse[] packageResponses = new PackageResponse[] {packageResponse};
        return packageResponses;
    }

    private static List<HelmPackage> getHelmPackages() {
        HelmPackage helmPackage1 = new HelmPackage();
        helmPackage1.setPriority(1);
        helmPackage1.setChartUrl("helm1.tgz");
        helmPackage1.setChartName("sample-helm1");
        helmPackage1.setChartType(HelmChartType.CNF);
        helmPackage1.setChartVersion("1.0.0");
        helmPackage1.setChartArtifactKey("helm_package_1");
        HelmPackage helmPackage2 = new HelmPackage();
        helmPackage2.setPriority(2);
        helmPackage2.setChartUrl("helm2.tgz");
        helmPackage2.setChartName("sample-helm2");
        helmPackage2.setChartType(HelmChartType.CNF);
        helmPackage2.setChartVersion("1.0.0");
        helmPackage2.setChartArtifactKey("helm_package_2");
        List<HelmPackage> helmPackages = List.of(helmPackage1, helmPackage2);
        return helmPackages;
    }

    private VnfInstance generateInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(TEST_VNF_ID);
        vnfInstance.setVnfDescriptorId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");
        vnfInstance.setVnfPackageId("testPackageIdDOWNGRADE");
        vnfInstance.setVnfdVersion("testPackageVersion");
        return vnfInstance;
    }

    private LifecycleOperation generateLifecycleOperation(VnfInstance vnfInstance, LifecycleOperationType operationType, LocalDateTime startTime,
                                                          String sourceVnfd, String targetVnfdId) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(UUID.randomUUID().toString());
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.setLifecycleOperationType(operationType);
        lifecycleOperation.setStartTime(startTime);
        lifecycleOperation.setStateEnteredTime(startTime.plusMinutes(2L));
        lifecycleOperation.setSourceVnfdId(sourceVnfd);
        lifecycleOperation.setTargetVnfdId(targetVnfdId);
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        return lifecycleOperation;
    }

    private ChangePackageOperationDetails generateChangeVnfPackageOperationDetails(String operationOccurrenceId) {
        ChangePackageOperationDetails operationDetails = new ChangePackageOperationDetails();
        operationDetails.setOperationOccurrenceId(operationOccurrenceId);
        operationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
        return operationDetails;
    }
}
