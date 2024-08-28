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

import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {
        "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}", "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
public class RollbackInfoPositiveBase extends AbstractDbSetupTest {

    private static final String TEST_VNF_ID = "test";

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;
    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        VnfInstance vnfInstance = generateInstance("1.0.11");
        VnfInstance tempInstnce = generateInstance("1.0.12");
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstnce));
        when(vnfInstanceRepository.findById(TEST_VNF_ID)).thenReturn(Optional.of(vnfInstance));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private VnfInstance generateInstance(String version) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(TEST_VNF_ID);
        vnfInstance.setVnfDescriptorId("ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5");
        vnfInstance.setVnfdVersion(version);
        return vnfInstance;
    }
}
