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

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.EvnfmOnboardingRoutingClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class PostVnfIdentifierPositiveBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @MockBean
    private EvnfmOnboardingRoutingClient evnfmOnboardingRoutingClient;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        stubOnboardingResponses();
        mockRepository();
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void mockRepository() {
        when(vnfInstanceRepository.save(any(VnfInstance.class)))
                .thenAnswer(invocation -> {
                    VnfInstance vnfInstance = invocation.getArgument(0, VnfInstance.class);
                    vnfInstance.setVnfInstanceId(UUID.randomUUID().toString());
                    return vnfInstance;
                });
    }

    private void stubOnboardingResponses() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile("descriptorModel.json");
        String vnfd = readDataFromFile("vnfd.yaml");

        PackageResponse packageResponse = mapper.readValue(
                readDataFromFile("contracts/api/postVnfIdentifier/packageResponse.json"), PackageResponse.class);
        packageResponse.setDescriptorModel(descriptorModel);

        given(evnfmOnboardingRoutingClient.execute(any(URI.class), anyString(),
                eq(HttpMethod.GET), eq(PackageResponse[].class), any()))
                .willReturn(new ResponseEntity<>(new PackageResponse[]{packageResponse}, HttpStatus.OK));
        given(evnfmOnboardingRoutingClient.execute(any(URI.class), anyString(),
                eq(HttpMethod.GET), eq(String.class), any()))
                .willReturn(new ResponseEntity<>(vnfd, HttpStatus.OK));
    }
}
