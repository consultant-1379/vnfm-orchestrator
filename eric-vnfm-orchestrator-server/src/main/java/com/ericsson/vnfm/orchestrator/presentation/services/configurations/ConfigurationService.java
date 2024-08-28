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
package com.ericsson.vnfm.orchestrator.presentation.services.configurations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.CommonProductsInfoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.DiscoveryServicesConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EvnfmProductInfoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.EvnfmProductConfiguration;
import com.ericsson.vnfm.orchestrator.presentation.services.kubernetes.KubernetesService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.EvnfmProductConfigMapper;

@Service
public class ConfigurationService {

    private static final String PACKAGES_SERVICE = "packages";

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private EvnfmProductInfoConfig evnfmProductInfoConfig;

    @Autowired
    private CommonProductsInfoConfig commonProductsInfoConfig;

    @Autowired
    private KubernetesService kubernetesService;

    @Autowired
    private DiscoveryServicesConfig discoveryServices;

    @Autowired
    private EvnfmProductConfigMapper evnfmProductConfigMapper;

    public EvnfmProductConfiguration getConfiguration() {
        EvnfmProductConfiguration evnfmProductConfiguration = evnfmProductConfigMapper.toInternalModel(evnfmProductInfoConfig);
        EvnfmProductConfiguration commonProductsConfiguration = evnfmProductConfigMapper.toInternalModel(commonProductsInfoConfig);

        evnfmProductConfiguration.getDependencies().addAll(commonProductsConfiguration.getDependencies());

        Map<String, Boolean> services = getServices();
        evnfmProductConfiguration.setAvailability(services);

        return evnfmProductConfiguration;
    }

    private Map<String, Boolean> getServices() {
        Map<String, Boolean> services = new HashMap<>();
        List<String> pods = kubernetesService.getPodNames();

        discoveryServices.getServices().forEach((key, value) -> {
            boolean islisted = matchPodName(pods, value);
            services.put(key, islisted);
        });

        if (nfvoConfig.isEnabled()) {
            services.put(PACKAGES_SERVICE, !nfvoConfig.isEnabled());
        }

        return services;
    }

    private static boolean matchPodName(List<String> podNames, String name) {
        if (podNames.isEmpty()) {
            return false;
        }
        return podNames.stream().anyMatch(item -> item.contains(name));
    }
}
