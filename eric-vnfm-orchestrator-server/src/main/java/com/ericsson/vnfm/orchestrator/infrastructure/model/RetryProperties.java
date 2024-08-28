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
package com.ericsson.vnfm.orchestrator.infrastructure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 Be aware that increase of the retry properties for the license service can greatly reduce usability
 of the EVNFM demo version that provided without a NeLs microservice.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retry")
public class RetryProperties {

    private long connectTimeout;
    private long readTimeout;
    private Service defaultProperties;
    private Service crypto;
    private Service licenseConsumer;
    private Service wfsRouting;

    public RetryProperties() {
        connectTimeout = 500L;
        readTimeout = 60000L;
        defaultProperties = new Service();
        crypto = new Service();
        crypto.setMaxAttempts(30);
        crypto.setRequestTimeout(300000L);
        licenseConsumer = new Service();
        licenseConsumer.setBackOff(1000L);
        licenseConsumer.setMaxAttempts(2);
        licenseConsumer.setRequestTimeout(500L);
        wfsRouting = new Service();
        wfsRouting.setBackOff(5000L);
        wfsRouting.setMaxAttempts(5);
    }

    @Getter
    @Setter
    public static class Service {
        private long requestTimeout;
        private int maxAttempts;
        private long backOff;

        public Service() {
            backOff = 5000L;
            maxAttempts = 10;
            requestTimeout = 300000L;
        }
    }
}
