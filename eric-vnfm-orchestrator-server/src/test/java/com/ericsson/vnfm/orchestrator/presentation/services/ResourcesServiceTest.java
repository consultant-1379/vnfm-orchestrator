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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.ScalingAspectsHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.filters.VnfResourceViewQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToInstancesQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToLifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ResourceViewResponseMapperImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceResourceResponseMapperImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfLifecycleMapperImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfResourceMapperImpl;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ResourcesService.class,
        ObjectMapper.class,
        VnfLifecycleMapperImpl.class,
        ResourceViewResponseMapperImpl.class,
        VnfResourceMapperImpl.class,
        VnfInstanceResourceResponseMapperImpl.class,
        VnfResourcesToLifecycleOperationQuery.class,
        VnfResourcesToInstancesQuery.class,
        VnfResourceViewQuery.class,
        MappingFileServiceImpl.class,
        ScalingAspectsHelper.class})
public class ResourcesServiceTest {

    @Autowired
    private ResourcesService resourcesService;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    @MockBean
    private VnfResourceViewRepository vnfResourceViewRepository;

    @MockBean
    private LifecycleOperationHelper lifecycleOperationHelper;

    @MockBean
    private PackageService packageService;

    @MockBean
    private LcmOpSearchService lcmOpSearchService;

    @MockBean
    private ChangePackageOperationDetailsService changePackageOperationDetailsService;

    @Test
    public void testGetAllInstance() {
        // given
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setAllOperations(Collections.emptyList());
        final var allInstances = List.of(vnfInstance);
        when(databaseInteractionService.getAllVnfInstances()).thenReturn(allInstances);
        doNothing().when(databaseInteractionService).initializeInstantiateOssTopologyFieldInVnfInstances(any());
        doNothing().when(databaseInteractionService).initializeOperationParamsFieldInLifecycleOperations(any());

        // when
        List<VnfInstance> actualVnfInstances = resourcesService.getInstances();

        // then
        assertThat(actualVnfInstances).isSameAs(allInstances);
        verify(databaseInteractionService).getAllVnfInstances();
    }

    @Test
    public void testGetInstanceWithInvalidId() {
        // given
        when(databaseInteractionService.getVnfInstance(anyString())).thenThrow(new NotFoundException("Not found"));

        // when and then
        assertThatThrownBy(() -> resourcesService.getInstance("test"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void testGetInstanceById() {
        //given
        final var vnfInstance = new VnfInstance();
        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);

        // when
        final var actualVnfInstance = resourcesService.getInstance("instance-id");

        // then
        assertThat(actualVnfInstance).isSameAs(vnfInstance);
    }

    @Test
    public void testGetAllLifecycleOperationWithNullInstanceId() {
        assertThatThrownBy(() -> resourcesService.getAllLifecycleOperationsWithInstanceId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetAllLifecycleOperationWithEmptyInstanceId() {
        assertThatThrownBy(() -> resourcesService.getAllLifecycleOperationsWithInstanceId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetAllLifecycleOperationWithValidInstanceId() {
        // when
        final var operations = List.of(new LifecycleOperation());
        when(databaseInteractionService.getLifecycleOperationsByVnfInstance(any())).thenReturn(operations);

        // when
        List<LifecycleOperation> actualOperations = resourcesService.getAllLifecycleOperationsWithInstanceId("instance-id");

        // then
        assertThat(actualOperations).isSameAs(operations);

        final var instanceCaptor = ArgumentCaptor.forClass(VnfInstance.class);
        verify(databaseInteractionService).getLifecycleOperationsByVnfInstance(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getVnfInstanceId()).isEqualTo("instance-id");
    }

    @Test
    public void testCreateVnfResources() {
        // given
        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");

        final LifecycleOperation operation = createOperation("occurrence-id");
        final var operations = List.of(operation);

        // when
        VnfResource vnfResource = resourcesService.createVnfResource(instance, operations, null, Collections.emptyList());

        // then
        assertThat(vnfResource.getInstanceId()).isEqualTo("instance-id");
        assertThat(vnfResource.getLcmOperationDetails().get(0).getOperationOccurrenceId()).isEqualTo("occurrence-id");
    }

    @Test
    public void testCreateVnfResourcesWithExtensions() {
        // given
        final var instance = new VnfInstance();
        instance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{"
                                                                  + "\"Aspect5\":\"CISMControlled\","
                                                                  + "\"Aspect1\":\"CISMControlled\","
                                                                  + "\"Aspect2\":\"ManualControlled\","
                                                                  + "\"Aspect3\":\"ManualControlled\"}}");

        final LifecycleOperation operation = createOperation("occurrence-id");
        final var operations = List.of(operation);

        // when
        VnfResource vnfResource = resourcesService.createVnfResource(instance, operations, null, Collections.emptyList());

        // then
        assertThat(vnfResource.getExtensions()).isEqualTo(convertStringToJSONObj(instance.getVnfInfoModifiableAttributesExtensions()));
    }

    @Test
    public void testCreateVnfResourceWithDisabledScalingAspectsFromScalingMappingFile() throws Exception {
        String vnfd = TestUtils.readDataFromFile("rel4_dm_upgrade_vnfd.yaml");
        JSONObject vnfdJson = VnfdUtility.validateYamlAndConvertToJsonObject(vnfd);

        VnfInstance vnfInstance = createVnfInstance(vnfdJson);

        HelmChart helmChart1 = createHelmChart(vnfInstance, "helm_package1", true);
        HelmChart helmChart2 = createHelmChart(vnfInstance, "helm_package2", false);
        vnfInstance.setHelmCharts(List.of(helmChart1, helmChart2));

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(packageService.getVnfd(anyString())).thenReturn(vnfdJson);
        when(packageService.getScalingMapping(anyString(), anyString())).thenReturn(createScalingMapping());

        ResourceResponse vnfResource = resourcesService.getVnfResource(vnfInstance.getVnfInstanceId());
        Map<String, ScalingAspectDataType> scalingAspects = mapper
                .convertValue(vnfResource.getScalingInfo(), new TypeReference<>() {});

        verify(packageService, times(1)).getVnfd(anyString());
        verify(packageService, times(1)).getScalingMapping(anyString(), anyString());

        assertFalse(scalingAspects.get("Aspect1").isEnabled());
        assertTrue(scalingAspects.get("Aspect2").isEnabled());
        assertTrue(scalingAspects.get("Aspect4").isEnabled());
    }

    @Test
    public void testCreateVnfResourceWithDisabledScalingAspectsFromVnfd() throws Exception {
        String vnfd = TestUtils.readDataFromFile("valid_vnfd_rel4_dm_no_scaling_mapping.yaml");
        JSONObject vnfdJson = VnfdUtility.validateYamlAndConvertToJsonObject(vnfd);

        VnfInstance vnfInstance = createVnfInstance(vnfdJson);

        HelmChart helmChart1 = createHelmChart(vnfInstance, "helm_package1", false);
        HelmChart helmChart2 = createHelmChart(vnfInstance, "helm_package2", true);
        vnfInstance.setHelmCharts(List.of(helmChart1, helmChart2));

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(vnfInstance);
        when(packageService.getVnfd(anyString())).thenReturn(vnfdJson);

        ResourceResponse vnfResource = resourcesService.getVnfResource(vnfInstance.getVnfInstanceId());
        Map<String, ScalingAspectDataType> scalingAspects = mapper
                .convertValue(vnfResource.getScalingInfo(), new TypeReference<>() {});

        assertTrue(scalingAspects.get("Aspect1").isEnabled());
        assertFalse(scalingAspects.get("Aspect2").isEnabled());
        assertFalse(scalingAspects.get("Aspect4").isEnabled());
    }

    @Test
    public void testCreateVnfResourcesForHealSupportedNotAlreadySetInInstance() {
        // given
        final var instance = new VnfInstance();

        final LifecycleOperation operation = createOperation("occurrence-id");
        final var operations = List.of(operation);

        // when
        VnfResource vnfResource = resourcesService.createVnfResource(instance, operations, null, Collections.emptyList());

        // then
        assertThat(vnfResource.isHealSupported()).isNull();
    }

    @Test
    public void testCreateVnfResourcesForHealSupportedAlreadySetInInstance() {
        // given
        final var instance = new VnfInstance();
        instance.setIsHealSupported(true);

        final LifecycleOperation operation = createOperation("occurrence-id");
        final var operations = List.of(operation);

        // when
        VnfResource vnfResource = resourcesService.createVnfResource(instance, operations, null, Collections.emptyList());

        // then
        assertThat(vnfResource.isHealSupported()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllResourcesWithFilterOnVnfInstance() {
        // given
        final var instance = new VnfInstance();
        instance.setAllOperations(List.of(createOperation("occurrence-id")));

        when(vnfInstanceRepository.findAll(any(Specification.class))).thenReturn(List.of(instance));

        // when
        List<VnfResource> allResources = resourcesService.getAllResourcesWithFilter("(eq,instanceId," +
                                                                                            "instance-id)");

        // then
        assertThat(allResources).isNotEmpty();

        verify(vnfInstanceRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllResourcesWithFilterOnLifecycleOperation() {
        // given
        final LifecycleOperation operation = createOperation("occurrence-id");
        operation.setVnfInstance(new VnfInstance());

        when(lifecycleOperationRepository.findAll(any(Specification.class))).thenReturn(List.of(operation));

        // when
        List<VnfResource> allResources = resourcesService.getAllResourcesWithFilter("(eq," +
                                                                                            "lcmOperationDetails/operationState,FAILED)");

        // then
        assertThat(allResources).isNotEmpty();

        verify(lifecycleOperationRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllResourcesWithFilterOnLifecycleOperationAndVnfInstanceDataNotPresent() {
        // given
        final var instance = new VnfInstance();
        instance.setAllOperations(List.of(createOperation("occurrence-id")));

        when(vnfInstanceRepository.findAll(any(Specification.class))).thenReturn(List.of(instance));
        when(lifecycleOperationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        // when
        List<VnfResource> allResources = resourcesService.getAllResourcesWithFilter("(eq,instanceId," +
                                                                                            "instance-id);" +
                                                                                            "(eq,lcmOperationDetails/operationOccurrenceId," +
                                                                                            "non-existing-occurrence-id)");

        // then
        assertThat(allResources).isEmpty();

        verify(vnfInstanceRepository).findAll(any(Specification.class));
        verify(lifecycleOperationRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAllResourcesWithFilterOnLifecycleOperationAndVnfInstance() {
        // given
        final LifecycleOperation operation = createOperation("occurrence-id");

        final var instance = new VnfInstance();
        instance.setAllOperations(List.of(operation));

        operation.setVnfInstance(instance);

        when(vnfInstanceRepository.findAll(any(Specification.class))).thenReturn(List.of(instance));
        when(lifecycleOperationRepository.findAll(any(Specification.class))).thenReturn(List.of(operation));

        // when
        List<VnfResource> allResources = resourcesService.getAllResourcesWithFilter("(eq,instanceId," +
                                                                                            "instance-id);" +
                                                                                            "(eq,lcmOperationDetails/operationState," +
                                                                                            "STARTING)");

        // then
        assertThat(allResources).isNotEmpty();

        verify(vnfInstanceRepository).findAll(any(Specification.class));
        verify(lifecycleOperationRepository).findAll(any(Specification.class));
    }

    @Test
    public void testGetDefaultLifecycleOperationFilter() {
        // given
        final var operation = createOperation("occurrence-id");

        final var instance = new VnfInstance();
        instance.setOperationOccurrenceId(operation.getOperationOccurrenceId());
        instance.setAllOperations(List.of(operation));

        final var allInstances = List.of(instance);
        when(databaseInteractionService.getAllVnfInstances()).thenReturn(allInstances);

        // when
        List<VnfResource> allResources = resourcesService.getAllResourcesWithFilter("(eq,lcmOperationDetails/currentLifecycleOperation,"
                                                                                            + "true)");

        // then
        assertThat(allResources).isNotEmpty();
    }

    @Test
    public void testGetVnfResourcesAllResourcesFalseAndNoOperations() {
        // given
        final var operation1 = createOperation("occurrence-id-1");

        final var instance1 = new VnfInstance();
        instance1.setVnfInstanceId("instance-id-1");
        instance1.setHealSupported(false);
        instance1.setAllOperations(List.of(operation1));

        final var instance2 = new VnfInstance();
        instance2.setVnfInstanceId("instance-id-2");
        instance2.setHealSupported(false);
        instance2.setAllOperations(Collections.emptyList());

        final var allInstances = List.of(instance1, instance2);
        when(databaseInteractionService.getAllVnfInstances()).thenReturn(allInstances);

        // when
        List<VnfResource> actualResources = resourcesService.getVnfResources(false);

        // then
        assertThat(actualResources).extracting(VnfResource::getInstanceId).containsOnly("instance-id-1");
    }

    @Test
    public void testGetVnfResourcesSetDowngradeSupported() {
        // given
        final var operation1 = createOperation("occurrence-id-1");

        final var instance1 = new VnfInstance();
        instance1.setVnfInstanceId("instance-id-1");
        instance1.setAllOperations(List.of(operation1));
        instance1.setHealSupported(false);

        final var operation2 = createOperation("occurrence-id-2");

        final var instance2 = new VnfInstance();
        instance2.setVnfInstanceId("instance-id-2");
        instance2.setAllOperations(List.of(operation2));
        instance2.setHealSupported(false);

        final var allInstances = List.of(instance1, instance2);
        when(databaseInteractionService.getAllVnfInstances()).thenReturn(allInstances);

        when(lcmOpSearchService.searchLastChangingOperation(same(instance1), any())).thenReturn(Optional.of(operation1));

        // when
        List<VnfResource> actualResources = resourcesService.getVnfResources(true);

        // then
        assertContainsVnfResourceAndDowngradeInStatus(actualResources,
                                                      "instance-id-1", true);
        assertContainsVnfResourceAndDowngradeInStatus(actualResources,
                                                      "instance-id-2", false);
    }

    @Test
    public void testGetVnfResourceSetDowngradeSupported() {
        // given
        final var operation = createOperation("occurrence-id");

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setAllOperations(List.of(operation));
        instance.setHealSupported(false);

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        when(lcmOpSearchService.searchLastChangingOperation(same(instance), any())).thenReturn(Optional.of(operation));

        // when
        VnfResource actualVnfResource = resourcesService.getVnfResource("instance-id", true);

        // then
        assertThat(actualVnfResource).isNotNull();
        assertThat(actualVnfResource.isDowngradeSupported()).isTrue();
    }

    @Test
    public void testGetVnfResourceSetDowngradeNotSupported() {
        // given
        final var operation = createOperation("occurrence-id");

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setAllOperations(List.of(operation));
        instance.setHealSupported(false);

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        when(lcmOpSearchService.searchLastChangingOperation(same(instance), any())).thenReturn(Optional.empty());

        // when
        VnfResource actualVnfResource = resourcesService.getVnfResource("instance-id", true);

        // then
        assertThat(actualVnfResource).isNotNull();
        assertThat(actualVnfResource.isDowngradeSupported()).isFalse();
    }

    @Test
    public void testGetVnfResourceOperationsAreSorted() {
        // given
        final var operation1 = createOperation("occurrence-id-1");
        operation1.setStateEnteredTime(LocalDateTime.now().minusMinutes(20));
        final var operation2 = createOperation("occurrence-id-2");
        operation2.setStateEnteredTime(LocalDateTime.now().minusMinutes(15));
        final var operation3 = createOperation("occurrence-id-3");
        operation3.setStateEnteredTime(LocalDateTime.now().minusMinutes(30));

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setHealSupported(false);
        instance.setAllOperations(List.of(operation1, operation2, operation3));

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        // when
        VnfResource actualVnfResource = resourcesService.getVnfResource("instance-id", false);

        // then
        assertThat(actualVnfResource.getLcmOperationDetails()).extracting(VnfResourceLifecycleOperation::getOperationOccurrenceId)
                .containsExactly("occurrence-id-2", "occurrence-id-1", "occurrence-id-3");
    }

    @Test
    public void testGetVnfResourceAllResourcesFalseAndNoOperations() {
        // given
        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setHealSupported(false);
        instance.setAllOperations(Collections.emptyList());

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        // when
        VnfResource actualVnfResource = resourcesService.getVnfResource("instance-id", false);

        // then
        assertThat(actualVnfResource).isNull();
    }

    @Test
    public void testGetVnfResourceAllResourcesTrueAndNoOperations() {
        // given
        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setHealSupported(false);
        instance.setAllOperations(Collections.emptyList());

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        // when
        VnfResource actualVnfResource = resourcesService.getVnfResource("instance-id", true);

        // then
        assertThat(actualVnfResource).isNotNull();
        assertThat(actualVnfResource.getInstanceId()).isEqualTo("instance-id");
    }

    @Test
    public void testGetVnfResourceNotFound() {
        // given
        when(databaseInteractionService.getVnfInstance(anyString())).thenThrow(new NotFoundException("Not found"));

        // when and then
        assertThatThrownBy(() -> resourcesService.getVnfResource("instance-id", true))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("instance-id vnf resource not present");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetResourcesPageWithFilterOnVnfInstance() {
        // given
        final LifecycleOperation operation1 = createOperation("occurrence-id-1");
        operation1.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operation1.setOperationState(LifecycleOperationState.COMPLETED);
        operation1.setStateEnteredTime(LocalDateTime.now().minusMinutes(25));
        operation1.setStartTime(LocalDateTime.now().minusMinutes(30));

        final LifecycleOperation operation2 = createOperation("occurrence-id-2");
        operation2.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        operation2.setOperationState(LifecycleOperationState.COMPLETED);
        operation2.setStateEnteredTime(LocalDateTime.now().minusMinutes(5));
        operation2.setStartTime(LocalDateTime.now().minusMinutes(10));

        final var view = new VnfResourceView();
        view.setVnfInstanceId("instance-id");
        view.setLastLifecycleOperation(operation2);
        view.setLastStateChanged(operation2.getStateEnteredTime());
        view.setAllOperations(new ArrayList<>(List.of(operation1, operation2)));
        view.setHealSupported(false);

        when(vnfResourceViewRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(view)));


        when(databaseInteractionService.getSupportedOperationsByVnfInstanceId(anyString()))
                .thenReturn(List.of(OperationDetail.ofSupportedOperation(LCMOperationsEnum.INSTANTIATE.getOperation())));

        // when
        Page<ResourceResponse> allResources = resourcesService.getVnfResourcesPage("(eq,instanceId,instance-id)", false, Pageable.unpaged());

        // then
        assertThat(allResources).isNotNull();
        assertThat(allResources.getContent()).isNotEmpty();

        allResources.forEach(ResourcesServiceTest::checkResource);
        allResources.forEach(resourceResponse -> assertThat(resourceResponse.getSupportedOperations()).isNotEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetResourcesPageWithNoResultsFilter() {
        // given
        when(vnfResourceViewRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // when
        Page<ResourceResponse> allResources = resourcesService.getVnfResourcesPage("(eq,instanceId,instance-id)", false, Pageable.unpaged());

        // then
        assertThat(allResources).isNotNull();
        assertThat(allResources.getContent()).isEmpty();
    }

    @Test
    public void testGetResourcesPageWithInvalidFilter() {
        assertThatThrownBy(() -> resourcesService.getVnfResourcesPage("(cont,vnfResourceName,test)", false, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filter not supported for (cont,vnfResourceName,test)");
    }

    @Test
    public void testNewGetResourceDowngradeNotSupported() {
        // given
        final LifecycleOperation operation = createOperation("occurrence-id");
        operation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operation.setOperationState(LifecycleOperationState.COMPLETED);
        operation.setStateEnteredTime(LocalDateTime.now().minusMinutes(5));
        operation.setStartTime(LocalDateTime.now().minusMinutes(10));

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setAllOperations(new ArrayList<>(List.of(operation)));
        instance.setIsHealSupported(false);

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        when(operationsInProgressRepository.findByVnfId(anyString())).thenReturn(Optional.empty());

        when(lcmOpSearchService.searchLastChangingOperation(any(), any())).thenReturn(Optional.empty());

        // when
        ResourceResponse resource = resourcesService.getVnfResource("instance-id");

        // then
        assertThat(resource).isNotNull();
        checkResource(resource);
        assertThat(resource.getDowngradeSupported()).isFalse();
    }

    @Test
    public void testNewGetResourceDowngradeSupported() {
        // given
        final LifecycleOperation operation1 = createOperation("occurrence-id-1");
        operation1.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operation1.setOperationState(LifecycleOperationState.COMPLETED);
        operation1.setStateEnteredTime(LocalDateTime.now().minusMinutes(25));
        operation1.setStartTime(LocalDateTime.now().minusMinutes(30));

        final LifecycleOperation operation2 = createOperation("occurrence-id-2");
        operation2.setLifecycleOperationType(LifecycleOperationType.CHANGE_VNFPKG);
        operation2.setOperationState(LifecycleOperationState.COMPLETED);
        operation2.setStateEnteredTime(LocalDateTime.now().minusMinutes(5));
        operation2.setStartTime(LocalDateTime.now().minusMinutes(10));

        final var instance = new VnfInstance();
        instance.setVnfInstanceId("instance-id");
        instance.setAllOperations(new ArrayList<>(List.of(operation1, operation2)));
        instance.setIsHealSupported(false);

        when(databaseInteractionService.getVnfInstance(anyString())).thenReturn(instance);

        when(operationsInProgressRepository.findByVnfId(anyString())).thenReturn(Optional.empty());

        when(lcmOpSearchService.searchLastChangingOperation(any(), any())).thenReturn(Optional.of(operation2));

        // when
        ResourceResponse resource = resourcesService.getVnfResource("instance-id");

        // then
        assertThat(resource).isNotNull();
        checkResource(resource);
        assertThat(resource.getDowngradeSupported()).isTrue();
    }

    private static LifecycleOperation createOperation(final String occurrenceId) {
        final var operation = new LifecycleOperation();
        operation.setOperationOccurrenceId(occurrenceId);
        return operation;
    }

    private void assertContainsVnfResourceAndDowngradeInStatus(List<VnfResource> resources,
                                                               String instanceId, boolean expectedDowngradeSupportedStatus) {
        Optional<VnfResource> actualVnfResource = resources.stream()
                .filter(resource -> instanceId.equals(resource.getInstanceId()))
                .findFirst();

        assertThat(actualVnfResource).isPresent();
        assertThat(actualVnfResource.get().isDowngradeSupported()).isEqualTo(expectedDowngradeSupportedStatus);
    }

    private static void checkResource(ResourceResponse resource) {
        assertThat(resource.getLastLifecycleOperation()).isNotNull();
        assertThat(resource.getLastStateChanged()).isNotEmpty();
        assertThat(resource.getLastLifecycleOperation().getCurrentLifecycleOperation()).isNotNull();
        final VnfResourceLifecycleOperation lastOperation = resource.getLastLifecycleOperation();
        assertThat(lastOperation.getCurrentLifecycleOperation()).isTrue();
        assertTrue(resource.getLcmOperationDetails().stream()
                           .filter(op -> !op.getOperationOccurrenceId().equals(lastOperation.getOperationOccurrenceId()))
                           .allMatch(op -> FALSE == op.getCurrentLifecycleOperation()));
        assertTrue(resource.getLcmOperationDetails().stream()
                           .filter(op -> op.getOperationOccurrenceId().equals(lastOperation.getOperationOccurrenceId()))
                           .allMatch(op -> TRUE == op.getCurrentLifecycleOperation()));
    }

    private VnfInstance createVnfInstance(JSONObject vnfd) throws JsonProcessingException {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(UUID.randomUUID().toString());
        vnfInstance.setVnfPackageId(UUID.randomUUID().toString());
        vnfInstance.setDeployableModulesSupported(true);
        vnfInstance.setPolicies(mapper.writeValueAsString(PolicyUtility.createPolicies(vnfd)));

        return vnfInstance;
    }

    private HelmChart createHelmChart(VnfInstance vnfInstance, String artifactKey, boolean enabled) {
        HelmChart helmChart = new HelmChart();
        helmChart.setId(UUID.randomUUID().toString());
        helmChart.setHelmChartName("test-helm-chart-" + artifactKey);
        helmChart.setId(UUID.randomUUID().toString());
        helmChart.setChartEnabled(enabled);
        helmChart.setHelmChartArtifactKey(artifactKey);
        return helmChart;
    }

    private Map<String, ScaleMapping> createScalingMapping() {
        Map<String, ScaleMapping> result = new HashMap<>();

        ScaleMapping scaleMapping1 = new ScaleMapping();
        scaleMapping1.setMciopName("helm_package1");

        ScaleMapping scaleMapping2 = new ScaleMapping();
        scaleMapping2.setMciopName("helm_package2");

        result.put("test-cnf", scaleMapping1);
        result.put("test-cnf-vnfc1", scaleMapping1);
        result.put("test-cnf-vnfc2", scaleMapping1);
        result.put("test-cnf-vnfc3", scaleMapping1);
        result.put("test-cnf-vnfc4", scaleMapping1);
        result.put("eric-pm-bulk-reporter", scaleMapping2);

        return result;
    }
}
