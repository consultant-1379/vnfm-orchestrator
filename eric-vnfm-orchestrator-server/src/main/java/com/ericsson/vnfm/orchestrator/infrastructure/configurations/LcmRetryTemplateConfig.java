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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.ericsson.vnfm.orchestrator.infrastructure.model.RetryProperties;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ServiceUnavailableException;

@Configuration
public class LcmRetryTemplateConfig {

    private RetryProperties retryProperties;

    @Value("${wfsRetryTemplate.backOffPeriod}")
    private Long backOffPeriodOfFillVerificationNamespaceUid;

    @Value("${wfsRetryTemplate.maxAttempts}")
    private Integer maxAttemptsOfFillVerificationNamespaceUid;

    @Autowired
    public LcmRetryTemplateConfig(final RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Bean
    @Qualifier("nfvoRetryTemplate")
    public RetryTemplate nfvoRetryTemplate() {
        RetryProperties.Service nfvo = retryProperties.getDefaultProperties();
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(nfvo.getBackOff());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
        timeoutRetryPolicy.setTimeout(nfvo.getRequestTimeout());

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(nfvo.getMaxAttempts());

        final BinaryExceptionClassifierRetryPolicy notRetryOnHttpClientError =
                new BinaryExceptionClassifierRetryPolicy(
                        new BinaryExceptionClassifier(List.of(HttpClientErrorException.class, AuthenticationException.class),
                                false));

        CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
        compositeRetryPolicy.setPolicies(new RetryPolicy[] {timeoutRetryPolicy, simpleRetryPolicy, notRetryOnHttpClientError});
        retryTemplate.setRetryPolicy(compositeRetryPolicy);

        return retryTemplate;
    }

    @Bean(name = "licenseRetryTemplate")
    public RetryTemplate licenseRetryTemplate() {
        RetryProperties.Service licenseRetryProperties = retryProperties.getLicenseConsumer();
        return RetryTemplate.builder()
                .retryOn(RestClientException.class)
                .fixedBackoff(licenseRetryProperties.getBackOff())
                .maxAttempts(licenseRetryProperties.getMaxAttempts())
                .build();
    }

    @Bean
    @Qualifier("keycloakRetryTemplate")
    public RetryTemplate keycloakRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(retryProperties.getDefaultProperties().getBackOff());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RetryPolicy retryPolicy = new SimpleRetryPolicy(retryProperties.getDefaultProperties().getMaxAttempts(),
                                                        Map.of(ServiceUnavailableException.class, Boolean.TRUE));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    @Qualifier("wfsRetryTemplate")
    public RetryTemplate wfsRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backOffPeriodOfFillVerificationNamespaceUid);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttemptsOfFillVerificationNamespaceUid,
                                                        Map.of(ResourceAccessException.class, Boolean.TRUE));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    @Qualifier("wfsRoutingRetryTemplate")
    public RetryTemplate wfsRoutingRetryTemplate() {
        RetryProperties.Service wfsRoutingRetryProperties = retryProperties.getWfsRouting();
        return RetryTemplate.builder()
                .retryOn(RestClientException.class)
                .fixedBackoff(wfsRoutingRetryProperties.getBackOff())
                .maxAttempts(wfsRoutingRetryProperties.getMaxAttempts())
                .build();
    }

    @Bean("redisRetryTemplate")
    public RetryTemplate redisRetryTemplate() {
        RetryProperties.Service redisRetryProperties = retryProperties.getDefaultProperties();
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(redisRetryProperties.getMaxAttempts(),
                                                                    Map.of(RedisConnectionFailureException.class, Boolean.TRUE));

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(redisRetryProperties.getBackOff());

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(simpleRetryPolicy);
        return retryTemplate;
    }
}
