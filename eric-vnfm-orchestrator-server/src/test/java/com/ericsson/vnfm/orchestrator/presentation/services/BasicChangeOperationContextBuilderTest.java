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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;

@ExtendWith(MockitoExtension.class)
public class BasicChangeOperationContextBuilderTest extends ChangeOperationContextBuilderTestCommon {

    @Mock
    private MessageUtility messageUtility;

    @Mock
    private AdditionalParamsUtils additionalParamsUtils;

    @InjectMocks
    private ChangeOperationContextBuilder changeOperationContextBuilder = new BasicChangeOperationContextBuilder();

    private VnfInstance vnfInstanceWithDownsizeCompleted;
    private VnfInstance vnfInstanceWithDownsizeFailed;
    private LifecycleOperation operationWithoutAutoRollback;
    private LifecycleOperation operationWithAutoRollback;
    private Map<String, Object> additionalParams;

    @BeforeEach
    public void setUp() throws Exception {
        vnfInstanceWithDownsizeCompleted = buildVnfInstance(LifecycleOperationState.COMPLETED);
        vnfInstanceWithDownsizeFailed = buildVnfInstance(LifecycleOperationState.FAILED);

        operationWithAutoRollback = buildLifecycleOperation(true);
        operationWithoutAutoRollback = buildLifecycleOperation(false);

        additionalParams = new HashMap<>();
        additionalParams.put("applicationTimeOut", "3600");
        additionalParams.put("skipVerification", "true");
        additionalParams.put("skipJobVerification", "true");
    }

    @Test
    public void buildWithDownsizeCompletedAndAutoRollbackDisabledSuccess() {
        ChangePackageOperationSubtype expectedType = ChangePackageOperationSubtype.UPGRADE;
        String expectedSourceVnfInstanceId = vnfInstanceWithDownsizeCompleted.getVnfInstanceId();
        String expectedOperationId = operationWithoutAutoRollback.getOperationOccurrenceId();
        int expectedAdditionalParamsSize = 3;

        when(additionalParamsUtils.convertAdditionalParamsToMap(any()))
                .thenReturn(additionalParams);

        ChangeOperationContext actual = changeOperationContextBuilder.build(
                vnfInstanceWithDownsizeCompleted, operationWithoutAutoRollback, new ChangeCurrentVnfPkgRequest());

        assertThat(actual.getChangePackageOperationSubtype()).isEqualTo(expectedType);
        assertThat(actual.getSourceVnfInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getSourceVnfInstance().getVnfDescriptorId()).isEqualTo(SOURCE_VNFD_ID);
        assertThat(actual.getTempInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getTempInstance().getVnfDescriptorId()).isEqualTo(TARGET_VNFD_ID);
        assertThat(actual.getSourcePackageInfo()).isNull();
        assertThat(actual.getTargetPackageInfo()).isNull();
        assertThat(actual.getOperation().getOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetVnfdId()).isEqualTo(TARGET_VNFD_ID);
        assertThat(actual.isDownsize()).isFalse();
        assertThat(actual.isAutoRollbackAllowed()).isFalse();

        assertThat(actual.getAdditionalParams()).hasSize(expectedAdditionalParamsSize);
    }

    @Test
    public void buildWithDownsizeFailedAndAutoRollbackEnabledSuccess() {
        ChangePackageOperationSubtype expectedType = ChangePackageOperationSubtype.UPGRADE;
        String expectedSourceVnfInstanceId = vnfInstanceWithDownsizeFailed.getVnfInstanceId();
        String expectedOperationId = operationWithAutoRollback.getOperationOccurrenceId();
        int expectedAdditionalParamsSize = 3;

        when(additionalParamsUtils.convertAdditionalParamsToMap(any()))
                .thenReturn(additionalParams);

        ChangeOperationContext actual = changeOperationContextBuilder.build(
                vnfInstanceWithDownsizeFailed, operationWithAutoRollback, new ChangeCurrentVnfPkgRequest());

        assertThat(actual.getChangePackageOperationSubtype()).isEqualTo(expectedType);
        assertThat(actual.getSourceVnfInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getSourceVnfInstance().getVnfDescriptorId()).isEqualTo(SOURCE_VNFD_ID);
        assertThat(actual.getTempInstance().getVnfInstanceId()).isEqualTo(expectedSourceVnfInstanceId);
        assertThat(actual.getTempInstance().getVnfDescriptorId()).isEqualTo(SOURCE_VNFD_ID);
        assertThat(actual.getTempInstance().getHelmCharts()).allMatch(helmChart -> helmChart.getState() == null);
        assertThat(actual.getSourcePackageInfo()).isNull();
        assertThat(actual.getTargetPackageInfo()).isNull();
        assertThat(actual.getOperation().getOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetOperationOccurrenceId()).isEqualTo(expectedOperationId);
        assertThat(actual.getTargetVnfdId()).isEqualTo(SOURCE_VNFD_ID);
        assertThat(actual.isDownsize()).isFalse();
        assertThat(actual.isAutoRollbackAllowed()).isFalse();

        assertThat(actual.getAdditionalParams()).hasSize(expectedAdditionalParamsSize);
    }
}