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

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestDetailsCleanupJob {

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @Value("${idempotency.requestDetailsExpirationSeconds}")
    private long requestDetailsExpirationSeconds;

    @Transactional
    @Scheduled(fixedDelayString = "${idempotency.fixedDelay}")
    public void cleanUpOldRequestDetails() {

        List<RequestProcessingDetails> expiredRequestProcessingDetails =
                requestProcessingDetailsRepository.findAllExpiredRequestProcessingDetails(LocalDateTime.now()
                                                                                                  .minusSeconds(requestDetailsExpirationSeconds));
        if (!CollectionUtils.isEmpty(expiredRequestProcessingDetails)) {
            LOGGER.info("Cleaning {} expired request processing details", expiredRequestProcessingDetails.size());
            requestProcessingDetailsRepository.deleteAll(expiredRequestProcessingDetails);
        }
    }
}