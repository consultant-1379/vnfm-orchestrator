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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangePackageOperationDetailsService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class DowngradeInfoNegativeBase extends ContractTestRunner {

    private static final URI VNFD_NOT_FOUND_URI = URI.create("http://localhost:10102/api/vnfpkgm/v1/vnf_packages?filter=(eq,vnfdId,vnfdNOTFOUND)");
    @MockBean
    private ChangePackageOperationDetailsService changePackageOperationDetailsService;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @MockBean
    private OnboardingClient onboardingClient;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        VnfInstance downgradeNotSupportedInstance = generateInstance("DOWNGRADE_NOT_SUPPORTED_FOR_INSTANCE_ID");
        downgradeNotSupportedInstance.setVnfDescriptorId("DOWNGRADE_NOT_SUPPORTED_FOR_VNFD_ID");

        VnfInstance packageDetailsDeletedInstance = generateInstance("package-details-deleted-for-downgrade");
        packageDetailsDeletedInstance.setVnfDescriptorId("PACKAGE_DETAILS_DELETED_FOR_DOWNGRADE_VNFD_ID");
        List<LifecycleOperation> operations = getLifecycleOperations(packageDetailsDeletedInstance);
        packageDetailsDeletedInstance.setAllOperations(operations);
        ChangePackageOperationDetails operationDetails = new ChangePackageOperationDetails();
        operationDetails.setOperationOccurrenceId(operations.get(1).getOperationOccurrenceId());
        operationDetails.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);

        when(databaseInteractionService.getVnfInstance("DOWNGRADE_NOT_SUPPORTED_FOR_INSTANCE_ID")).thenReturn(downgradeNotSupportedInstance);
        when(databaseInteractionService.getVnfInstance("NO_INSTANCE_ID_FOUND")).thenThrow(new NotFoundException(""));
        when(databaseInteractionService.getVnfInstance("package-details-deleted-for-downgrade")).thenReturn(packageDetailsDeletedInstance);
        when(changePackageOperationDetailsService.findAllByVnfInstance(packageDetailsDeletedInstance)).thenReturn(List.of(operationDetails));
        when(onboardingClient.get(eq(VNFD_NOT_FOUND_URI), eq(MediaType.APPLICATION_JSON_VALUE), eq(PackageResponse[].class))).thenReturn(Optional.empty());
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<LifecycleOperation> getLifecycleOperations(final VnfInstance packageDetailsDeletedInstance) {
        List<LifecycleOperation> operations = Lists.newArrayList(
                generateLifecycleOperation(packageDetailsDeletedInstance, LifecycleOperationType.INSTANTIATE, now().minusHours(6L),
                                           "vnfdNOTFOUND", null),
                generateLifecycleOperation(packageDetailsDeletedInstance, LifecycleOperationType.CHANGE_VNFPKG, now().minusHours(4L),
                                           "vnfdNOTFOUND", "validVnfd")
        );
        return operations;
    }

    private VnfInstance generateInstance(String vnfId) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(vnfId);
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
}
