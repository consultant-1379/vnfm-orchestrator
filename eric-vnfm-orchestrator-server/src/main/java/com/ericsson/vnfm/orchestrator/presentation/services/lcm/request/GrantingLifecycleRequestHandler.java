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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static java.lang.String.format;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.GRANTING_FAILED;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.granting.StructureLinks;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantRequest;
import com.ericsson.vnfm.orchestrator.model.granting.request.GrantedLcmOperationType;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.response.Grant;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.GrantingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ServiceUnavailableException;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.utils.UrlUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GrantingLifecycleRequestHandler extends LifecycleRequestHandler implements GrantingRequestHandler {

    @Value("${vnfm.host}")
    private String vnfmHost;

    @Autowired
    private GrantingService grantingService;

    @Autowired
    protected GrantingResourceDefinitionCalculation grantingResourceDefinitionCalculation;

    @Override
    public void verifyGrantingResources(LifecycleOperation operation, final Object request, final Map<String, Object> valuesYamlMap) {
        final VnfInstance vnfInstance = operation.getVnfInstance();

        if (vnfInstance.isRel4()) {
            doVerifyGrantingResources(operation, request, valuesYamlMap);
        } else {
            LOGGER.debug("Granting not supported since instance is not rel4");
        }
    }

    protected abstract void doVerifyGrantingResources(LifecycleOperation operation,
                                                      Object request,
                                                      Map<String, Object> valuesYamlMap);

    protected abstract GrantedLcmOperationType getGrantingOperationType();

    protected void fillAndExecuteGrantRequest(List<ResourceDefinition> resourcesToAdd,
                                              List<ResourceDefinition> resourcesToRemove,
                                              VnfInstance vnfInstance,
                                              LifecycleOperation operation,
                                              GrantRequest grantRequest) {
        grantRequest.setAddResources(resourcesToAdd);
        grantRequest.setRemoveResources(resourcesToRemove);

        if (areGrantingResourcesEmpty(grantRequest)) {
            LOGGER.info("No changes in resource definitions are detected or not rel4 package is processed. "
                                + "Finished granting validation. Package ID: {}", vnfInstance.getVnfPackageId());
            return;
        }

        fillGrantRequestCommonFields(grantRequest, vnfInstance, getGrantingOperationType());

        executeGrantRequest(grantRequest, operation);
    }

    private boolean areGrantingResourcesEmpty(GrantRequest grantRequest) {
        return CollectionUtils.isEmpty(grantRequest.getAddResources())
                && CollectionUtils.isEmpty(grantRequest.getRemoveResources())
                && CollectionUtils.isEmpty(grantRequest.getUpdateResources());
    }

    private void fillGrantRequestCommonFields(GrantRequest grantRequest,
                                              VnfInstance vnfInstance,
                                              GrantedLcmOperationType lcmOperationType) {
        grantRequest.setVnfInstanceId(vnfInstance.getVnfInstanceId());
        grantRequest.setVnfdId(vnfInstance.getVnfDescriptorId());
        grantRequest.setOperation(lcmOperationType);
        grantRequest.setVnfLcmOpOccId(vnfInstance.getOperationOccurrenceId());

        StructureLinks links = new StructureLinks();
        links.setVnfInstance(UrlUtils.getVnfInstanceLink(vnfmHost, vnfInstance.getVnfInstanceId()));
        links.setVnfLcmOpOcc(UrlUtils.getVnfLcmOpOccLink(vnfmHost, vnfInstance.getOperationOccurrenceId()));
        grantRequest.setLinks(links);
    }

    private void executeGrantRequest(GrantRequest grantRequest, LifecycleOperation operation) {
        VnfInstance vnfInstance = operation.getVnfInstance();
        ResponseEntity<Grant> grantResponse = grantingService.executeGrantRequest(grantRequest);
        HttpStatusCode grantResponseStatus = grantResponse.getStatusCode();
        if (grantResponseStatus.value() == HttpStatus.FORBIDDEN.value()) {
            String grantingForbiddenMessage = format(GRANTING_FAILED, vnfInstance.getVnfInstanceId(), vnfInstance.getVnfPackageId());
            lcmOpErrorManagementService.process(operation, INTERNAL_SERVER_ERROR,
                                                "Granting Request wasn't confirmed by NFVO", grantingForbiddenMessage);
            throw new GrantingException(grantingForbiddenMessage);
        }
        if (grantResponseStatus.is5xxServerError() || grantResponseStatus.is4xxClientError()) {
            String grantingUnavailableMessage = format("Error occurs due to failed Granting request for VNF %s", grantRequest.getVnfInstanceId());
            lcmOpErrorManagementService.process(operation, INTERNAL_SERVER_ERROR,
                                                "NFVO Service call failed", grantingUnavailableMessage);
            throw new ServiceUnavailableException("NFVO", grantingUnavailableMessage);
        }
    }
}

