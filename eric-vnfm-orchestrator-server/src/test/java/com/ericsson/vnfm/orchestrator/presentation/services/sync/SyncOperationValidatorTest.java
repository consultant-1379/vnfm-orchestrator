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
package com.ericsson.vnfm.orchestrator.presentation.services.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowable;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.AUTOSCALING_PARAM_MISMATCH;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.sync.TargetScaleDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SyncValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.google.common.collect.Streams;


@SpringBootTest(classes = {
        SyncOperationValidator.class
})
@MockBean(classes = {
        VnfInstanceService.class
})
public class SyncOperationValidatorTest {

    @Autowired
    private SyncOperationValidator syncOperationValidator;

    @Test
    public void testAutoscalingIsTheSame() {
        List<TargetScaleDetails> targetScaleDetails = buildTargetScaleDetailsWithAutoscaling(true, true);

        assertThatNoException().isThrownBy(() -> syncOperationValidator.validateAutoscalingEnabledIsSame(targetScaleDetails));
    }

    @Test
    public void testAutoscalingIsTheSameWithNullValues() {
        List<TargetScaleDetails> targetScaleDetails = buildTargetScaleDetailsWithAutoscaling(true, true, null);

        assertThatNoException().isThrownBy(() -> syncOperationValidator.validateAutoscalingEnabledIsSame(targetScaleDetails));
    }

    @Test
    public void testAutoscalingIsTheSameFail() {
        List<TargetScaleDetails> targetScaleDetails = buildTargetScaleDetailsWithAutoscaling(true, null, false);

        Throwable exception = catchThrowable(() -> syncOperationValidator
                .validateAutoscalingEnabledIsSame(targetScaleDetails));

        assertThat(exception).isInstanceOf(SyncValidationException.class);
        assertThat(((SyncValidationException)exception).getErrors())
                .contains(String.format(AUTOSCALING_PARAM_MISMATCH, "vdu0, vdu2"));
    }

    private List<TargetScaleDetails> buildTargetScaleDetailsWithAutoscaling(Boolean ... values) {
        return Streams.mapWithIndex(Arrays.stream(values),
                             (value, i) -> {
                                 String targetName = "vdu"+i;
                                 return new TargetScaleDetails("releaseName", targetName, null, value, 0);
                             }).collect(Collectors.toList());
    }
}