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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToInstancesQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToLifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import okhttp3.OkHttpClient;

@Configuration
@Profile({ "test", "dev", "prod", "debug" })
@EnableCaching
public class Config {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Bean
    public LifecycleOperationQuery getLifecycleOperationQuery() {
        return new LifecycleOperationQuery(lifecycleOperationRepository);
    }

    @Bean
    public VnfInstanceQuery getVnfInstanceQuery() {
        return new VnfInstanceQuery(vnfInstanceRepository);
    }

    @Bean
    public VnfResourcesToInstancesQuery getVnfResourcesToInstancesQuery() {
        return new VnfResourcesToInstancesQuery(vnfInstanceRepository);
    }

    @Bean
    public VnfResourcesToLifecycleOperationQuery getVnfResourcesToLifecycleOperationQuery() {
        return new VnfResourcesToLifecycleOperationQuery(lifecycleOperationRepository);
    }

    @Bean
    public OkHttpClient getclient() {
        return new OkHttpClient();
    }
}
