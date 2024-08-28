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
package com.ericsson.vnfm.orchestrator.presentation.controllers.internal;

import static com.ericsson.vnfm.orchestrator.model.entity.InstantiationState.NOT_INSTANTIATED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.LAST_STATE_CHANGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.SORT_COLUMNS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.SORT_COLUMN_MAPPINGS;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildLinks;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildPaginationInfo;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.PAGE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.evnfm.orchestrator.api.ResourcesApi;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.DowngradeInfo;
import com.ericsson.vnfm.orchestrator.model.PagedResourcesResponse;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.RollbackInfo;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class EvnfmResourcesController implements ResourcesApi {

    private ResourcesService resourcesService;
    private ScaleService scaleService;
    private InstanceService instanceService;
    private WorkflowRoutingService workflowRoutingService;

    @Override
    public ResponseEntity<PagedResourcesResponse> getAllResource(final String filter,
                                                                 final Boolean allResources,
                                                                 final Integer page,
                                                                 final Integer size,
                                                                 final List<String> sort) {
        Pageable pageable = new PaginationUtils.PageableBuilder()
                .defaults(Sort.by(Sort.Direction.DESC, LAST_STATE_CHANGE))
                .page(page).size(size).sort(sort, SORT_COLUMNS, SORT_COLUMN_MAPPINGS).build();
        Page<ResourceResponse> responsePage = resourcesService.getVnfResourcesPage(filter, allResources, pageable);
        PaginationInfo paginationInfo = buildPaginationInfo(responsePage);
        PagedResourcesResponse response = new PagedResourcesResponse()
                .items(responsePage.getContent())
                .links(buildLinks(paginationInfo, PAGE))
                .page(paginationInfo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DowngradeInfo> getDowngradeInfo(String instanceId) {
        return new ResponseEntity<>(instanceService.getDowngradePackagesInfo(instanceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ComponentStatusResponse> getPodStatus(final String resourceId) {
        VnfInstance vnfInstance = resourcesService.getInstanceWithoutOperations(resourceId);
        checkVnfNotInState(vnfInstance, NOT_INSTANTIATED);
        List<VimLevelAdditionalResourceInfo> pods =
                Optional.ofNullable(workflowRoutingService.getComponentStatusRequest(vnfInstance))
                        .map(ComponentStatusResponse::getPods).orElseGet(Collections::emptyList);
        return new ResponseEntity<>(new ComponentStatusResponse().pods(pods), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceResponse> getResource(final String resourceId) {
        return new ResponseEntity<>(resourcesService.getVnfResource(resourceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<RollbackInfo> getRollbackInfo(final String resourceId) {
        return new ResponseEntity<>(instanceService.getRollbackInfo(resourceId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<VnfcScaleInfo>> getVnfcScaleInfo(final String resourceId,
                                                          String aspectId, String type, String numberOfSteps) {
        final ScaleVnfRequest.TypeEnum typeEnum = ScaleVnfRequest.TypeEnum.fromValue(type);
        if (typeEnum == null) {
            throw new InvalidInputException("type value not supported - Supported values : SCALE_OUT, SCALE_IN");
        }
        final int numberOfStepsAsInt = validateGetVnfcInfoRequestStepParameter(numberOfSteps);
        LOGGER.info(
                "Attempting to get VNFC Scale Info for instance {} with parameters aspectId={}, numberOfSteps={}, "
                        + "type={}", resourceId, aspectId, numberOfSteps, type);

        VnfInstance vnfInstance = resourcesService.getInstanceWithoutOperations(resourceId);
        List<VnfcScaleInfo> vnfcScaleInfoList = scaleService
                .getVnfcScaleInfoList(vnfInstance, typeEnum, numberOfStepsAsInt, aspectId);
        return new ResponseEntity<>(vnfcScaleInfoList, HttpStatus.OK);
    }

    public static int validateGetVnfcInfoRequestStepParameter(String numberOfSteps) {
        int numberOfStepsAsInt = 1;
        if (StringUtils.isNotEmpty(numberOfSteps)) {
            try {
                numberOfStepsAsInt = Integer.parseInt(numberOfSteps);
                if (numberOfStepsAsInt == 0) {
                    LOGGER.info("Number of steps is equal to zero, setting {} default value", numberOfStepsAsInt);
                    numberOfStepsAsInt = 1;
                } else if (numberOfStepsAsInt <= 0) {
                    throw new IllegalArgumentException("Invalid scale step provided, Scale step should be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new InvalidInputException("Invalid scale step provided, Scale step should be a positive integer", e);
            }
        } else {
            LOGGER.info("Number of steps is equal to null or empty, setting {} default value", numberOfStepsAsInt);
        }
        return numberOfStepsAsInt;
    }

    @Autowired
    public void setResourcesService(final ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Autowired
    public void setScaleService(final ScaleService scaleService) {
        this.scaleService = scaleService;
    }

    @Autowired
    public void setInstanceService(final InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Autowired
    public void setWorkflowRoutingService(final WorkflowRoutingService workflowRoutingService) {
        this.workflowRoutingService = workflowRoutingService;
    }
}
