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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {"spring.flyway.enabled = false"})
public class DeleteNodePositiveBase extends AbstractDbSetupTest {
    @Inject
    private WebApplicationContext context;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private EnmTopologyService enmTopologyService;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        VnfInstance instance = new VnfInstance();
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setVnfInstanceId("this-id-exists");
        instance.setAddedToOss(true);
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(instance);
        PropertiesModel managedElement = new PropertiesModel("test");
        HashMap<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("managedElementId", managedElement);
        instance.setAddNodeOssTopology(mapper.writeValueAsString(additionalParams));
        Path path = new File("/tmp").toPath();
        given(enmTopologyService.generateDeleteNodeScript(anyMap())).willReturn(path);
        doNothing().when(ossNodeService).deleteNode(any(), anyMap(), any(), eq(true));
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
