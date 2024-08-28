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
package com.ericsson.vnfm.orchestrator.presentation.services.kubernetes;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Service
public class KubernetesService {

    private final KubernetesClient kubernetesClient;

    public KubernetesService() {
        this.kubernetesClient = new DefaultKubernetesClient();
    }


    public List<String> getPodNames() {
        return this.kubernetesClient.pods().list()
                .getItems()
                .stream()
                .filter(pod -> {
                    List<PodCondition> conditions = pod.getStatus().getConditions();
                    return conditions.stream()
                            .filter(cond -> "ContainersReady".equalsIgnoreCase(cond.getType()))
                            .anyMatch(condition -> "True".equalsIgnoreCase(condition.getStatus()));

                })
                .map(pod -> pod.getMetadata().getName()).collect(Collectors.toList());
    }
}
