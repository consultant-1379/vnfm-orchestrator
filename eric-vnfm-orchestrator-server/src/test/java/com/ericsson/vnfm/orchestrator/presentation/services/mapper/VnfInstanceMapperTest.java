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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static com.ericsson.vnfm.orchestrator.model.McioInfo.McioTypeEnum.DEPLOYMENT;
import static com.ericsson.vnfm.orchestrator.model.McioInfo.McioTypeEnum.STATEFULSET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_DESCRIPTOR_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_PRODUCT_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_PROVIDER_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_SCALE_VNF_INFO;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_SCALE_VNF_INFO_ENTITY;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_VNFD_VERSION;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_VNF_SOFTWARE_VERSION;
import static com.ericsson.vnfm.orchestrator.TestUtils.EXTENSIONS;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyInstance;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyInstanceWithHelmChart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.vnfm.orchestrator.ApplicationServer;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.ComputeResource;
import com.ericsson.vnfm.orchestrator.model.InstantiatedVnfInfo;
import com.ericsson.vnfm.orchestrator.model.McioInfo;
import com.ericsson.vnfm.orchestrator.model.OwnerReference;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoBase;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoDeploymentStatefulSet;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoRel4;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfcResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = ApplicationServer.class)
@ActiveProfiles("test")
public class VnfInstanceMapperTest extends AbstractDbSetupTest {

    private static final TypeReference<List<ScaleInfoEntity>> SCALE_INFO_TYPE = new TypeReference<>() {
    };

    private final VnfInstanceMapper vnfInstanceMapper = Mappers.getMapper(VnfInstanceMapper.class);

    private WorkflowRoutingServicePassThrough workflowRoutingService;

    private ObjectMapper mapper;

    private final ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();

    @BeforeEach
    public void setup() {
        workflowRoutingService = mock(WorkflowRoutingServicePassThrough.class);
        mapper = new ObjectMapper();

        when(workflowRoutingService.getComponentStatusRequest(any(VnfInstance.class))).thenReturn(componentStatusResponse);

        componentStatusResponse.setPods(new ArrayList<>());
    }

    @Test
    public void testMapperFunctionality() throws JsonProcessingException {
        List<ScaleInfoEntity> scaleInfoEntityList = mapper.readValue(TestUtils.DUMMY_SCALE_VNF_INFO_ENTITY, SCALE_INFO_TYPE);
        VnfInstance vnfInstance = createDummyInstance(scaleInfoEntityList, InstantiationState.INSTANTIATED);

        final VnfInstanceResponse instanceResponse =
                vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);

        assertInstance(instanceResponse);
        assertThat(mapper.writeValueAsString(instanceResponse.getInstantiatedVnfInfo())).contains(DUMMY_SCALE_VNF_INFO);
        Map<String, Object> metadata = Utility.checkAndCastObjectToMap(instanceResponse.getMetadata());
        assertThat(metadata)
                .isNotNull()
                .containsEntry("tenantName", "ecm");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMapperFunctionalityWithExtensions() throws JsonProcessingException {
        List<ScaleInfoEntity> scaleInfoEntityList = mapper.readValue(TestUtils.DUMMY_SCALE_VNF_INFO_ENTITY, SCALE_INFO_TYPE);
        VnfInstance vnfInstance = createDummyInstance(scaleInfoEntityList, InstantiationState.INSTANTIATED);

        final VnfInstanceResponse instanceResponse =
                vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);

        assertInstance(instanceResponse);
        assertThat(mapper.writeValueAsString(instanceResponse.getExtensions())).isEqualTo(EXTENSIONS);
        Map<String, Map<String, String>> extensions = (Map<String, Map<String, String>>) instanceResponse.getExtensions();
        Map<String, String> vnfControlledScaling = extensions.get("vnfControlledScaling");
        assertThat(vnfControlledScaling).containsEntry("Aspect1", "ManualControlled");
    }

    @Test
    public void testMapperFunctionalityNotInstantiated() throws JsonProcessingException {
        List<ScaleInfoEntity> scaleInfoEntityList = mapper.readValue(DUMMY_SCALE_VNF_INFO_ENTITY, SCALE_INFO_TYPE);
        VnfInstance vnfInstance = createDummyInstance(scaleInfoEntityList, InstantiationState.NOT_INSTANTIATED);

        final VnfInstanceResponse instanceResponse =
                vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);

        assertInstance(instanceResponse);
        assertThat(instanceResponse.getInstantiatedVnfInfo().getScaleStatus()).isNotEmpty();
    }

    @Test
    public void testMapperFunctionalityScaleInfoNull() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        final VnfInstanceResponse instanceResponse =
                vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);

        assertInstance(instanceResponse);
        assertThat(instanceResponse.getInstantiatedVnfInfo().getScaleStatus()).isNull();
    }

    @Test
    public void testMapperToNotMapResourceInfo() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        vnfInstanceMapper.toOutputModel(vnfInstance);

        verify(workflowRoutingService, times(0)).getComponentStatusRequest(vnfInstance);
    }

    @Test
    public void testMapperToMapResourceInfo() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        vnfInstanceMapper.toOutputModel(vnfInstance);
        verify(workflowRoutingService, times(0)).getComponentStatusRequest(vnfInstance);
    }

    @Test
    public void testMapperToMapResourceInfoWithPropertyTrue() {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.INSTANTIATED);

        vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);

        verify(workflowRoutingService, times(0)).getComponentStatusRequest(vnfInstance);
    }

    @Test
    public void testVimLevelAdditionalResourceInfoRunningState() {
        List<VimLevelAdditionalResourceInfo> vimLevelResourceInfoList = new ArrayList<>();
        VimLevelAdditionalResourceInfo vimLevelAdditionalResourceInfo1 = new VimLevelAdditionalResourceInfo();
        vimLevelAdditionalResourceInfo1.setStatus("Running");
        VimLevelAdditionalResourceInfo vimLevelAdditionalResourceInfo2 = new VimLevelAdditionalResourceInfo();
        vimLevelAdditionalResourceInfo2.setStatus("Pending");

        vimLevelResourceInfoList.add(vimLevelAdditionalResourceInfo1);
        vimLevelResourceInfoList.add(vimLevelAdditionalResourceInfo2);
        boolean notRunning = Utility.isRunning(vimLevelResourceInfoList);
        assertThat(notRunning).isFalse();

        List<VimLevelAdditionalResourceInfo> vimLevelResourceInfoList2 = new ArrayList<>();
        vimLevelResourceInfoList2.add(vimLevelAdditionalResourceInfo1);
        vimLevelAdditionalResourceInfo2.setStatus("Running");
        vimLevelResourceInfoList2.add(vimLevelAdditionalResourceInfo2);
        notRunning = Utility.isRunning(vimLevelResourceInfoList2);
        assertThat(notRunning).isTrue();
    }

    @Test
    public void testParsingInvalidMetadataFieldThrowsException() throws JsonProcessingException {
        List<ScaleInfoEntity> scaleInfoEntityList = mapper.readValue(DUMMY_SCALE_VNF_INFO_ENTITY, SCALE_INFO_TYPE);
        VnfInstance vnfInstance = createDummyInstance(scaleInfoEntityList, InstantiationState.INSTANTIATED);
        vnfInstance.setMetadata("Invalid json");

        assertThatExceptionOfType(InternalRuntimeException.class)
                .isThrownBy(() -> vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance,
                                                                                  componentStatusResponse));
    }

    @Test
    public void testWithVnfcResourceInfo() {
        List<VnfcResourceInfo> expected = buildExpectedVnfcResourceInfoList();

        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.NOT_INSTANTIATED);
        List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setPods(pods);

        VnfInstanceResponse actualVnfInstance = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);
        List<VnfcResourceInfo> actual = actualVnfInstance.getInstantiatedVnfInfo().getVnfcResourceInfo();

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testWithMcioInfoAndVnfcResourceInfo() {
        String firstReleaseName = "test-release-name";
        String secondsReleaseName = "test-release-name-2";
        VnfInstance vnfInstance = createDummyInstanceWithHelmChart(firstReleaseName, secondsReleaseName);

        ComponentStatusResponse firstComponentStatusResponse = buildMockedComponentStatusResponse(vnfInstance.getVnfInstanceId(), firstReleaseName);
        ComponentStatusResponse secondComponentStatusResponse = buildMockedComponentStatusResponse(vnfInstance.getVnfInstanceId(), secondsReleaseName);
        List<ComponentStatusResponse> componentStatusResponses = List.of(firstComponentStatusResponse, secondComponentStatusResponse);

        List<VnfInstanceResponse> vnfInstanceResponse =
                vnfInstanceMapper.toOutputModelWithResourceInfo(Collections.singletonList(vnfInstance), componentStatusResponses);

        assertThat(vnfInstanceResponse).isNotNull();
        assertThat(vnfInstanceResponse).hasSize(1);

        assertThat(vnfInstanceResponse.get(0).getInstantiatedVnfInfo().getVnfcResourceInfo()).isNotNull();
        assertThat(vnfInstanceResponse.get(0).getInstantiatedVnfInfo().getVnfcResourceInfo()).hasSize(2);

        List<McioInfo> mcioInfoActual = vnfInstanceResponse.get(0).getInstantiatedVnfInfo().getMcioInfo();
        assertThat(mcioInfoActual).hasSize(4);
    }

    @Test
    public void testWithMcioInfoAndVnfcResourceInfoWithNoMatchesReleaseNames() {
        String instanceReleaseName = "test-release-name";
        String responseReleaseName = "response-release-name";
        VnfInstance vnfInstance = createDummyInstanceWithHelmChart(instanceReleaseName);
        ComponentStatusResponse componentStatusResponse = buildMockedComponentStatusResponse(vnfInstance.getVnfInstanceId(), responseReleaseName);

        List<ComponentStatusResponse> componentStatusResponses = List.of(componentStatusResponse);
        List<VnfInstance> vnfInstances = List.of(vnfInstance);

        List<VnfInstanceResponse> actual = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusResponses);
        List<VnfcResourceInfo> actualVnfcResourceInfo = actual.get(0).getInstantiatedVnfInfo().getVnfcResourceInfo();
        List<McioInfo> actualMcioInfo = actual.get(0).getInstantiatedVnfInfo().getMcioInfo();

        assertThat(actualVnfcResourceInfo).isEmpty();
        assertThat(actualMcioInfo).isNull();
    }

    @Test
    public void testWithMcioInfoAndVnfcResourceInfoWithEmptyReleaseNames() {
        String instanceReleaseName = "test-release-name";
        VnfInstance vnfInstance = createDummyInstanceWithHelmChart(instanceReleaseName);
        vnfInstance.setHelmCharts(null);

        List<ComponentStatusResponse> componentStatusResponses = new ArrayList<>();
        List<VnfInstance> vnfInstances = List.of(vnfInstance);

        List<VnfInstanceResponse> actual = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusResponses);
        List<VnfcResourceInfo> actualVnfcResourceInfo = actual.get(0).getInstantiatedVnfInfo().getVnfcResourceInfo();
        List<McioInfo> actualMcioInfo = actual.get(0).getInstantiatedVnfInfo().getMcioInfo();

        assertThat(actualVnfcResourceInfo).isEmpty();
        assertThat(actualMcioInfo).isNull();
    }

    @Test
    public void testMapVnfInstanceListWithEmptyClusterName() {
        String instanceReleaseName = "test-release-name";
        VnfInstance vnfInstance = createDummyInstanceWithHelmChart(instanceReleaseName);
        vnfInstance.setClusterName(null);

        List<ComponentStatusResponse> componentStatusResponses = new ArrayList<>();
        List<VnfInstance> vnfInstances = List.of(vnfInstance);

        List<VnfInstanceResponse> actual = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusResponses);
        List<VnfcResourceInfo> actualVnfcResourceInfo = actual.get(0).getInstantiatedVnfInfo().getVnfcResourceInfo();
        List<McioInfo> actualMcioInfo = actual.get(0).getInstantiatedVnfInfo().getMcioInfo();

        assertThat(actualVnfcResourceInfo).isEmpty();
        assertThat(actualMcioInfo).isNull();
    }

    @Test
    public void testWithMcioInfoAndVnfcResourceInfoWithPendingState() {
        String releaseName = "test-release-name";
        VnfInstance vnfInstance = createDummyInstanceWithHelmChart(releaseName);

        ComponentStatusResponse componentStatusResponse = buildMockedComponentStatusResponse(vnfInstance.getVnfInstanceId(), releaseName);
        componentStatusResponse.getPods().get(0).setStatus("Pending");

        List<ComponentStatusResponse> componentStatusResponses = List.of(componentStatusResponse);
        List<VnfInstance> vnfInstances = List.of(vnfInstance);

        List<VnfInstanceResponse> actual = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusResponses);
        InstantiatedVnfInfo.VnfStateEnum actualVnfState = actual.get(0).getInstantiatedVnfInfo().getVnfState();

        assertThat(actualVnfState).isEqualTo(InstantiatedVnfInfo.VnfStateEnum.STOPPED);
    }

    @Test
    public void testWithMcioInfoAndVnfcResourceInfoList() {
        List<McioInfo> expectedFirstMcioInfo = List.of(buildMcioInfo(DEPLOYMENT));

        String firstReleaseName = "test-release-name-1";
        String secondReleaseName = "test-release-name-2";

        VnfInstance firstVnfInstance = createDummyInstanceWithHelmChart(firstReleaseName, secondReleaseName);
        VnfInstance secondVnfInstance = createDummyInstanceWithHelmChart(secondReleaseName);
        secondVnfInstance.setVnfInstanceId("dummy_instance_id_2");
        List<VnfInstance> vnfInstances = List.of(firstVnfInstance, secondVnfInstance);

        ComponentStatusResponse firstComponentStatusResponse =
                buildMockedComponentStatusResponse(firstVnfInstance.getVnfInstanceId(), firstReleaseName, "Deployment");
        ComponentStatusResponse secondComponentStatusResponse =
                buildMockedComponentStatusResponse(firstVnfInstance.getVnfInstanceId(), secondReleaseName, "Deployment");
        ComponentStatusResponse thirdComponentStatusResponse =
                buildMockedComponentStatusResponse(secondVnfInstance.getVnfInstanceId(), secondReleaseName, "StatefulSet");
        List<ComponentStatusResponse> componentStatusResponses = List.of(firstComponentStatusResponse, secondComponentStatusResponse, thirdComponentStatusResponse);

        List<VnfInstanceResponse> vnfInstanceResponses = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusResponses);

        List<McioInfo> actualFirstMcioInfo = vnfInstanceResponses.get(0).getInstantiatedVnfInfo().getMcioInfo();
        List<McioInfo> actualSecondMcioInfo = vnfInstanceResponses.get(1).getInstantiatedVnfInfo().getMcioInfo();

        assertThat(expectedFirstMcioInfo).containsAll(actualFirstMcioInfo);
        assertThat(actualSecondMcioInfo).isNotNull();
        assertThat(actualSecondMcioInfo).hasSize(1);
        assertThat(actualSecondMcioInfo.get(0).getMcioType()).isEqualTo(STATEFULSET);
    }

    @Test
    public void testWithVnfcResourceInfoWithoutOwnerReferences() {
        List<VnfcResourceInfo> expected = buildExpectedVnfcResourceInfoList();
        expected.get(0).setVduId(null);
        resetOwnerReferences(expected.get(0));

        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.NOT_INSTANTIATED);
        List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        pods.get(0).setOwnerReferences(new ArrayList<>());
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setPods(pods);

        VnfInstanceResponse actualVnfInstance = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);
        List<VnfcResourceInfo> actual = actualVnfInstance.getInstantiatedVnfInfo().getVnfcResourceInfo();

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testWithVnfcResourceInfoWithNullOwnerReferences() {
        List<VnfcResourceInfo> expected = buildExpectedVnfcResourceInfoList();
        expected.get(0).setVduId(null);
        resetOwnerReferencesToNull(expected.get(0));

        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.NOT_INSTANTIATED);
        List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        pods.get(0).setOwnerReferences(null);
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setPods(pods);

        VnfInstanceResponse actualVnfInstance = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);
        List<VnfcResourceInfo> actual = actualVnfInstance.getInstantiatedVnfInfo().getVnfcResourceInfo();

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void testRel4VnfInstance() {
        List<VnfcResourceInfo> expected = buildExpectedVnfcResourceInfoListRel4();

        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.NOT_INSTANTIATED);
        vnfInstance.setRel4(true);

        List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setPods(pods);

        VnfInstanceResponse actualVnfInstance = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusResponse);
        List<VnfcResourceInfo> actual = actualVnfInstance.getInstantiatedVnfInfo().getVnfcResourceInfo();

        assertThat(expected).isEqualTo(actual);
    }

    private void resetOwnerReferences(VnfcResourceInfo vnfcResourceInfo) {
        Object vimLevelAdditionalResourceInfo = vnfcResourceInfo.getComputeResource().getVimLevelAdditionalResourceInfo();
        if (vimLevelAdditionalResourceInfo instanceof VimLevelAdditionalResourceInfo) {
            ((VimLevelAdditionalResourceInfo) vimLevelAdditionalResourceInfo).setOwnerReferences(new ArrayList<>());
            vnfcResourceInfo.getComputeResource().setVimLevelAdditionalResourceInfo(vimLevelAdditionalResourceInfo);
        }
    }

    private void resetOwnerReferencesToNull(VnfcResourceInfo vnfcResourceInfo) {
        Object vimLevelAdditionalResourceInfo = vnfcResourceInfo.getComputeResource().getVimLevelAdditionalResourceInfo();
        if (vimLevelAdditionalResourceInfo instanceof VimLevelAdditionalResourceInfo) {
            ((VimLevelAdditionalResourceInfo) vimLevelAdditionalResourceInfo).setOwnerReferences(null);
            vnfcResourceInfo.getComputeResource().setVimLevelAdditionalResourceInfo(vimLevelAdditionalResourceInfo);
        }
    }

    private List<VimLevelAdditionalResourceInfo> buildVimLevelAdditionalResourceInfoList() {
        VimLevelAdditionalResourceInfo vimLevelAdditionalResourceInfo = new VimLevelAdditionalResourceInfo()
                .uid("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .name("eric-am-onboarding-service-85748b467-tg2vf")
                .status("Running")
                .namespace("unit-testing-ns")
                .hostname("hostname")
                .annotations(Map.of("containerID", "044b6319c00236c87e9bfe43c7910a50ce594385b8aff8e564364020f5efbe0b"))
                .labels(Map.of("app.kubernetes.io/instance", "test-resources"))
                .ownerReferences(List.of(new OwnerReference().uid("c0734032ad4c")));
        return List.of(vimLevelAdditionalResourceInfo);
    }

    private List<VimLevelAdditionalResourceInfoRel4> buildVimLevelAdditionalResourceInfoListRel4() {
        VimLevelAdditionalResourceInfoBase vimLevelAdditionalResourceInfoBase = new VimLevelAdditionalResourceInfoBase()
                .uid("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .name("eric-am-onboarding-service-85748b467-tg2vf")
                .status("Running")
                .namespace("unit-testing-ns")
                .annotations(Map.of("containerID", "044b6319c00236c87e9bfe43c7910a50ce594385b8aff8e564364020f5efbe0b"))
                .labels(Map.of("app.kubernetes.io/instance", "test-resources"))
                .ownerReferences(List.of(new OwnerReference().uid("c0734032ad4c")));
        VimLevelAdditionalResourceInfoRel4 vimLevelAdditionalResourceInfoRel4 = new VimLevelAdditionalResourceInfoRel4()
                .hostname("hostname")
                .persistentVolume(StringUtils.EMPTY)
                .additionalInfo(vimLevelAdditionalResourceInfoBase);
        return List.of(vimLevelAdditionalResourceInfoRel4);
    }

    private List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> buildVimLevelAdditionalResourceInfoDeploymentStatefulSetList(String kind) {
        VimLevelAdditionalResourceInfoDeploymentStatefulSet vimLevelAdditionalResourceInfo = new VimLevelAdditionalResourceInfoDeploymentStatefulSet()
                .uid("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .name("eric-am-onboarding-service-85748b467-tg2vf")
                .kind(kind)
                .status("Running")
                .namespace("unit-testing-ns")
                .replicas(1)
                .annotations(Map.of("containerID", "044b6319c00236c87e9bfe43c7910a50ce594385b8aff8e564364020f5efbe0b"))
                .labels(Map.of("app.kubernetes.io/instance", "test-resources"))
                .availableReplicas(1)
                .ownerReferences(List.of(new OwnerReference().uid("c0734032ad4c")));
        return List.of(vimLevelAdditionalResourceInfo);
    }

    private List<VnfcResourceInfo> buildExpectedVnfcResourceInfoList() {
        VimLevelAdditionalResourceInfo vimLevelAdditionalResourceInfo = buildVimLevelAdditionalResourceInfoList().get(0);
        ComputeResource computeResource = new ComputeResource()
                .resourceId("eric-am-onboarding-service-85748b467-tg2vf")
                .vimLevelResourceType("Pod")
                .vimLevelAdditionalResourceInfo(vimLevelAdditionalResourceInfo);
        VnfcResourceInfo vnfcResourceInfo = new VnfcResourceInfo()
                .id("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .vduId("c0734032ad4c")
                .computeResource(computeResource);

        return List.of(vnfcResourceInfo);
    }

    private List<VnfcResourceInfo> buildExpectedVnfcResourceInfoListRel4() {
        VimLevelAdditionalResourceInfoRel4 vimLevelAdditionalResourceInfoRel4 = buildVimLevelAdditionalResourceInfoListRel4().get(0);
        ComputeResource computeResource = new ComputeResource()
                .resourceId("eric-am-onboarding-service-85748b467-tg2vf")
                .vimLevelResourceType("Pod")
                .vimLevelAdditionalResourceInfo(vimLevelAdditionalResourceInfoRel4);
        VnfcResourceInfo vnfcResourceInfo = new VnfcResourceInfo()
                .id("63e7fe0e-022d-4266-b67e-c0734032ad4c")
                .vduId("c0734032ad4c")
                .computeResource(computeResource);

        return List.of(vnfcResourceInfo);
    }

    private ComponentStatusResponse buildMockedComponentStatusResponse(String vnfInstanceId, String releaseName) {
        List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> deployments =
                buildVimLevelAdditionalResourceInfoDeploymentStatefulSetList(DEPLOYMENT.toString());
        List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> statefulSets =
                buildVimLevelAdditionalResourceInfoDeploymentStatefulSetList(STATEFULSET.toString());

        return new ComponentStatusResponse()
                .vnfInstanceId(vnfInstanceId)
                .clusterName("hart070")
                .releaseName(releaseName)
                .pods(pods)
                .deployments(deployments)
                .statefulSets(statefulSets);
    }

    private ComponentStatusResponse buildMockedComponentStatusResponse(String vnfInstanceId, String releaseName,
                                                                       String mcioType) {
        final List<VimLevelAdditionalResourceInfo> pods = buildVimLevelAdditionalResourceInfoList();
        List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> kubernetesResources =
                buildVimLevelAdditionalResourceInfoDeploymentStatefulSetList(mcioType.toString());
        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();
        componentStatusResponse.setVnfInstanceId(vnfInstanceId);
        componentStatusResponse.setClusterName("hart070");
        componentStatusResponse.setReleaseName(releaseName);
        componentStatusResponse.setPods(pods);
        if (mcioType.equals(DEPLOYMENT)) {
            componentStatusResponse.setDeployments(kubernetesResources);
        } else {
            componentStatusResponse.setStatefulSets(kubernetesResources);
        }

        return componentStatusResponse;
    }

    private McioInfo buildMcioInfo(McioInfo.McioTypeEnum mcioType) {
        String mcioId = mcioType + "/eric-am-onboarding-service-85748b467-tg2vf";
        return new McioInfo()
                .mcioId(mcioId)
                .mcioName("eric-am-onboarding-service-85748b467-tg2vf")
                .mcioType(mcioType)
                .mcioNamespace("unit-testing-ns")
                .desiredInstances(1)
                .availableInstances(1)
                .cismId("hart070")
                .vduId("c0734032ad4c");
    }

    private void assertInstance(final VnfInstanceResponse instanceResponse) {
        assertThat(instanceResponse).isNotNull();
        assertThat(instanceResponse.getId()).isEqualTo(DUMMY_INSTANCE_ID);
        assertThat(instanceResponse.getVnfInstanceName()).isEqualTo(DUMMY_INSTANCE_NAME);
        assertThat(instanceResponse.getVnfInstanceDescription()).isEqualTo(DUMMY_INSTANCE_DESCRIPTION);
        assertThat(instanceResponse.getVnfdId()).isEqualTo(DUMMY_DESCRIPTOR_ID);
        assertThat(instanceResponse.getVnfProductName()).isEqualTo(DUMMY_PRODUCT_NAME);
        assertThat(instanceResponse.getVnfProvider()).isEqualTo(DUMMY_PROVIDER_NAME);
        assertThat(instanceResponse.getVnfSoftwareVersion()).isEqualTo(DUMMY_VNF_SOFTWARE_VERSION);
        assertThat(instanceResponse.getVnfdVersion()).isEqualTo(DUMMY_VNFD_VERSION);
        assertThat(instanceResponse.getVnfPkgId()).isEqualTo(DUMMY_DESCRIPTOR_ID);
    }
}
