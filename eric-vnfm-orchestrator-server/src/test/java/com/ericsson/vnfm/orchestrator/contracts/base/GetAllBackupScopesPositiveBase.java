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
import static org.mockito.BDDMockito.given;

import static com.ericsson.vnfm.orchestrator.TestUtils.BRO_ENDPOINT_URL;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllBackupScopesPositiveBase extends ContractTestRunner{

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("test-id");
        vnfInstance.setBroEndpointUrl(BRO_ENDPOINT_URL);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        String response = TestUtils.parseJsonFile("contracts/api/getAllBackupScopes/positive/AllBackupScopesBroResponse.json");
        ResponseEntity<String> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        given(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class),
                                    ArgumentMatchers.<Class<String>>any())).willReturn(responseEntity);
        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(vnfInstance));
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(vnfInstance);
    }

}