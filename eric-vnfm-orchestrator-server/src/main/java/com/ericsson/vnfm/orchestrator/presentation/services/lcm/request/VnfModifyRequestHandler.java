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

import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.getCnfChartWithHighestPriority;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.MapUtils;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfModificationException;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class VnfModifyRequestHandler extends LifecycleRequestHandler {

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ExtensionsService extensionsService;

    @Autowired
    private ReplicaDetailsService replicaDetailsService;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private PackageService packageService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    private VnfdHelper vnfdHelper;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Override
    public LifecycleOperationType getType() {
        return LifecycleOperationType.MODIFY_INFO;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
        final VnfInfoModificationRequest vnfInfoModificationRequest = (VnfInfoModificationRequest) request;
        validatePreviousOperationStateCompleted(vnfInstance);
        final PackageResponse packageResponse = packageService.getPackageInfoWithDescriptorModel(vnfInstance.getVnfDescriptorId());
        //we do a reversed way to validate packageId fetching package by descriptorId
        //and then compare pkgId from modify request and package response id
        validateVnfPkgId(vnfInfoModificationRequest.getVnfPkgId(), packageResponse.getId());
        validateInstanceName(vnfInfoModificationRequest.getVnfInstanceName(), vnfInstance);
        validateMetadata(vnfInfoModificationRequest.getMetadata(), vnfInstance);
        validateExtensions(vnfInfoModificationRequest.getExtensions(), getPolicies(new JSONObject(packageResponse.getDescriptorModel())));
    }

    @Override
    public void updateInstance(final VnfInstance vnfInstance, final Object request, final LifecycleOperationType type,
                               final LifecycleOperation operation, final Map<String, Object> additionalParams) {
        final VnfInfoModificationRequest vnfInfoModificationRequest = (VnfInfoModificationRequest) request;
        this.updateInstanceFromModificationRequest(vnfInfoModificationRequest, vnfInstance);
        operation.setHelmClientVersion(vnfInstance.getHelmClientVersion());
        PackageResponse packageResponse = updatePackage(vnfInstance, vnfInfoModificationRequest);
        createAndPersistChangedInfo(vnfInfoModificationRequest, operation, packageResponse);
    }

    @Override
    public void createTempInstance(final VnfInstance vnfInstance, final Object request) {
        // no need to create temp instance
    }

    @Override
    public void sendRequest(final VnfInstance vnfInstance, LifecycleOperation operation, final Object request,
                            final Path toValuesFile) {
        final VnfInfoModificationRequest vnfInfoModificationRequest = (VnfInfoModificationRequest) request;
        var modifyRequestExtensions = checkAndCastObjectToMap(vnfInfoModificationRequest.getExtensions());
        if (!MapUtils.isEmpty(modifyRequestExtensions) && vnfInstance.getInstantiationState() == InstantiationState.INSTANTIATED
                && isSendRequestAllowed(modifyRequestExtensions, extractExtensions(vnfInstance))) {
            VnfInstance tempVnfInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            LifeCycleManagementHelper.updateOperationState(operation, LifecycleOperationState.PROCESSING);
            var workflowRoutingResponse = workflowRoutingService.routeChangePackageInfoRequest(
                    getCnfChartWithHighestPriority(tempVnfInstance).getPriority(),
                    operation,
                    tempVnfInstance);
            checkAndProcessFailedError(operation, workflowRoutingResponse, vnfInfoModificationRequest.getVnfPkgId());
        } else {
            vnfInfoModificationRequest.setExtensions(null);
            updateInstanceFromModificationRequest(vnfInfoModificationRequest, vnfInstance);
            MessageUtility.updateOperationAndInstanceOnCompleted(operation, vnfInstance.getInstantiationState(),
                                                                 vnfInstance, LifecycleOperationState.COMPLETED);
        }
    }

    private static Map<String, Object> extractExtensions(VnfInstance vnfInstance) {
        if (StringUtils.isNotEmpty(vnfInstance.getVnfInfoModifiableAttributesExtensions())) {
            return new JSONObject(vnfInstance.getVnfInfoModifiableAttributesExtensions()).toMap();
        } else {
            return new HashMap<>();
        }
    }

    private static boolean isSendRequestAllowed(final Map<String, Object> modifyRequestExtensions, final Map<String, Object> vnfInstanceExtensions) {
        Set<String> keysOfNewExtensions = new HashSet<>();
        for (Map.Entry<String, Object> modifyRequestEntries : modifyRequestExtensions.entrySet()) {
            if (!vnfInstanceExtensions.containsKey(modifyRequestEntries.getKey())) {
                return true;
            }
            Map<String, Object> instanceExtensions = checkAndCastObjectToMap(vnfInstanceExtensions.get(modifyRequestEntries.getKey()));
            Map<String, Object> requestExtensions = checkAndCastObjectToMap(modifyRequestEntries.getValue());
            findUpdatesToExtensions(instanceExtensions, requestExtensions, keysOfNewExtensions);
        }
        return !keysOfNewExtensions.isEmpty();
    }

    private static void findUpdatesToExtensions(final Map<String, Object> instanceExtensions,
                                                final Map<String, Object> requestExtensions,
                                                Set<String> keysOfNewExtensions) {
        for (Map.Entry<String, Object> modifyEntries : requestExtensions.entrySet()) {
            for (Map.Entry<String, Object> vnfInstanceEntries : instanceExtensions.entrySet()) {
                if (modifyEntries.getKey().equals(vnfInstanceEntries.getKey()) &&
                        !modifyEntries.getValue().equals(vnfInstanceEntries.getValue())) {
                    keysOfNewExtensions.add(modifyEntries.getKey());
                    break;
                }
            }
        }
    }

    private void createAndPersistChangedInfo(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                             final LifecycleOperation operation,
                                             final PackageResponse packageResponse) {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceDescription(vnfInfoModificationRequest.getVnfInstanceDescription());
        changedInfo.setVnfInstanceName(vnfInfoModificationRequest.getVnfInstanceName());

        if (vnfInfoModificationRequest.getMetadata() != null) {
            changedInfo.setMetadata(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
        }

        if (vnfInfoModificationRequest.getExtensions() != null) {
            changedInfo.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(vnfInfoModificationRequest.getExtensions()));
        }

        if (packageResponse != null) {
            changedInfo.setVnfPkgId(vnfInfoModificationRequest.getVnfPkgId());
            changedInfo.setVnfDescriptorId(packageResponse.getId());
            changedInfo.setVnfdVersion(packageResponse.getVnfdVersion());
            changedInfo.setVnfProductName(packageResponse.getVnfProductName());
            changedInfo.setVnfProviderName(packageResponse.getVnfProvider());
            changedInfo.setVnfSoftwareVersion(packageResponse.getVnfSoftwareVersion());
        }

        databaseInteractionService.persistChangedInfo(changedInfo, operation.getOperationOccurrenceId());
    }

    private static void validateVnfPkgId(final String vnfPkgId, final String responsePkgId) {
        if (StringUtils.isNotBlank(vnfPkgId) && !vnfPkgId.equals(responsePkgId)) {
            throw new VnfModificationException("VnfPackageId cannot be modified when packageId from onboarding doesn't match with the vnfPackageId");
        }
    }

    private static void validateInstanceName(final String vnfInstanceName, final VnfInstance vnfInstance) {
        if (StringUtils.isNotBlank(vnfInstanceName) && !Objects.equals(vnfInstanceName, vnfInstance.getVnfInstanceName())
                && vnfInstance.getInstantiationState() == InstantiationState.INSTANTIATED) {
            throw new VnfModificationException("Instance name cannot be modified in Instantiated state");
        }
    }

    private static void validateMetadata(final Map<String, String> metadata, final VnfInstance vnfInstance) {
        if (!MapUtils.isEmpty(metadata) && !Objects.equals(convertObjToJsonString(metadata), vnfInstance.getMetadata())
                && vnfInstance.getInstantiationState() == InstantiationState.INSTANTIATED) {
            throw new VnfModificationException("Metadata cannot be modified in Instantiated state");
        }
    }

    private void validateExtensions(final Object extensionsRequest, final Optional<Policies> policies) {
        var extensions = checkAndCastObjectToMap(extensionsRequest);
        if (extensionsRequest != null && MapUtils.isEmpty(extensions)) {
            throw new VnfModificationException("Extensions cannot be updated when it is provided as empty in request");
        }

        if (policies.isPresent() && !MapUtils.isEmpty(extensions)) {
            String policiesAsJsonString = super.getPoliciesAsJsonString(policies.get());
            extensionsService.validateVnfControlledScalingExtension(extensions, policiesAsJsonString);
        }
    }

    private void updateInstanceFromModificationRequest(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                                       final VnfInstance vnfInstance) {
        if (vnfInfoModificationRequest.getExtensions() != null && vnfInstance.getInstantiationState().equals(InstantiationState.INSTANTIATED)) {
            this.updateInInstantiatedStateWithExtensions(vnfInfoModificationRequest, vnfInstance);
        }

        if (vnfInfoModificationRequest.getExtensions() == null && vnfInstance.getInstantiationState() == InstantiationState.INSTANTIATED) {
            updateNonRestrictedFields(vnfInfoModificationRequest, vnfInstance);
        }

        if (vnfInstance.getInstantiationState() == InstantiationState.NOT_INSTANTIATED) {
            updateInNonInstantiatedState(vnfInfoModificationRequest, vnfInstance);
        }
    }

    private void updateInInstantiatedStateWithExtensions(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                                         final VnfInstance vnfInstance) {
        var extensions = checkAndCastObjectToMap(vnfInfoModificationRequest.getExtensions());
        final PackageResponse packageResponse =
                packageService.getPackageInfoWithDescriptorModel(vnfInstance.getVnfDescriptorId());
        InstanceUtils.createTempInstance(vnfInstance);
        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        tempInstance.setVnfInfoModifiableAttributesExtensions(vnfInstance.getVnfInfoModifiableAttributesExtensions());
        extensionsService.updateInstanceWithExtensionsInRequest(extensions, tempInstance);

        replicaDetailsService.setReplicaDetailsToVnfInstance(packageResponse.getDescriptorModel(),
                                                             tempInstance);
        updateNonRestrictedFields(vnfInfoModificationRequest, tempInstance);
        super.updateHelmChartWithVnfControlledScalingExtension(tempInstance);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        removeHelmChartsFromTempInstance(vnfInstance);
    }

    private void removeHelmChartsFromTempInstance(final VnfInstance vnfInstance) {
        try {
            VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
            scaleService.removeHelmChartFromTempInstance(vnfInstance, tempInstance);
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Exception occurred while removing helm charts from temp instance", e);
        }
    }

    private void updateInNonInstantiatedState(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                              final VnfInstance vnfInstance) {
        if (vnfInfoModificationRequest.getExtensions() != null) {
            extensionsService.updateInstanceWithExtensionsInRequest(
                    checkAndCastObjectToMap(vnfInfoModificationRequest.getExtensions()), vnfInstance);
        }

        if (StringUtils.isNotEmpty(vnfInfoModificationRequest.getVnfInstanceName())) {
            vnfInstance.setVnfInstanceName(vnfInfoModificationRequest.getVnfInstanceName());
        }

        if (!MapUtils.isEmpty(vnfInfoModificationRequest.getMetadata())) {
            vnfInstance.setMetadata(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
        }

        updateNonRestrictedFields(vnfInfoModificationRequest, vnfInstance);
    }

    private static void updateNonRestrictedFields(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                                  final VnfInstance vnfInstance) {
        if (vnfInfoModificationRequest.getVnfInstanceDescription() != null) {
            vnfInstance.setVnfInstanceDescription(vnfInfoModificationRequest.getVnfInstanceDescription());
        }

        if (StringUtils.isNotEmpty(vnfInfoModificationRequest.getVnfPkgId())) {
            vnfInstance.setVnfPackageId(vnfInfoModificationRequest.getVnfPkgId());
        }
    }

    private Optional<Policies> getPolicies(final JSONObject vnfd) {
        Optional<Policies> policies = Optional.empty();
        try {
            policies = vnfdHelper.getVnfdScalingInformation(vnfd);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Exception occurred while casting policy", e);
        }
        return policies;
    }

    private PackageResponse updatePackage(VnfInstance vnfInstance, VnfInfoModificationRequest vnfInfoModificationRequest) {
        PackageResponse packageResponse = null;
        String vnfPkgId = vnfInfoModificationRequest.getVnfPkgId();
        if (StringUtils.isNotBlank(vnfPkgId)) {
            instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(vnfInstance.getVnfPackageId(),
                                                                                             vnfPkgId,
                                                                                             vnfPkgId,
                                                                                             vnfInstance.getVnfInstanceId(),
                                                                                             true);
            packageResponse = packageService.getPackageInfoWithDescriptorModel(vnfInstance.getVnfDescriptorId());
        }
        return packageResponse;
    }
}
