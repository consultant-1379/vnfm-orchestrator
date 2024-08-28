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
package com.ericsson.vnfm.orchestrator.presentation.services.validator;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_CLIENT_VERSION_YAML;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HelmClientVersionValidator {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    public String validateAndGetHelmClientVersion(final Map<String, Object> additionalParams) {
        if (additionalParams == null || !additionalParams.containsKey(HELM_CLIENT_VERSION_YAML)) {
            return null;
        }

        final String helmClientVersion = (String) additionalParams.get(HELM_CLIENT_VERSION_YAML);
        if (helmClientVersion == null) {
            return null;
        }

        List<String> availableHelmVersions = getHelmClientVersions();

        if (!availableHelmVersions.contains(helmClientVersion)) {
            throw new IllegalArgumentException(String.format("Helm version %s is not supported, available options: %s",
                                                             helmClientVersion, availableHelmVersions));
        }

        return helmClientVersion;
    }

    private List<String> getHelmClientVersions() {
        HelmVersionsResponse helmVersionsResponse = workflowRoutingService.getHelmVersionsRequest();

        if (CollectionUtils.isNotEmpty(helmVersionsResponse.getHelmVersions())) {
            return helmVersionsResponse.getHelmVersions();
        }

        return Collections.emptyList();
    }
}
