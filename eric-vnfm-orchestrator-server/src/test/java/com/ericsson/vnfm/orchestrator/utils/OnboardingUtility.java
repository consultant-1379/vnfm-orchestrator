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
package com.ericsson.vnfm.orchestrator.utils;

import static java.util.Collections.singleton;

import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;

public class OnboardingUtility {

    private OnboardingUtility() {
        //private
    }

    public static OnboardingConfig createOnboardingConfig(String host, String path, String queryValue) {
        OnboardingConfig onboardingConfig = new OnboardingConfig();
        onboardingConfig.setHost(host);
        onboardingConfig.setPath(path);
        onboardingConfig.setQueryValue(queryValue);

        return onboardingConfig;
    }

    public static RetryTemplate createOnboardingRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
        timeoutRetryPolicy.setTimeout(30000L);

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(4);

        final BinaryExceptionClassifierRetryPolicy notRetryOnHttpClientError =
                new BinaryExceptionClassifierRetryPolicy(
                        new BinaryExceptionClassifier(singleton(HttpClientErrorException.class), false));

        CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
        compositeRetryPolicy.setPolicies(new RetryPolicy[] {timeoutRetryPolicy, simpleRetryPolicy, notRetryOnHttpClientError});
        retryTemplate.setRetryPolicy(compositeRetryPolicy);

        return retryTemplate;
    }
}
