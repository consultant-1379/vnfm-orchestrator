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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.HelmVersionsResponse;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.WorkflowSecretResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * This class routes the Lifecycle operation request to the correct Workflow service.
 */
public interface WorkflowRoutingService {

    /**
     * Route an instantiate request to the correct Workflow service.
     *
     * @param vnfInstance
     * @param operation
     * @param instantiateVnfRequest
     *
     * @return
     */
    WorkflowRoutingResponse routeInstantiateRequest(VnfInstance vnfInstance, LifecycleOperation operation,
            InstantiateVnfRequest instantiateVnfRequest);

    /**
     * Route an instantiate request with a values file to the correct Workflow service
     *
     * @param vnfInstance
     * @param operation
     * @param instantiateVnfRequest
     * @param toValuesFile
     *
     * @return
     */
    WorkflowRoutingResponse routeInstantiateRequest(VnfInstance vnfInstance, LifecycleOperation operation,
            InstantiateVnfRequest instantiateVnfRequest, Path toValuesFile);

    /**
     * Route an instantiate request based on priority to the correct Workflow service
     *
     * @param priority
     * @param operation
     * @param vnfInstance
     *
     * @return
     */
    WorkflowRoutingResponse routeInstantiateRequest(int priority, LifecycleOperation operation,
            VnfInstance vnfInstance);

    /**
     * Route an instantiate request to be used in rollback patterns
     *
     * @param operation
     * @param vnfInstance
     * @param helmChart
     * @param additionalParams
     * @return
     */
    WorkflowRoutingResponse routeInstantiateRequest(VnfInstance vnfInstance,
                                                    LifecycleOperation operation,
                                                    HelmChart helmChart,
                                                    Map<String, Object> additionalParams,
                                                    Path toValuesFile);

    WorkflowRoutingResponse routeInstantiateRequest(VnfInstance vnfInstance,
                                                    LifecycleOperation operation,
                                                    HelmChart helmChart);

    /**
     * Route a terminate request to the correct Workflow service.
     *
     * @param vnfInstance
     * @param operation
     * @param additionalParams
     * @param releaseName
     *
     * @return
     */
    WorkflowRoutingResponse routeTerminateRequest(VnfInstance vnfInstance, LifecycleOperation operation,
            Map<String, Object> additionalParams, String releaseName);


    /**
     * Route a terminate request to the correct Workflow service.
     *
     * @param vnfInstance
     * @param operation
     * @param releaseName
     *
     * @return
     */
    WorkflowRoutingResponse routeTerminateRequest(VnfInstance vnfInstance, LifecycleOperation operation, String releaseName);

    /**
     * Route a scale request to correct Workflow service
     *
     * @param vnfInstance
     * @param operation
     * @param scaleVnfRequest
     *
     * @return Returs workflow routing response
     */
    WorkflowRoutingResponse routeScaleRequest(VnfInstance vnfInstance, LifecycleOperation operation,
            ScaleVnfRequest scaleVnfRequest);

    /**
     * Route an instantiate request based on priority to the correct Workflow service
     *
     * @param priority
     * @param operation
     * @param vnfInstance
     *
     * @return
     */
    WorkflowRoutingResponse routeScaleRequest(int priority, LifecycleOperation operation, VnfInstance vnfInstance);

    /**
     * Route a changePackageInfo request to the correct Workflow service.
     *
     * @param context
     *
     * @return
     */
    WorkflowRoutingResponse routeChangePackageInfoRequest(ChangeOperationContext context);


    /**
     * Route a changePackageInfo request to the correct Workflow service.
     *
     * @param context
     * @param priority
     *
     * @return
     */
    WorkflowRoutingResponse routeChangePackageInfoRequest(ChangeOperationContext context, int priority);

    /**
     * Route a changePackageInfo request to the correct Workflow service.
     *
     * @param context
     * @param toValuesFile
     * @param priority
     *
     * @return
     */
    WorkflowRoutingResponse routeChangePackageInfoRequest(ChangeOperationContext context, Path toValuesFile, int priority);

    /**
     * Route a change package info request based on priority to the correct Workflow service
     *
     * @param priority
     * @param operation
     * @param upgradedInstance
     *
     * @return
     */
    WorkflowRoutingResponse routeChangePackageInfoRequest(int priority, LifecycleOperation operation,
                                                          VnfInstance upgradedInstance);

    /**
     * Route a rollback request to the correct Workflow service.
     *
     * @param vnfInstance
     * @param operation
     * @param releaseName
     * @param revisionNumber
     *
     * @return
     */
    WorkflowRoutingResponse routeRollbackRequest(VnfInstance vnfInstance, LifecycleOperation operation,
            String releaseName, String revisionNumber);

    /**
     * Route a component status request to the correct Workflow service.
     *
     * @param vnfInstance
     *
     * @return ComponentStatusResponse
     */
    ComponentStatusResponse getComponentStatusRequest(VnfInstance vnfInstance);

    /**
     * Route a component status requests to the correct Workflow service.
     *
     * @param vnfInstances
     *
     * @return list of ComponentStatusResponse
     */
    List<ComponentStatusResponse> getComponentStatusRequest(List<VnfInstance> vnfInstances);

    /**
     * Route a request for checking if namespace is equal to EVNFM namespace to the correct Workflow service.
     *
     * @param namespace
     * @param clusterName
     *
     * @return list of ComponentStatusResponse
     */
    boolean isEvnfmNamespaceAndCluster(String namespace, String clusterName);

    /**
     * Route to get values to the correct Workflow service.
     *
     * @param vnfInstance
     * @param releaseName
     *
     * @return ChartValuesResponse values for specific chart
     */
    ResponseEntity<Map> getChartValuesRequest(VnfInstance vnfInstance, String releaseName);

    /**
     * Route a delete namespace request to the correct Workflow service.
     *
     * @param namespace
     * @param clusterName
     * @param releaseName
     * @param applicationTimeout
     * @param lifecycleOperationId
     *
     * @return
     */
    void routeDeleteNamespace(String namespace, String clusterName, String releaseName, String applicationTimeout, String lifecycleOperationId);

    /**
     * Route request to delete PVCs to the correct Workflow service.
     *
     * @param vnfInstance
     * @param releaseName
     * @param labels
     * @return
     */
    ResponseEntity<Object> routeDeletePvcRequest(VnfInstance vnfInstance, String releaseName, String lifecycleOperationId,
                                                 String... labels);

    /**
     * @param vnfInstance
     * @param operation
     * @param releaseName
     * @return
     */
    ResponseEntity<Object> routeDownsizeRequest(VnfInstance vnfInstance, LifecycleOperation operation, String releaseName);

    /***
     * Route an upgrade request with ChangeOperationContext to correct workflow service.
     *
     * @param context
     * @param priority
     *
     * @return
     */
    WorkflowRoutingResponse routeToEvnfmWfsUpgrade(ChangeOperationContext context, int priority);

    /***
     * Routes to upgrade request with ChangeOperationContext using values file to correct Workflow service.
     *
     * @param context
     * @param toValuesFile
     * @return
     */
    WorkflowRoutingResponse routeToEvnfmWfsUpgrade(ChangeOperationContext context, Path toValuesFile, int priority);

    WorkflowRoutingResponse routeToEvnfmWfsUpgrade(LifecycleOperation operation,
                                                   HelmChart currentHelmChart,
                                                   Map<String, Object> additionalParams,
                                                   Path valuesFile);

    /**
     * Routes to fake upgrade request during rollback at failure.
     * @param operation
     * @param originalInstance
     * @param currentHelmChart
     * @param additionalParams
     * @param toValuesFile
     * @return
     */
    WorkflowRoutingResponse routeToEvnfmWfsFakeUpgrade(VnfInstance originalInstance,
                                                       LifecycleOperation operation,
                                                       HelmChart currentHelmChart,
                                                       Map<String, Object> additionalParams,
                                                       Path toValuesFile);

    /**
     * Route to Workflow service to get all the the secrets in namespace
     *
     * @param clusterName
     * @param namespace
     *
     * @return workflowSecretResponse all secrets response
     */
    WorkflowSecretResponse routeToEvnfmWfsForGettingAllSecrets(String clusterName, String namespace);

    /**
     * Route to Workflow service to get all the the secrets in namespace
     *
     * @param secretName
     * @param key
     * @param keyContent
     * @param clusterName
     * @param namespace
     *
     * @return workflowSecretResponse all secrets response
     */
    ResponseEntity<Object> routeToEvnfmWfsForPatchingSecrets(String secretName, String key, String keyContent,
                                                             String clusterName, String namespace);

    /**
     * Route to workflow service to get all available helm versions
     *
     *
     * @return HelmVersionsResponse all available helm versions
     */
    HelmVersionsResponse getHelmVersionsRequest();
}
