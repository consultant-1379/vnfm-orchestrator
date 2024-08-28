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
import static org.mockito.BDDMockito.given;

import static com.ericsson.vnfm.orchestrator.utils.Utility.readJsonFromResource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.PagedResourcesResponse;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourcesService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}",
        "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server" })
public class GetResourcesPaginatedPositiveBase extends AbstractDbSetupTest {

    @Autowired
    protected WebApplicationContext context;

    @MockBean
    ResourcesService resourcesService;

    @BeforeEach
    public void setUp() {
        given(resourcesService.getVnfResource(any())).willAnswer(invocation -> {
            switch (invocation.getArgument(0, String.class)) {
                case "343531":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource2.json");
                case "343532":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource3.json");
                case "343533":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource4.json");
                case "343533-2":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource5.json");
                case "343536":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource6.json");
                case "343537":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource7.json");
                case "343531-2":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource1-failed.json");
                case "3435378":
                    return getResource("contracts/api/getResourcesPaginated/positive/resource8.json");
                default:
                    return getResource("contracts/api/getResourcesPaginated/positive/resource.json");
            }
        });
        given(resourcesService.getVnfResourcesPage(any(), anyBoolean(), any()))
                .willAnswer(i -> {
                    if (i.getArgument(1)) {
                        return getResourcesWithoutLifecycleOperations(i.getArgument(2, PageRequest.class));
                    } else {
                        return getResources(i.getArgument(2, PageRequest.class));
                    }
                });
        RestAssuredMockMvc.webAppContextSetup(this.context);
    }

    private Page<ResourceResponse> getResourcesWithoutLifecycleOperations(Pageable pageable) throws IOException {
        String jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/allResourcesWithoutLifecycles.json");
        ObjectMapper mapper = new ObjectMapper();
        List<ResourceResponse> resourceResponses = mapper.readValue(jsonString, PagedResourcesResponse.class).getItems();
        return new PageImpl<>(resourceResponses, pageable, 21);
    }

    private Page<ResourceResponse> getResources(Pageable pageable) throws IOException {
        String jsonString;
        switch (pageable.getPageNumber()) {
            case 0: // first page
                Optional<Sort.Order> orderInstanceName = Optional.ofNullable(pageable.getSort().getOrderFor("vnfInstanceName"));
                Optional<Sort.Order> orderLastState = Optional.ofNullable(pageable.getSort().getOrderFor("lastStateChanged"));
                if (orderInstanceName.isPresent() && orderInstanceName.get().isDescending()) {
                    jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/pageOneResourcesDesc.json");
                    break;
                } else if (orderLastState.isPresent() && orderLastState.get().isAscending()) {
                    jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/pageOneResourcesAsc.json");
                    break;
                } else {
                    jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/allResourcesDefault.json");
                    break;
                }
            case 1: // second page
                jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/allResourcesPage2.json");
                break;
            default:
                jsonString = readJsonFromResource("contracts/api/getResourcesPaginated/positive/allResourcesDefault.json");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<ResourceResponse> resourceResponses = mapper.readValue(jsonString, PagedResourcesResponse.class).getItems();
        return new PageImpl<>(resourceResponses, pageable, 18);
    }

    private ResourceResponse getResource(String fileName) throws IOException {
        String jsonString = readJsonFromResource(fileName);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, ResourceResponse.class);
    }
}
