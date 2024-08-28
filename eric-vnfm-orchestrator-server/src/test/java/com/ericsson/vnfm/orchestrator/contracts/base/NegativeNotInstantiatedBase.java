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

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.EvnfmOnboardingRoutingClient;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class NegativeNotInstantiatedBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private EvnfmOnboardingRoutingClient evnfmOnboardingRoutingClient;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        VnfInstance instance = TestUtils.getVnfInstance();
        instance.setVnfInstanceId("dummy_id");
        instance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);

        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(instance));
        given(vnfInstanceRepository.findById(anyString(), any())).willReturn(Optional.of(instance));
        given(packageService.getPackageInfoWithDescriptorModel(any())).willReturn(createPackageResponse());
        stubOnboardingResponse();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void stubOnboardingResponse() throws IOException, URISyntaxException {
        String vnfd = readDataFromFile("vnfd.yaml");

        given(evnfmOnboardingRoutingClient.execute(any(URI.class), anyString(),
                eq(HttpMethod.GET), eq(String.class), any()))
                .willReturn(new ResponseEntity<>(vnfd, HttpStatus.OK));
    }

    private PackageResponse createPackageResponse() throws IOException {
        final String vnfdString = TestUtils.readDataFromFile(Path.of("src/test/resources/descriptorModelWithoutParams.json"));
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }
}
