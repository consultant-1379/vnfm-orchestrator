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

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class DeleteVnfIdentifierPositiveBase extends ContractTestRunner {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        stubOnboardingResponse();

        VnfInstance vnfInstance = getVnfInstance();
        given(vnfInstanceRepository.findById(anyString())).willReturn(Optional.of(vnfInstance));
    }

    private VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        vnfInstance.setVnfPackageId(UUID.randomUUID().toString());
        vnfInstance.setVnfInstanceId(UUID.randomUUID().toString());
        return vnfInstance;
    }

    private void stubOnboardingResponse() {
        given(nfvoRetryTemplate.execute(any(RetryCallback.class)))
                .willReturn(new ResponseEntity<>(HttpStatus.OK));
    }
}
