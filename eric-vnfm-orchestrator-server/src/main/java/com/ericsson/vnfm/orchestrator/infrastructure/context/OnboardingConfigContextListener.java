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
package com.ericsson.vnfm.orchestrator.infrastructure.context;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
public class OnboardingConfigContextListener implements ApplicationListener<ContextRefreshedEvent> {
    private volatile OnboardingConfig onboardingConfigCache;
    private final VnfInstanceMissingFieldUpdater vnfInstanceMissingFieldUpdater;

    public OnboardingConfigContextListener(VnfInstanceMissingFieldUpdater vnfInstanceMissingFieldUpdater) {
        this.vnfInstanceMissingFieldUpdater = vnfInstanceMissingFieldUpdater;
    }

    @Override
    @Async
    public synchronized void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("Checking onboarding config has been updated");

        OnboardingConfig onboardingConfig = event.getApplicationContext().getBean(OnboardingConfig.class);
        if (!onboardingConfig.equals(onboardingConfigCache)) {
            onboardingConfigCache = OnboardingConfig.builder()
                    .host(onboardingConfig.getHost())
                    .path(onboardingConfig.getPath())
                    .queryValue(onboardingConfig.getQueryValue())
                    .build();

            vnfInstanceMissingFieldUpdater.updateMissingFields();
        }
    }
}