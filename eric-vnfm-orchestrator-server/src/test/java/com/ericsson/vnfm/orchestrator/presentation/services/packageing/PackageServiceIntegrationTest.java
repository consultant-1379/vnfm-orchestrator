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
package com.ericsson.vnfm.orchestrator.presentation.services.packageing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;


@TestPropertySource(properties = {
        "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
public class PackageServiceIntegrationTest extends AbstractDbSetupTest {

    @Autowired
    private PackageService packageService;

    @Test
    public void testGetPackageArtifactsSuccess() {
        Optional<String> result = packageService.getPackageArtifacts("test", "Definitions/OtherTemplates/values_1.yaml");
        assertThat(result).isPresent();
    }

    @Test
    public void testGetPackageArtifactsWhenNotFound() {
        Optional<String> result = packageService.getPackageArtifacts("TEST_NOT_FOUND", "TEST_NOTFOUND");
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetVnfdSuccess() {
        JSONObject result = packageService.getVnfd("no-scaling");
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetVnfdWhenNotFound() {
        assertThatThrownBy(() -> packageService.getVnfd("TEST_NOTFOUND"))
                .hasMessage("VNFD of package with ID: TEST_NOTFOUND is not found");
    }

    @Test
    public void testGetScalingMappingSuccess() {
        Map<String, ScaleMapping> result = packageService.getScalingMapping("test", "Definitions/OtherTemplates/scaling_mapping.yaml");
        assertThat(result).isNotEmpty().hasSize(3);
    }

    @Test
    public void testGetScalingMappingWhenNotFound() {
        Map<String, ScaleMapping> result = packageService.getScalingMapping("TEST_NOT_FOUND", "TEST_NOTFOUND");
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetPackageInfoSuccess() {
        PackageResponse result = packageService.getPackageInfo("test");
        assertThat(result).isNotNull();
    }

    @Test
    public void getPackageInfoWithDescriptionModelSuccess() {
        PackageResponse result = packageService.getPackageInfoWithDescriptorModel("test");
        assertThat(result).isNotNull();
        assertThat(result.getDescriptorModel()).isNotNull();
    }

}
