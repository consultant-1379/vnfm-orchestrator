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

import static java.util.Collections.emptyMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.am.shared.vnfd.PolicyUtility.createPolicies;
import static com.ericsson.vnfm.orchestrator.TestUtils.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.TestUtils.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.TestUtils.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.TestUtils.createExtensions;
import static com.ericsson.vnfm.orchestrator.TestUtils.createVnfInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.getResource;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.REPLICA_DETAILS_MAP_TYPE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfModificationException;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class VnfModifyRequestHandlerTest {

    private static final String DESCRIPTION = "Description test";

    @Mock
    private WorkflowRoutingService workflowRoutingService;

    @Mock
    private InstanceService instanceService;

    @Mock
    private PackageService packageService;

    @Mock
    private ExtensionsService extensionsService;

    @Mock
    private ReplicaDetailsMapper replicaDetailsMapper;

    @Spy
    private ObjectMapper mapper;

    @Mock
    private DatabaseInteractionService databaseInteractionService;

    @Mock
    private ReplicaDetailsService replicaDetailsService;

    @Mock
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Spy
    private ScaleServiceImpl scaleService;

    @Mock
    private LifecycleOperationHelper lifecycleOperationHelper;

    @Mock
    private LcmOpSearchService lcmOpSearchService;

    @Mock
    private VnfdHelper vnfdHelper;

    @Mock
    private VnfInstanceService vnfInstanceService;

    @InjectMocks
    private VnfModifyRequestHandler vnfModifyRequestHandler;

    private static String vnfd;

    @BeforeEach
    public void init() throws IOException, URISyntaxException {
        vnfd = getFile("vnfdForModifyRequest.json");
    }

    @Test
    public void testSpecificValidationExtensions() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        var extensions = createExtensions();
        vnfInfoModificationRequest.setExtensions(extensions);
        var vnfdJson = new JSONObject(vnfd);
        var policies = createPolicies(vnfdJson);

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(createPackageResponse());
        when(vnfdHelper.getVnfdScalingInformation(any()))
                .thenReturn(Optional.of(policies));

        vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);

        verify(extensionsService, times(1))
                .validateVnfControlledScalingExtension(extensions, getPoliciesAsJsonString(policies));
    }

    @Test
    public void testSpecificValidationPreviousOperationFailed() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        var extensions = createExtensions();
        vnfInfoModificationRequest.setExtensions(extensions);
        LifecycleOperation operation = new LifecycleOperation();
        operation.setStateEnteredTime(LocalDateTime.now());
        operation.setOperationState(LifecycleOperationState.FAILED);
        operation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);

        when(lcmOpSearchService.searchLastOperation(any())).thenReturn(Optional.of(operation));

        assertThatThrownBy(() -> vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Previous Lifecycle operation state: FAILED, but has to be COMPLETED in order to perform MODIFY_INFO operation.");
    }

    @Test
    public void testSpecificValidationExtensionsNullSkipped() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(null);
        var vnfdJson = new JSONObject(vnfd);
        var policies = createPolicies(vnfdJson);

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(createPackageResponse());
        when(vnfdHelper.getVnfdScalingInformation(any()))
                .thenReturn(Optional.of(policies));

        vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);

        verify(extensionsService, times(0))
                .validateVnfControlledScalingExtension(null, getPoliciesAsJsonString(policies));
    }

    @Test
    public void testSpecificValidationExtensionsNotPassed() {
        Assertions.assertThrows(VnfModificationException.class, () -> {
            final VnfInstance vnfInstance = createVnfInstance(true);
            final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
            final Map<String, Object> extensions = emptyMap();
            vnfInfoModificationRequest.setExtensions(extensions);
            var vnfdJson = new JSONObject(vnfd);
            var policies = createPolicies(vnfdJson);

            when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                    .thenReturn(createPackageResponse());
            when(vnfdHelper.getVnfdScalingInformation(any()))
                    .thenReturn(Optional.of(policies));

            vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);

            verify(extensionsService, times(0))
                    .validateVnfControlledScalingExtension(extensions, getPoliciesAsJsonString(policies));
        });

    }

    @Test
    public void testSpecificValidationVnfInstanceNameNotPassedInInstantiatedState() {
        Assertions.assertThrows(VnfModificationException.class, () -> {
            final VnfInstance vnfInstance = createVnfInstance(true);
            vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
            final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
            vnfInfoModificationRequest.setVnfInstanceName(RandomStringUtils.randomAlphabetic(10));

            when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                    .thenReturn(createPackageResponse());

            vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);
        });
    }

    @Test
    public void testSpecificValidationMetadaInInstantiatedState() {
        Assertions.assertThrows(VnfModificationException.class, () -> {
            final VnfInstance vnfInstance = createVnfInstance(true);
            vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
            final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
            vnfInfoModificationRequest.setMetadata(
                    Collections.singletonMap(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10))
            );

            when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                    .thenReturn(createPackageResponse());

            vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);
        });
    }

    @Test
    public void testSpecificValidationVnfPackageIdNotPassedWhenIdsAreDifferent() {
        Assertions.assertThrows(VnfModificationException.class, () -> {
            final VnfInstance vnfInstance = createVnfInstance(true);
            vnfInstance.setVnfInstanceId(RandomStringUtils.randomAlphanumeric(15));
            final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
            vnfInfoModificationRequest.setVnfPkgId(RandomStringUtils.randomAlphanumeric(10));

            when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                    .thenReturn(createPackageResponse());

            vnfModifyRequestHandler.specificValidation(vnfInstance, vnfInfoModificationRequest);
        });
    }

    @Test
    public void testSendRequestWhenExtensionsAreModifiedInInstantiatedState() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInstance tempInstance = createVnfInstance(true);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(createExtensions());
        var workflowRoutingResponse = createResponse();
        var lifecycleOperation = createLifecycleOperation();

        when(workflowRoutingService.routeChangePackageInfoRequest(
                Mockito.anyInt(), any(), any()))
                .thenReturn(workflowRoutingResponse);

        vnfModifyRequestHandler.sendRequest(vnfInstance, lifecycleOperation, vnfInfoModificationRequest, null);

        assertThat(lifecycleOperation.getOperationState()).isNotEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    @Test
    public void testSendRequestOnAnInstanceWithoutExtensionsInInstantiatedState() {
        final VnfInstance vnfInstance = createVnfInstance(false);
        final VnfInstance tempInstance = createVnfInstance(true);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setVnfInstanceDescription(DESCRIPTION);
        var lifecycleOperation = createLifecycleOperation();

        vnfModifyRequestHandler.sendRequest(vnfInstance, lifecycleOperation, vnfInfoModificationRequest, null);

        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getVnfInstanceDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    public void testSendRequestWhenProvidedExtensionsIdentical() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInstance tempInstance = createVnfInstance(true);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        var extensions = createExtensions();
        vnfInfoModificationRequest.setExtensions(extensions);
        vnfInfoModificationRequest.setVnfInstanceDescription(DESCRIPTION);
        vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
        var lifecycleOperation = createLifecycleOperation();

        vnfModifyRequestHandler.sendRequest(vnfInstance, lifecycleOperation, vnfInfoModificationRequest, null);

        verify(workflowRoutingService, times(0))
                .routeChangePackageInfoRequest(0, null, vnfInstance);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(vnfInstance.getVnfInstanceDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    public void testSendRequestWhenExtensionsAreModifiedInInstantiatedStateError() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            final VnfInstance vnfInstance = createVnfInstance(true);
            final VnfInstance tempInstance = createVnfInstance(true);
            vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
            vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
            final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
            vnfInfoModificationRequest.setExtensions(createExtensions());
            var workflowRoutingResponse = createResponse();
            workflowRoutingResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            var lifecycleOperation = createLifecycleOperation();

            when(workflowRoutingService.routeChangePackageInfoRequest(
                    Mockito.anyInt(), any(), any()))
                    .thenReturn(workflowRoutingResponse);

            vnfModifyRequestHandler.sendRequest(vnfInstance, lifecycleOperation, vnfInfoModificationRequest, null);

            verify(workflowRoutingService, times(1))
                    .routeChangePackageInfoRequest(0, null, vnfInstance);
        });
    }

    @Test
    public void testSendRequestWhenExtensionsAreNotModifiedInInstantiatedState() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInstance tempInstance = createVnfInstance(true);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(null);
        var lifecycleOperation = createLifecycleOperation();

        vnfModifyRequestHandler.sendRequest(vnfInstance, lifecycleOperation, vnfInfoModificationRequest, null);

        verify(workflowRoutingService, times(0))
                .routeChangePackageInfoRequest(1, null, vnfInstance);
        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.COMPLETED);
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
    }

    @Test
    public void testUpdateInNonInstantiatedState() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        vnfInstance.setVnfInstanceDescription("description");
        var vnfInfoModificationRequest = createVnfInfoModificationRequest();
        vnfInfoModificationRequest.setVnfInstanceDescription(null);
        var lifecycleOperation = createLifecycleOperation();

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(createPackageResponse());

        vnfModifyRequestHandler.updateInstance(vnfInstance, vnfInfoModificationRequest, LifecycleOperationType.MODIFY_INFO, lifecycleOperation,  new HashMap<>());

        verify(extensionsService, times(1))
                .updateInstanceWithExtensionsInRequest(checkAndCastObjectToMap(vnfInfoModificationRequest.getExtensions()), vnfInstance);

        assertThat(vnfInstance.getVnfInstanceName())
                .isEqualTo(vnfInfoModificationRequest.getVnfInstanceName());
        assertThat(vnfInstance.getVnfInstanceDescription()).isNotNull();
        assertThat(vnfInstance.getVnfPackageId())
                .isEqualTo(vnfInfoModificationRequest.getVnfPkgId());
        assertThat(vnfInstance.getMetadata())
                .isEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
    }

    @Test
    public void testUpdatePackageStateIsChangedWhenPkgIdIsUpdated() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        var vnfInfoModificationRequest = createVnfInfoModificationRequest();
        var lifecycleOperation = createLifecycleOperation();
        var packageResponse = createPackageResponse();
        ChangedInfo changedInfo = createdChangedInfo(vnfInfoModificationRequest, packageResponse);

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(packageResponse);

        vnfModifyRequestHandler.updateInstance(vnfInstance, vnfInfoModificationRequest, LifecycleOperationType.MODIFY_INFO, lifecycleOperation,  new HashMap<>());

        verify(instanceService, times(1))
                .updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(vnfInstance.getVnfPackageId(),
                                                                                  vnfInfoModificationRequest.getVnfPkgId(),
                                                                                  vnfInfoModificationRequest.getVnfPkgId(),
                                                                                  vnfInstance.getVnfInstanceId(),
                                                                                  true);
        verify(databaseInteractionService, times(1))
                .persistChangedInfo(changedInfo, lifecycleOperation.getOperationOccurrenceId());
    }

    @Test
    public void testUpdatePackageStateIsNotChangedWhenPkgIdIsNotUpdated() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        var vnfInfoModificationRequest = createVnfInfoModificationRequest();
        vnfInfoModificationRequest.setVnfPkgId(null);
        var lifecycleOperation = createLifecycleOperation();
        ChangedInfo changedInfo = createdChangedInfoWithoutPackage(vnfInfoModificationRequest);

        vnfModifyRequestHandler.updateInstance(vnfInstance, vnfInfoModificationRequest, LifecycleOperationType.MODIFY_INFO, lifecycleOperation,  new HashMap<>());

        verify(instanceService, times(0))
                .updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(vnfInstance.getVnfPackageId(),
                                                                                  vnfInfoModificationRequest.getVnfPkgId(),
                                                                                  vnfInfoModificationRequest.getVnfPkgId(),
                                                                                  vnfInstance.getVnfInstanceId(),
                                                                                  true);
        verify(databaseInteractionService, times(1))
                .persistChangedInfo(changedInfo, lifecycleOperation.getOperationOccurrenceId());

        assertThat(vnfInstance.getVnfPackageId())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfPkgId());
    }

    @Test
    public void testUpdateInInstantiatedStateWithoutExtensions() {
        final VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        var vnfInfoModificationRequest = createVnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(null);
        vnfInfoModificationRequest.setMetadata(null);
        var lifecycleOperation = createLifecycleOperation();
        var packageResponse = createPackageResponse();
        final ChangedInfo changedInfo = createdChangedInfoWithoutMetadataAndExtensions(vnfInfoModificationRequest, packageResponse);

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(packageResponse);

        vnfModifyRequestHandler.updateInstance(vnfInstance, vnfInfoModificationRequest, LifecycleOperationType.MODIFY_INFO, lifecycleOperation,  new HashMap<>());

        assertThat(vnfInstance.getVnfInstanceName())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfInstanceName());
        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions())
                .isNotEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getExtensions()));
        assertThat(vnfInstance.getVnfInstanceDescription())
                .isEqualTo(vnfInfoModificationRequest.getVnfInstanceDescription());
        assertThat(vnfInstance.getVnfPackageId())
                .isEqualTo(vnfInfoModificationRequest.getVnfPkgId());
        assertThat(vnfInstance.getMetadata())
                .isNotEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));

        verify(databaseInteractionService, times(1))
                .persistChangedInfo(changedInfo, lifecycleOperation.getOperationOccurrenceId());
    }

    @Test
    public void testUpdateInInstantiatedStateWithExtensions() throws JsonProcessingException {
        final VnfInstance vnfInstance = createVnfInstance(true);
        final VnfInstance tempInstance = createVnfInstance(true);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        var vnfInfoModificationRequest = createVnfInfoModificationRequest();
        var packageResponse = createPackageResponse();
        var lifecycleOperation = createLifecycleOperation();

        final ChangedInfo changedInfo = createdChangedInfo(vnfInfoModificationRequest, packageResponse);

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(packageResponse);
        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(createPackageResponse());
        Mockito.doCallRealMethod().when(scaleService).removeHelmChartFromTempInstance(any(), any());

        vnfModifyRequestHandler.updateInstance(vnfInstance, vnfInfoModificationRequest, LifecycleOperationType.MODIFY_INFO, lifecycleOperation,  new HashMap<>());

        verify(extensionsService, times(1))
                .updateInstanceWithExtensionsInRequest(anyMap(), any());
        verify(replicaDetailsService, times(1))
                .setReplicaDetailsToVnfInstance(anyString(), any());
        verify(databaseInteractionService, times(1))
                .persistChangedInfo(changedInfo, lifecycleOperation.getOperationOccurrenceId());

        VnfInstance tempInstanceUpdated = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        assertThat(tempInstanceUpdated.getVnfInstanceName())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfInstanceName());
        assertThat(tempInstanceUpdated.getVnfInstanceDescription())
                .isEqualTo(vnfInfoModificationRequest.getVnfInstanceDescription());
        assertThat(tempInstanceUpdated.getVnfPackageId())
                .isEqualTo(vnfInfoModificationRequest.getVnfPkgId());
        assertThat(tempInstanceUpdated.getMetadata())
                .isNotEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));

        assertThat(vnfInstance.getVnfInstanceName())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfInstanceName());
        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions())
                .isNotEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getExtensions()));
        assertThat(vnfInstance.getVnfInstanceDescription())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfInstanceDescription());
        assertThat(vnfInstance.getVnfPackageId())
                .isNotEqualTo(vnfInfoModificationRequest.getVnfPkgId());
        assertThat(vnfInstance.getMetadata())
                .isNotEqualTo(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
    }

    @Test
    public void testUpdateInInstantiatedStateWithExtensionsAndMultipleAspectDeltasPerAspect() throws JsonProcessingException {
        // given
        final VnfInstance vnfInstance = createVnfInstance(true);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setInstantiationLevel(TestUtils.INST_LEVEL_2);
        vnfInstance.setPolicies(readDataFromFile(getClass(), "modify-vnfpkg/policies-multiple-aspect-deltas.json"));

        vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(
                Map.of(VNF_CONTROLLED_SCALING, Map.of("Aspect3", CISM_CONTROLLED))));

        final Map<String, ReplicaDetails> replicaDetails = Map.of(
                "test-cnf", ReplicaDetails.builder()
                        .withAutoScalingEnabledValue(true)
                        .withCurrentReplicaCount(1)
                        .withMinReplicasCount(1)
                        .withMaxReplicasCount(5)
                        .build(),
                "test-cnf-vnfc1", ReplicaDetails.builder()
                        .withAutoScalingEnabledValue(true)
                        .withCurrentReplicaCount(2)
                        .withMinReplicasCount(2)
                        .withMaxReplicasCount(5)
                        .build());
        final HelmChart chart = new HelmChart();
        chart.setHelmChartType(HelmChartType.CNF);
        chart.setHelmChartUrl("chart-url");
        chart.setReplicaDetails(Utility.convertObjToJsonString(replicaDetails));

        vnfInstance.setHelmCharts(List.of(chart));

        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(Map.of(VNF_CONTROLLED_SCALING, Map.of("Aspect3", MANUAL_CONTROLLED)));

        when(packageService.getPackageInfoWithDescriptorModel(anyString()))
                .thenReturn(createPackageResponse());

        when(vnfInstanceService.getVnfControlledScalingExtension(any())).thenReturn(Map.of("Aspect3", MANUAL_CONTROLLED));
        when(replicaDetailsMapper.getReplicaDetailsFromHelmChart(any()))
                .thenAnswer(invocation -> mapper.readValue(((HelmChart) invocation.getArgument(0)).getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE));
        doNothing().when(scaleService).removeHelmChartFromTempInstance(any(), any());

        // when
        vnfModifyRequestHandler.updateInstance(vnfInstance,
                                               vnfInfoModificationRequest,
                                               LifecycleOperationType.MODIFY_INFO,
                                               createLifecycleOperation(),
                                               new HashMap<>());

        // then
        final VnfInstance modifiedTempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        final HelmChart modifiedChart = modifiedTempInstance.getHelmCharts().get(0);
        final Map<String, ReplicaDetails> modifiedReplicaDetails = mapper.readValue(modifiedChart.getReplicaDetails(), REPLICA_DETAILS_MAP_TYPE);
        assertThat(modifiedReplicaDetails)
                .hasEntrySatisfying("test-cnf", details -> assertThat(details.getAutoScalingEnabledValue()).isFalse())
                .hasEntrySatisfying("test-cnf-vnfc1", details -> assertThat(details.getAutoScalingEnabledValue()).isFalse());
    }

    private static PackageResponse createPackageResponse() {
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setId("UPDATED-SCALING");
        packageResponse.setVnfdId("UPDATED-SCALING");
        packageResponse.setVnfdVersion("cxp9025898_4r81e08");
        packageResponse.setVnfSoftwareVersion("1.20 (CXS101289_R81E08)");
        packageResponse.setVnfProductName("SGSN-MME");
        packageResponse.setVnfProvider("Ericsson");
        packageResponse.setDescriptorModel(vnfd);
        return packageResponse;
    }

    private ChangedInfo createdChangedInfo(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                           final PackageResponse packageResponse) {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceDescription(vnfInfoModificationRequest.getVnfInstanceDescription());
        changedInfo.setVnfInstanceName(vnfInfoModificationRequest.getVnfInstanceName());
        changedInfo.setMetadata(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
        changedInfo.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(vnfInfoModificationRequest.getExtensions()));
        changedInfo.setVnfPkgId(vnfInfoModificationRequest.getVnfPkgId());
        changedInfo.setVnfDescriptorId(packageResponse.getId());
        changedInfo.setVnfdVersion(packageResponse.getVnfdVersion());
        changedInfo.setVnfProductName(packageResponse.getVnfProductName());
        changedInfo.setVnfProviderName(packageResponse.getVnfProvider());
        changedInfo.setVnfSoftwareVersion(packageResponse.getVnfSoftwareVersion());
        return changedInfo;
    }

    private ChangedInfo createdChangedInfoWithoutMetadataAndExtensions(final VnfInfoModificationRequest vnfInfoModificationRequest,
                                                                        final PackageResponse packageResponse) {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceDescription(vnfInfoModificationRequest.getVnfInstanceDescription());
        changedInfo.setVnfInstanceName(vnfInfoModificationRequest.getVnfInstanceName());
        changedInfo.setVnfPkgId(vnfInfoModificationRequest.getVnfPkgId());
        changedInfo.setVnfDescriptorId(packageResponse.getId());
        changedInfo.setVnfdVersion(packageResponse.getVnfdVersion());
        changedInfo.setVnfProductName(packageResponse.getVnfProductName());
        changedInfo.setVnfProviderName(packageResponse.getVnfProvider());
        changedInfo.setVnfSoftwareVersion(packageResponse.getVnfSoftwareVersion());
        return changedInfo;
    }

    private ChangedInfo createdChangedInfoWithoutPackage(final VnfInfoModificationRequest vnfInfoModificationRequest) {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceDescription(vnfInfoModificationRequest.getVnfInstanceDescription());
        changedInfo.setVnfInstanceName(vnfInfoModificationRequest.getVnfInstanceName());
        changedInfo.setMetadata(convertObjToJsonString(vnfInfoModificationRequest.getMetadata()));
        changedInfo.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(vnfInfoModificationRequest.getExtensions()));
        return changedInfo;
    }

    private static VnfInfoModificationRequest createVnfInfoModificationRequest() {
        final VnfInfoModificationRequest vnfInfoModificationRequest = new VnfInfoModificationRequest();
        vnfInfoModificationRequest.setExtensions(createExtensions());
        vnfInfoModificationRequest.setMetadata(
                Map.of(RandomStringUtils.randomAlphanumeric(10),
                       RandomStringUtils.randomAlphanumeric(10))
        );
        vnfInfoModificationRequest.setVnfPkgId(RandomStringUtils.randomAlphanumeric(15));
        vnfInfoModificationRequest.setVnfInstanceName(RandomStringUtils.randomAlphanumeric(10));
        vnfInfoModificationRequest.setVnfInstanceDescription(RandomStringUtils.randomAlphanumeric(10));
        return vnfInfoModificationRequest;
    }

    private String getPoliciesAsJsonString(final Policies policies) {
        String policiesAsJsonString;
        try {
            policiesAsJsonString = (mapper.writeValueAsString(policies));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to write policies object to json");
        }
        return policiesAsJsonString;
    }

    private static WorkflowRoutingResponse createResponse() {
        WorkflowRoutingResponse response = new WorkflowRoutingResponse();
        response.setErrorMessage("errorMessage");
        response.setHttpStatus(HttpStatus.OK);
        return response;
    }

    private static LifecycleOperation createLifecycleOperation() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationState(LifecycleOperationState.PROCESSING);
        return lifecycleOperation;
    }

    @NotNull
    private static String getFile(final String file) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(getResource(file)));
    }
}
