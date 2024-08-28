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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.Utility.readJsonFromResource;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}",
        "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server" })
public class GetResourcesPositiveBase extends AbstractDbSetupTest {

    @Autowired
    protected WebApplicationContext context;

    @MockBean
    ResourcesService resourcesService;

    @BeforeEach
    public void setUp() throws IOException {
        VnfInstance vnfInstance1 = new VnfInstance();
        vnfInstance1.setVnfInstanceId("34353");
        when(resourcesService.getInstance("34353")).thenReturn(vnfInstance1);
        VnfInstance vnfInstance2 = new VnfInstance();
        vnfInstance2.setVnfInstanceId("343531");
        when(resourcesService.getInstance("343531")).thenReturn(vnfInstance2);
        VnfInstance vnfInstance3 = new VnfInstance();
        vnfInstance3.setVnfInstanceId("343532");
        when(resourcesService.getInstance("343532")).thenReturn(vnfInstance3);

        when(resourcesService.getVnfResource(any(), anyBoolean())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                String vnfInstanceId = (String) args[0];
                VnfResource resource;
                if ("34353".equals(vnfInstanceId)) {
                    resource = getResource();
                } else if ("343531".equals(vnfInstanceId)) {
                    resource = getResource2();
                } else if ("343532".equals(vnfInstanceId)) {
                    resource = getResource3();
                } else if ("343536".equals(vnfInstanceId)) {
                    resource = getResource6();
                } else if ("343537".equals(vnfInstanceId)) {
                    resource = getResource7();
                } else if ("3435378".equals(vnfInstanceId)) {
                    resource = getResource8();
                } else if ("343531-2".equals(vnfInstanceId)) {
                    resource = getResource1Failed();
                } else {
                    resource = getResource();
                }

                return resource;
            }
        });
        when(resourcesService.getVnfResources(anyBoolean())).thenReturn(getResources());
        when(resourcesService.getAllResourcesWithFilter(anyString())).thenReturn(getResources());

        RestAssuredMockMvc.webAppContextSetup(this.context);
    }

    private List<VnfResource> getResources() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/allResources.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<List<VnfResource>>() {
        });
    }

    private VnfResource getResource() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<VnfResource>() {
        });
    }

    private VnfResource getResource2() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource2.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<VnfResource>() {
        });
    }

    private VnfResource getResource3() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource3.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<VnfResource>() {
        });
    }

    private VnfResource getResource6() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource6.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<VnfResource>() {
        });
    }

    private VnfResource getResource7() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource7.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<>() {
        });
    }

    private VnfResource getResource8() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource8.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<>() {
        });
    }

    private VnfResource getResource1Failed() throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResources/positive/resource1-failed.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<>() {
        });
    }
}
