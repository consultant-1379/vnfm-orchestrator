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

import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllClusterConfigNamesPositiveBase extends ContractTestRunner {
    @Autowired
    private WebApplicationContext context;


    // @MockBean just for better readability
    // Here could be @Autowired as well because the bean of the class already created as a mock at the ApplicationContext
    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;


    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        given(clusterConfigFileRepository.getAllClusterConfigNames()).willReturn(getAllClusterConfigNamesResponse());
    }

    private List<String> getAllClusterConfigNamesResponse() {
        return List.of("default.config", "cluster1.config", "cluster2.config", "cluster3.config", "cluster503ForDeregister.config");
    }
}
