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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class EvnfmWorkflowScaleRequestTest {

    @Test
    public void testEvnfmWorkflowScaleRequestWithFilteringAdditionalParameters() {
        Map<String, Object> inputAdditionalParameters = new HashMap<>();
        for (String value: EVNFM_PARAMS) {
            inputAdditionalParameters.put(value, true);
        }
        for (String value: EVNFM_PARAMS_FOR_WFS) {
            inputAdditionalParameters.put(value, true);
        }

        EvnfmWorkflowScaleRequest scaleRequest = new EvnfmWorkflowScaleRequest.EvnfmWorkFlowScaleBuilder(inputAdditionalParameters, "")
                .withScaleResources(Collections.emptyMap()).build();
        Map<String, Object> outputAdditionalParams = scaleRequest.getAdditionalParams();

        for (String value: EVNFM_PARAMS) {
            assertThat(outputAdditionalParams.containsKey(value)).isFalse();
        }
        for (String value: EVNFM_PARAMS_FOR_WFS) {
            assertThat(outputAdditionalParams.containsKey(value)).isTrue();
        }
    }
}