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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DOWNSIZE_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;

@ExtendWith(MockitoExtension.class)
public class EnhancedChangeOperationContextBuilderTest extends ChangeOperationContextBuilderTestCommon {

    private static final String DOWNGRADE_OPERATION_ID = "i3def1ce-4cf4-477c-aab3-21c454e6a501";

    @Mock
    private MessageUtility messageUtility;

    @Mock
    private PackageService packageService;

    @Mock
    private ChangeVnfPackageService changeVnfPackageService;

    @InjectMocks
    private ChangeOperationContextBuilder changeOperationContextBuilder = new EnhancedChangeOperationContextBuilder();

    private VnfInstance vnfInstance;
    private LifecycleOperation operation;
    private ChangeCurrentVnfPkgRequest request;

    private PackageResponse mockedPackageResponse;
    private PackageResponse mockedPackageResponseWithoutPolicy;
    private Optional<LifecycleOperation> mockedDowngradeOperation;

    @BeforeEach
    public void setUp() throws Exception {
        vnfInstance = buildVnfInstance(LifecycleOperationState.COMPLETED);
        operation = buildLifecycleOperation(false);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("applicationTimeOut", "3600");
        additionalParams.put("skipVerification", "true");
        additionalParams.put("skipJobVerification", "true");
        additionalParams.put(DOWNSIZE_VNFD_KEY, "true");
        additionalParams.put(IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY, "true");
        request = new ChangeCurrentVnfPkgRequest().vnfdId(TARGET_VNFD_ID);
        request.setAdditionalParams(additionalParams);

        mockedPackageResponse = new PackageResponse();
        String descriptorModel = TestUtils.readDataFromFile("changeOperationContextBuilder/descriptor-model.json");
        mockedPackageResponse.setDescriptorModel(descriptorModel);

        mockedPackageResponseWithoutPolicy = new PackageResponse();
        String descriptorModelWithoutDowngrade = TestUtils
                .readDataFromFile("changeOperationContextBuilder/descriptor-model-without-downgrade.json");
        mockedPackageResponseWithoutPolicy.setDescriptorModel(descriptorModelWithoutDowngrade);

        LifecycleOperation downgradeOperation = new LifecycleOperation();
        downgradeOperation.setOperationOccurrenceId(DOWNGRADE_OPERATION_ID);
        mockedDowngradeOperation = Optional.of(downgradeOperation);
    }

    @Test
    public void buildDowngradeSuccess() {
        ChangePackageOperationSubtype expectedType = ChangePackageOperationSubtype.DOWNGRADE;
        String expectedSourceVnfInstanceId = vnfInstance.getVnfInstanceId();
        String expectedOperationId = operation.getOperationOccurrenceId();
        String expectedPackageId = mockedPackageResponse.getId();
        int expectedAdditionalParamsSize = 5;

        doReturn(mockedPackageResponse).when(packageService).getPackageInfoWithDescriptorModel(TARGET_VNFD_ID);
        doReturn(mockedPackageResponse).when(packageService).getPackageInfoWithDescriptorModel(SOURCE_VNFD_ID);
        when(changeVnfPackageService.getSuitableTargetDowngradeOperationFromVnfInstance(eq(TARGET_VNFD_ID), any(VnfInstance.class)))
                .thenReturn(mockedDowngradeOperation);

        ChangeOperationContext actual = changeOperationContextBuilder.build(vnfInstance, operation, request);

        assertThat(actual.getChangePackageOperationSubtype()).isEqualTo(expectedType);
        assertThat(actual.getSourceVnfInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getSourcePackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getTargetPackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getOperation().getOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetOperationOccurrenceId()).isEqualTo(DOWNGRADE_OPERATION_ID);
        assertThat(actual.getTargetVnfdId()).isEqualTo(TARGET_VNFD_ID);
        assertThat(actual.isDownsize()).isTrue();
        assertThat(actual.isAutoRollbackAllowed()).isTrue();

        Map<String, Object> actualAdditionalParams = actual.getAdditionalParams();
        assertThat(actualAdditionalParams)
                .hasSize(expectedAdditionalParamsSize)
                .containsKey("data_conversion_identifier")
                .doesNotContainKey("property_without_default_value");
    }

    @Test
    public void buildUpgradeWhenDowngradePolicyIsAbsentSuccess() {
        ChangePackageOperationSubtype expectedType = ChangePackageOperationSubtype.UPGRADE;
        String expectedSourceVnfInstanceId = vnfInstance.getVnfInstanceId();
        String expectedOperationId = operation.getOperationOccurrenceId();
        String expectedPackageId = mockedPackageResponse.getId();
        int expectedAdditionalParamsSize = 4;

        doReturn(mockedPackageResponse).when(packageService).getPackageInfoWithDescriptorModel(TARGET_VNFD_ID);
        doReturn(mockedPackageResponseWithoutPolicy).when(packageService).getPackageInfoWithDescriptorModel(SOURCE_VNFD_ID);
        when(changeVnfPackageService.getSuitableTargetDowngradeOperationFromVnfInstance(eq(TARGET_VNFD_ID), any(VnfInstance.class)))
                .thenReturn(mockedDowngradeOperation);

        ChangeOperationContext actual = changeOperationContextBuilder.build(vnfInstance, operation, request);

        assertThat(actual.getChangePackageOperationSubtype()).isEqualTo(expectedType);
        assertThat(actual.getSourceVnfInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getSourcePackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getTargetPackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getOperation().getOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetOperationOccurrenceId()).isNull();
        assertThat(actual.getTargetVnfdId()).isEqualTo(TARGET_VNFD_ID);
        assertThat(actual.isDownsize()).isTrue();
        assertThat(actual.isAutoRollbackAllowed()).isTrue();

        Map<String, Object> actualAdditionalParams = actual.getAdditionalParams();
        assertThat(actualAdditionalParams).hasSize(expectedAdditionalParamsSize)
                .doesNotContainKey("data_conversion_identifier");
    }

    @Test
    public void buildUpgradeSuccess() {
        ChangePackageOperationSubtype expectedType = ChangePackageOperationSubtype.UPGRADE;
        String expectedSourceVnfInstanceId = vnfInstance.getVnfInstanceId();
        String expectedOperationId = operation.getOperationOccurrenceId();
        String expectedPackageId = mockedPackageResponse.getId();
        int expectedAdditionalParamsSize = 4;

        doReturn(mockedPackageResponse).when(packageService).getPackageInfoWithDescriptorModel(TARGET_VNFD_ID);
        when(changeVnfPackageService.getSuitableTargetDowngradeOperationFromVnfInstance(eq(TARGET_VNFD_ID), any(VnfInstance.class)))
                .thenReturn(Optional.empty());

        ChangeOperationContext actual = changeOperationContextBuilder.build(vnfInstance, operation, request);

        assertThat(actual.getChangePackageOperationSubtype()).isEqualTo(expectedType);
        assertThat(actual.getSourceVnfInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getSourcePackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getTargetPackageInfo().getId()).isEqualTo(expectedPackageId);
        assertThat(actual.getOperation().getOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetOperationOccurrenceId()).isNull();
        assertThat(actual.getTargetVnfdId()).isEqualTo(TARGET_VNFD_ID);
        assertThat(actual.isDownsize()).isTrue();
        assertThat(actual.isAutoRollbackAllowed()).isTrue();

        Map<String, Object> actualAdditionalParams = actual.getAdditionalParams();
        assertThat(actualAdditionalParams).hasSize(expectedAdditionalParamsSize)
                .doesNotContainKey("data_conversion_identifier");
    }

    @Test
    public void buildUpgradeWithNonExistentTargetPackageFailed() {
        doThrow(PackageDetailsNotFoundException.class).when(packageService).getPackageInfoWithDescriptorModel(TARGET_VNFD_ID);
        doNothing().when(messageUtility).updateOperation(any(), any(), eq(LifecycleOperationState.FAILED));

        assertThatThrownBy(() -> changeOperationContextBuilder.build(vnfInstance, operation, request))
                .isInstanceOf(PackageDetailsNotFoundException.class);
    }
}