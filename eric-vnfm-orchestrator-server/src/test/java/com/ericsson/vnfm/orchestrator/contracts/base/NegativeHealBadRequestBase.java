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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class NegativeHealBadRequestBase extends AbstractDbSetupTest {

    @MockBean
    private LifeCycleManagementService lifeCycleManagementService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);
        setUpMocks();
    }

    private void setUpMocks() {
        given(lifeCycleManagementService
                      .executeRequest(any(), contains("BAD_REQUEST_CNA_AND_CNF_PARAMS_DEFINED"), any(HealVnfRequest.class), anyString(), anyMap()))
                .willThrow(new InvalidHealRequestException(
                        "restore.backupName and restore.backupFileReference can not be present in the same HEAL Request"));

        given(lifeCycleManagementService
                      .executeRequest(any(), contains("BAD_REQUEST_CNA_SCOPE"), any(HealVnfRequest.class), anyString(), anyMap()))
                .willThrow(new InvalidHealRequestException(
                        "Invalid CNA Restore request. restore.scope is not present or an invalid value."));

        given(lifeCycleManagementService
                      .executeRequest(any(), contains("BAD_REQUEST_CNA_NO_REQUIRED_KEY"), any(HealVnfRequest.class), anyString(), anyMap()))
                .willThrow(new InvalidHealRequestException(
                        "Invalid Day0 configuration. At least one key-value pair is needed."));
    }
}
