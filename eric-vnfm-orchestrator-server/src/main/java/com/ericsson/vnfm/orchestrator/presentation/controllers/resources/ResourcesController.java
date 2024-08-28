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

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.model.DowngradeInfo;
import com.ericsson.vnfm.orchestrator.model.RollbackInfo;
import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfcScaleInfo;

@RestController
@Validated
@RequestMapping("/vnflcm/v1/resources")
public interface ResourcesController {

    /**
     * Retrieve all vnf resources
     *
     * @return returns List<VnfResource>
     */
    @GetMapping
    ResponseEntity<List<VnfResource>> getAllResource(@RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "getAllResources", required = false) boolean getAllResources);

    /**
     * Retrieve a resource with id
     *
     * @return returns a VnfResource
     */
    @GetMapping("/{resourceId}")
    ResponseEntity<VnfResource> getResource(@PathVariable("resourceId") String resourceId,
            @RequestParam(value = "getAllResources", required = false) boolean getAllResources);
    /**
     * Gets the status of the pods
     * @return a list of pods for the resource
     */
    @GetMapping("/{resourceId}/pods")
    ResponseEntity<?> getPodStatus(@PathVariable("resourceId") String resourceId);

    /**
     * Gets the VNFC names, current replica count and expected replica count after scale operation has completed
     * @return a VnfcScaleInfo
     */
    @GetMapping("/{resourceId}/vnfcScaleInfo")
    ResponseEntity<List<VnfcScaleInfo>> getVnfcScaleInfo(@PathVariable String instanceId,
                                                         @RequestParam(value = "aspectId") String aspectId,
                                                         @RequestParam(value = "type") String type,
                                                         @RequestParam(value = "numberOfSteps", required = false) String numberOfSteps);

    /**
     * Returns  the downgrade info for the rollback operation
     *
     * @return a DowngradeInfo
     */
    @GetMapping("/{resourceId}/downgradeInfo")
    ResponseEntity<DowngradeInfo> getDowngradeInfo(@PathVariable(value = "resourceId") String instanceId);

    /**
     * Returns the info required to rollback operation from failed_temp state
     *
     * @return a RollbackInfo
     */
    @GetMapping("/{resourceId}/rollbackInfo")
    ResponseEntity<RollbackInfo> getRollbackInfo(@PathVariable(value = "resourceId") String instanceId);
}
