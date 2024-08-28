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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.DEFAULT_PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfInstances.VNF_INSTANCE_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.OwnerReference;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.ComponentStatusResponseList;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetInstancePositiveBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;

    @BeforeEach
    public void setUp() {
        mockRepositories();
        stubGettingComponentStatusResponse();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void mockRepositories() {
        List<VnfInstance> instances = getInstances();
        given(vnfInstanceRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(instances, getPage(), instances.size()));
        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(getVnfInstance("1")));
        given(clusterConfigFileRepository.findByName(anyString())).willReturn(Optional.of(getClusterConfigFile()));
    }

    protected Pageable getPage() {
        return new PaginationUtils.PageableBuilder()
                .defaults(DEFAULT_PAGE_SIZE, VNF_INSTANCE_NAME)
                .size(15)
                .build();
    }

    private void stubGettingComponentStatusResponse() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(ComponentStatusResponseList.class)))
                .thenReturn(buildComponentStatusResponseList());
    }

    private ClusterConfigFile getClusterConfigFile() {
        return ClusterConfigFile.builder()
                .name("my-cluster")
                .content("my-cluster")
                .build();
    }

    protected List<VnfInstance> getInstances() {
        List<VnfInstance> instances = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            VnfInstance vnf = getVnfInstance(String.valueOf(i));
            instances.add(vnf);
        }
        return instances;
    }

    protected VnfInstance getVnfInstance(String suffix) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("54321" + suffix);
        vnfInstance.setVnfInstanceName("my-instance-name-" + suffix);
        vnfInstance.setVnfInstanceDescription("testVnfDescription");
        vnfInstance.setVnfDescriptorId("1234567" + suffix);
        vnfInstance.setVnfProviderName("Ericsson");
        vnfInstance.setVnfProductName("SGSN-MME");
        vnfInstance.setVnfSoftwareVersion("1.20");
        vnfInstance.setVnfdVersion("1.20");
        vnfInstance.setVnfPackageId("1234567");
        vnfInstance.setClusterName("my-cluster");
        vnfInstance.setNamespace("evnfm-deployment");
        vnfInstance.setCrdNamespace("eric-crd-ns");
        vnfInstance.setHelmCharts(Collections.emptyList());
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);

        Map<String, String> aspects = new HashMap<>();
        Map<String, String> deployableModules = new HashMap<>();
        aspects.put("Aspect1", "CISMControlled");
        aspects.put("Aspect2", "ManualControlled");
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "disabled");
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("vnfControlledScaling", aspects);
        extensions.put("deployableModules", deployableModules);
        vnfInstance.setVnfInfoModifiableAttributesExtensions(Utility.convertObjToJsonString(extensions));
        vnfInstance.setMetadata(Utility.convertObjToJsonString(Map.of("tenantName", "ecm")));

        return vnfInstance;
    }

    private ResponseEntity<ComponentStatusResponseList> buildComponentStatusResponseList() {
        List<ComponentStatusResponse> responseList = new ArrayList<>();

        ComponentStatusResponse componentStatusResponse = new ComponentStatusResponse();

        List<VimLevelAdditionalResourceInfo> vimLevelAdditionalResourceInfos = new ArrayList<>();

        VimLevelAdditionalResourceInfo wfsResourceInfo = new VimLevelAdditionalResourceInfo();
        wfsResourceInfo.setUid("7230dc3a-dd27-4d9c-9527-88405803ee99");
        wfsResourceInfo.setName("eric-am-common-wfs-ui-57f8ff6886-25r7m");
        wfsResourceInfo.setStatus("Running");
        wfsResourceInfo.setNamespace("evnfm-deployment");

        Map<String, String> wfsLabels = new HashMap<>();
        wfsLabels.put("ericsson.com/product-name", "AM Common WFS UI");
        wfsLabels.put("ericsson.com/product-revision", "R1A");
        wfsResourceInfo.setLabels(wfsLabels);

        Map<String, String> wfsAnnotations = new HashMap<>();
        wfsAnnotations.put("app", "eric-am-common-wfs-ui");
        wfsAnnotations.put("app.kubernetes.io/instance", "optimus-prime");
        wfsResourceInfo.setAnnotations(wfsAnnotations);

        List<OwnerReference> wfsOwnerReferences = new ArrayList<>();
        OwnerReference wfsOwnerReference = new OwnerReference();
        wfsOwnerReference.setApiVersion("apps/v1");
        wfsOwnerReference.setKind("ReplicaSet");
        wfsOwnerReference.setName("eric-am-common-wfs-ui-57f8ff6886");
        wfsOwnerReference.setUid("e5fcee16-ad66-4aa7-8534-ba83cbb0b111");
        wfsOwnerReferences.add(wfsOwnerReference);

        wfsResourceInfo.setOwnerReferences(wfsOwnerReferences);

        vimLevelAdditionalResourceInfos.add(wfsResourceInfo);

        VimLevelAdditionalResourceInfo postgresResourceInfo = new VimLevelAdditionalResourceInfo();
        postgresResourceInfo.setUid("f04b05e0-1a8d-4097-8cad-c879c4d45c24");
        postgresResourceInfo.setName("application-manager-postgres-0");
        postgresResourceInfo.setStatus("Running");
        postgresResourceInfo.setNamespace("evnfm-deployment");

        Map<String, String> postgresLabels = new HashMap<>();
        postgresLabels.put("app", "application-manager-postgres");
        postgresLabels.put("app.kubernetes.io/instance", "evnfm-deployment");
        postgresLabels.put("app.kubernetes.io/name", "application-manager-postgres");
        postgresLabels.put("app.kubernetes.io/version", "4.0.0_35");
        postgresLabels.put("cluster-name", "application-manager-postgres");
        postgresLabels.put("controller-revision-hash", "application-manager-postgres-fc7c4b554");
        postgresLabels.put("role", "master");
        postgresLabels.put("statefulset.kubernetes.io/pod-name", "application-manager-postgres-0");
        postgresResourceInfo.setLabels(postgresLabels);

        Map<String, String> postgresAnnotations = new HashMap<>();
        postgresAnnotations.put("cni.projectcalico.org/podIP", "192.168.78.165/32");
        postgresAnnotations.put("cni.projectcalico.org/podIPs", "192.168.78.165/32");
        postgresResourceInfo.setAnnotations(postgresAnnotations);

        List<OwnerReference> postgresOwnerReferences = new ArrayList<>();
        OwnerReference postgresOwnerReference = new OwnerReference();
        postgresOwnerReference.setApiVersion("apps/v1");
        postgresOwnerReference.setKind("StatefulSet");
        postgresOwnerReference.setName("application-manager-postgres");
        postgresOwnerReference.setUid("62a065fd-c953-4c1d-89bd-b8c14814c58d");
        postgresOwnerReferences.add(postgresOwnerReference);

        postgresResourceInfo.setOwnerReferences(postgresOwnerReferences);

        vimLevelAdditionalResourceInfos.add(postgresResourceInfo);

        componentStatusResponse.setPods(vimLevelAdditionalResourceInfos);
        responseList.add(componentStatusResponse);

        return new ResponseEntity<>(new ComponentStatusResponseList(responseList), HttpStatus.OK);
    }
}
