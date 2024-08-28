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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.KUBE_NAMESPACES;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.presentation.services.ValidateNamespaceServiceImpl;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}",
        "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server" })
public class GetValidateNamespaceNegativeBase extends AbstractDbSetupTest {

    @Autowired
    protected WebApplicationContext context;

    @MockBean
    private ValidateNamespaceServiceImpl validateNamespaceService;

    @BeforeEach
    public void setUp() {
        doAnswer(invocation -> {
            String namespace = invocation.getArgument(1);
            if (namespace.equals("default")) {
                throw new IllegalArgumentException(String.format(
                        "Cannot instantiate in any of the " +
                                "Kubernetes initialized namespaces : %s", KUBE_NAMESPACES));
            } else {
                throw new IllegalArgumentException("reservedNameSpace is reserved for CRDs. Cannot instantiate CNFs in CRD namespace");
            }
        }).when(validateNamespaceService).validateNamespace(anyString(), anyString());

        RestAssuredMockMvc.webAppContextSetup(context);
    }
}