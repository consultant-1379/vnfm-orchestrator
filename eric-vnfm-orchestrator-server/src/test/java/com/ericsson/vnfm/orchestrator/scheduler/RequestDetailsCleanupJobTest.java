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
package com.ericsson.vnfm.orchestrator.scheduler;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.ProcessingState;
import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;

@SpringBootTest(classes = {RequestDetailsCleanupJob.class})
public class RequestDetailsCleanupJobTest {

    @Autowired
    private RequestDetailsCleanupJob requestDetailsCleanupJob;
    @MockBean
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @Test
    public void testCleanUpOldRequestDetails() {
        RequestProcessingDetails details = createDummyRequestDetails();
        when(requestProcessingDetailsRepository.findAllExpiredRequestProcessingDetails(any()))
                .thenReturn(List.of(details));

        requestDetailsCleanupJob.cleanUpOldRequestDetails();
        verify(requestProcessingDetailsRepository, atLeastOnce()).deleteAll(any());
    }

    public RequestProcessingDetails createDummyRequestDetails() {
        RequestProcessingDetails processingDetails = new RequestProcessingDetails();
        processingDetails.setId("dummy-request-id");
        processingDetails.setProcessingState(ProcessingState.FINISHED);
        processingDetails.setResponseCode(201);
        processingDetails.setRetryAfter(5);
        processingDetails.setRequestHash("dummy-hash-sum");
        processingDetails.setCreationTime(LocalDateTime.now().minusSeconds(2));
        return  processingDetails;
    }
}