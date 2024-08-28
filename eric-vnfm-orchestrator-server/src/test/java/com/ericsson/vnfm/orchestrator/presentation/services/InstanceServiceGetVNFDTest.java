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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import static junit.framework.TestCase.assertTrue;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class InstanceServiceGetVNFDTest extends AbstractDbSetupTest {

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private VnfdHelper vnfdHelper;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void order1TestGetVnfdWithScaling() {
        whenOnboardingReturnsVnfd("instance-service/with-scaling-vnfd.yaml");

        String vnfd = packageService.getVnfd("with-scaling").toString();
        Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(vnfd));
        assertThat(policies).isPresent();
        assertThat(policies.get().getAllScalingAspects()).hasSize(1);
    }

    @Test
    public void order2TestPersistingInstanceWithScaling() {
        whenOnboardingReturnsDescriptor("instance-service/with-scaling-descriptor-model.json");
        whenOnboardingReturnsVnfd("instance-service/with-scaling-vnfd.yaml");
        whenOnboardingReturnsSupportedOperations("instance-service/supported-operations.json");

        String vnfd = packageService.getVnfd("with-scaling").toString();
        Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(vnfd));
        assertThat(policies).isPresent();
        assertThat(policies.get().getAllScalingAspects()).hasSize(1);
        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel("with-scaling");
        VnfInstance vnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest("with-scaling"), policies);
        assertThat(vnfInstance.getScaleInfoEntity()).isNotNull();
        List<ScaleInfoEntity> scaleInfoEntity = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntity).hasSize(2).extracting("aspectId").contains("Payload", "Payload_2");
        assertThat(scaleInfoEntity).extracting("scaleLevel").containsExactly(0, 0);
    }

    @Test
    public void order3TestGetVnfdWithoutScaling() {
        whenOnboardingReturnsVnfd("instance-service/no-scaling-vnfd.yaml");

        PackageResponse packageInfo = new PackageResponse();
        packageInfo.setDescriptorModel(packageService.getVnfd("no-scaling").toString());
        Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(packageInfo.getDescriptorModel()));
        assertThat(policies).isEmpty();
    }

    @Test
    public void order4TestPersistingInstanceWithDeployableModulesSupported() {
        whenOnboardingReturnsDescriptor("instance-service/with-scaling-descriptor-model.json");
        whenOnboardingReturnsVnfd("instance-service/with-deployable-modules-vnfd.yaml");
        whenOnboardingReturnsSupportedOperations("instance-service/supported-operations.json");

        PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel("multi-chart-477c-arel4-multi");
        VnfInstance vnfInstance = instanceService.createVnfInstanceEntity(packageInfo, getCreateVnfRequest("multi-chart-477c-arel4-multi"),
                                                                          Optional.of(new Policies()));

        assertTrue(vnfInstance.isDeployableModulesSupported());
    }

    private CreateVnfRequest getCreateVnfRequest(final String vnfdId) {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest();
        createVnfRequest.setVnfdId(vnfdId);
        createVnfRequest.setVnfInstanceName("test_application");
        createVnfRequest.setVnfInstanceDescription("dummy application to be created");
        return createVnfRequest;
    }

    private void whenOnboardingReturnsDescriptor(final String fileName) {
        doReturn(new ResponseEntity<>(readFile(fileName), HttpStatus.OK))
                .when(restTemplate).exchange(argThat(arg -> arg.toString().contains("vnf_packages")), eq(HttpMethod.GET), any(), eq(String.class));
    }

    private void whenOnboardingReturnsVnfd(final String fileName) {
        doReturn(new ResponseEntity<>(readFile(fileName), HttpStatus.OK))
                .when(restTemplate).exchange(argThat(arg -> arg.toString().endsWith("vnfd")), eq(HttpMethod.GET), any(), eq(String.class));
    }

    private void whenOnboardingReturnsSupportedOperations(final String fileName) {
        doReturn(new ResponseEntity<>(readFile(fileName), HttpStatus.OK))
                .when(restTemplate)
                .exchange(argThat(arg -> arg.toString().endsWith("supported_operations")), eq(HttpMethod.GET), any(), eq(String.class));
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }
}
