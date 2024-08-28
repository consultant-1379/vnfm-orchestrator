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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.CLUSTER_CONFIG_DESCRIPTION_MAX_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.KUBE_NAMESPACES;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.presentation.services.WorkflowService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.validator.RespondentValidator;
import com.ericsson.vnfm.orchestrator.validator.context.ClusterRegistrationValidationContext;
import com.ericsson.workflow.orchestration.mgmt.model.v3.ClusterServerDetailsResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class ClusterRegistrationRespondentValidatorImpl implements
        RespondentValidator<ClusterRegistrationValidationContext, ClusterServerDetailsResponse> {

    private static final String CLUSTER_REGISTER_FAIL = "Cluster failed to register.";

    private final WorkflowService workflowService;
    private final ClusterConfigFileRepository configFileRepository;

    @Override
    public ClusterServerDetailsResponse validate(ClusterRegistrationValidationContext validationContext) {
        String originalFileName = validationContext.getClusterConfig().getOriginalFilename();
        String description = validationContext.getDescription();
        String crdNamespace = validationContext.getCrdNamespace();

        validateClusterDescription(description);

        if (KUBE_NAMESPACES.contains(crdNamespace)) {
            throw new ValidationException("Kubernetes reserved namespace cannot be used as a CRD namespace.", CLUSTER_REGISTER_FAIL, CONFLICT);
        }

        Optional<ClusterConfigFile> existingClusterConfigFile = configFileRepository.findByName(originalFileName);
        if (existingClusterConfigFile.isPresent()) {
            String configExistsExceptionMessage = "File with name " + originalFileName + " already exists.";
            throw new ValidationException(configExistsExceptionMessage, CLUSTER_REGISTER_FAIL, CONFLICT);
        }

        return workflowService.validateClusterConfigFile(validationContext.getClusterConfig().getResource());
    }

    public static void validateClusterDescription(final String description) {
        if (description != null && description.length() > CLUSTER_CONFIG_DESCRIPTION_MAX_SIZE) {
            throw new ValidationException("Description should not be longer then 250 characters", "Description validation failed",
                                          BAD_REQUEST);
        }
    }
}
