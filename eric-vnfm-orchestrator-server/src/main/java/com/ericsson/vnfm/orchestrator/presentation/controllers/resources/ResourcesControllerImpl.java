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
package com.ericsson.vnfm.orchestrator.presentation.controllers.resources;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NO_LIFE_CYCLE_OPERATION_FOUND;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.DowngradeInfo;
import com.ericsson.vnfm.orchestrator.model.RollbackInfo;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.controllers.internal.EvnfmResourcesController;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ResourcesControllerImpl implements ResourcesController {

    @Autowired
    private ResourcesService resourcesService;
    @Autowired
    private WorkflowRoutingService workflowRoutingService;
    @Autowired
    private ScaleService scaleService;
    @Autowired
    private InstanceService instanceService;

    @Override
    public ResponseEntity<List<VnfResource>> getAllResource(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "getAllResources", required = false) boolean getAllResources) {
        if (Strings.isNullOrEmpty(filter)) {
            List<VnfResource> allVnfResources = resourcesService.getVnfResources(getAllResources);
            return new ResponseEntity<>(allVnfResources, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(resourcesService.getAllResourcesWithFilter(filter), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<VnfResource> getResource(@PathVariable("resourceId") String resourceId,
                                                   @RequestParam(value = "getAllResources", required = false) boolean getAllResources) {
        VnfResource vnfResource = resourcesService.getVnfResource(resourceId,
                                                                  getAllResources);
        if (null == vnfResource) {
            throw new NotFoundException(resourceId + NO_LIFE_CYCLE_OPERATION_FOUND);
        }
        return new ResponseEntity<>(vnfResource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getPodStatus(@PathVariable("resourceId") final String resourceId) {
        VnfInstance vnfInstance = resourcesService.getInstanceWithoutOperations(resourceId);
        checkVnfNotInState(vnfInstance, NOT_INSTANTIATED);
        ComponentStatusResponse componentStatusResponseResponse = workflowRoutingService.getComponentStatusRequest(vnfInstance);
        return new ResponseEntity<>(componentStatusResponseResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<VnfcScaleInfo>> getVnfcScaleInfo(@PathVariable("resourceId") final String resourceId,
                                                                @RequestParam(value = "aspectId") String aspectId,
                                                                @RequestParam(value = "type") String type,
                                                                @RequestParam(value = "numberOfSteps", required = false) String numberOfSteps) {
        final ScaleVnfRequest.TypeEnum typeEnum = ScaleVnfRequest.TypeEnum.fromValue(type);
        if (typeEnum == null) {
            throw new InvalidInputException("type value not supported - Supported values : SCALE_OUT, SCALE_IN");
        }
        final int numberOfStepsAsInt = EvnfmResourcesController.validateGetVnfcInfoRequestStepParameter(numberOfSteps);
        LOGGER.info(
                "Attempting to get VNFC Scale Info for instance {} with parameters aspectId={}, numberOfSteps={}, "
                        + "type={}", resourceId, aspectId, numberOfSteps, type);

        VnfInstance vnfInstance = resourcesService.getInstanceWithoutOperations(resourceId);
        List<VnfcScaleInfo> vnfcScaleInfoList = scaleService
                .getVnfcScaleInfoList(vnfInstance, typeEnum, numberOfStepsAsInt, aspectId);
        return new ResponseEntity<>(vnfcScaleInfoList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DowngradeInfo> getDowngradeInfo(@PathVariable(value = "resourceId") String instanceId) {
        return new ResponseEntity<>(instanceService.getDowngradePackagesInfo(instanceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RollbackInfo> getRollbackInfo(@PathVariable(value = "resourceId") String instanceId) {
        return new ResponseEntity<>(instanceService.getRollbackInfo(instanceId), HttpStatus.OK);
    }
}
