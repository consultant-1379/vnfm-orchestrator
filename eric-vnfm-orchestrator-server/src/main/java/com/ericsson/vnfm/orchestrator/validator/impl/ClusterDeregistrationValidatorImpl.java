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
package com.ericsson.vnfm.orchestrator.validator.impl;

import static org.springframework.http.HttpStatus.CONFLICT;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NOT_FOUND;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.CLUSTER_NOT_FOUND_MESSAGE;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.validator.Validator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ClusterDeregistrationValidatorImpl implements Validator<String> {

    private final ClusterConfigFileRepository configFileRepository;

    @Override
    public void validate(String clusterConfigName) {
        String configNotFoundMessage = String.format(CLUSTER_NOT_FOUND_MESSAGE, clusterConfigName);
        ClusterConfigFile targetClusterConfigFile = configFileRepository.findByName(clusterConfigName)
                .orElseThrow(() -> new ValidationException(configNotFoundMessage, CLUSTER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (targetClusterConfigFile.isDefault()) {
            throw new ValidationException("Default cluster config can not be deregister.", "Cluster operation failed.", CONFLICT);
        }

        if (targetClusterConfigFile.getStatus() == ConfigFileStatus.IN_USE) {
            String clusterConfigInUseMessage = String.format("Cluster config file %s is in use and not available for deletion.", clusterConfigName);
            throw new ValidationException(clusterConfigInUseMessage, "Cluster config file is in use and cannot be removed", HttpStatus.CONFLICT);
        }
    }

}
