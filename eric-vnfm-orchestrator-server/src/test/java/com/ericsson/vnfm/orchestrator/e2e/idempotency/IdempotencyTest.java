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
package com.ericsson.vnfm.orchestrator.e2e.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.aspect.IdempotencyAspect;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ProcessingState;
import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;

public class IdempotencyTest extends AbstractEndToEndTest {

    private static final String TEST_CONFIG_PATH = "configs/cluster01.config";

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @SpyBean
    private IdempotencyService idempotencyService;

    @SpyBean
    private IdempotencyAspect idempotencyAspect;

    @Test
    public void testIdempotencyFlowPositiveAndNegative() throws Throwable {
        requestHelper.deregisterCluster(FilenameUtils.getName(TEST_CONFIG_PATH));

        final String description = "";
        final UUID idempotencyKeyPositive = UUID.randomUUID();
        final UUID idempotencyKeyNegative = UUID.randomUUID();
        int countOfCallIdempotencyAspect = mockingDetails(idempotencyAspect).getInvocations().size();
        final MvcResult registerResult = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description, idempotencyKeyPositive);

        verify(idempotencyService, times(1)).createProcessingRequest(eq(String.valueOf(idempotencyKeyPositive)), any(), any());
        // TODO uncomment when the functionality is ready
        // verify(idempotencyService, times(1)).saveIdempotentResponse(String.valueOf(idempotencyKeyPositive), any());

        assertThat(registerResult.getResponse().getStatus()).isEqualTo(201);

        final String positiveResponseContent = registerResult.getResponse().getContentAsString();
        final Optional<RequestProcessingDetails> positiveRequestProcessingDetailsOptional = requestProcessingDetailsRepository.findById(
                String.valueOf(idempotencyKeyPositive));

        positiveRequestProcessingDetailsOptional.ifPresentOrElse(requestProcessingDetails -> {
            //  assertThat(requestProcessingDetails.getProcessingState()).isEqualTo(ProcessingState.FINISHED);
            //  assertThat(requestProcessingDetails.getResponseBody()).isEqualTo(positiveResponseContent);
        }, () -> fail(String.format("RequestProcessingDetails not found, id = %s", idempotencyKeyPositive)));

        final MvcResult registerExistResult = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description, idempotencyKeyNegative);

        countOfCallIdempotencyAspect += 1;

        assertThat(registerExistResult.getResponse().getStatus()).isEqualTo(409);

        verify(idempotencyService, times(1)).createProcessingRequest(eq(String.valueOf(idempotencyKeyNegative)), any(), any());
        verify(idempotencyService, times(1)).saveIdempotentResponse(eq(String.valueOf(idempotencyKeyNegative)), any());
        verify(idempotencyAspect, times(countOfCallIdempotencyAspect)).around(any());

        final String negativeResponseContent = registerExistResult.getResponse().getContentAsString();

        final Optional<RequestProcessingDetails> negativeRequestProcessingDetailsOptional = requestProcessingDetailsRepository.findById(
                String.valueOf(idempotencyKeyNegative));

        negativeRequestProcessingDetailsOptional.ifPresentOrElse(requestProcessingDetails -> {
            assertThat(requestProcessingDetails.getProcessingState()).isEqualTo(ProcessingState.FINISHED);
            assertThat(requestProcessingDetails.getResponseBody()).isEqualTo(negativeResponseContent);
        }, () -> fail(String.format("RequestProcessingDetails not found, id = %s", idempotencyKeyNegative)));

        final MvcResult repeatRegisterExistResult = requestHelper.registerNewCluster(TEST_CONFIG_PATH, description, idempotencyKeyNegative);

        assertThat(registerExistResult.getResponse().getStatus()).isEqualTo(409);
        verify(idempotencyService, times(1)).createProcessingRequest(eq(String.valueOf(idempotencyKeyNegative)), any(), any());
        verify(idempotencyService, times(1)).saveIdempotentResponse(eq(String.valueOf(idempotencyKeyNegative)), any());
        verify(idempotencyAspect, times(countOfCallIdempotencyAspect)).around(any());
        assertThat(repeatRegisterExistResult.getResponse().getContentAsString()).isEqualTo(negativeResponseContent);
    }
}
