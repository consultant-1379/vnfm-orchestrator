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

import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {
        "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}", "spring.flyway.enabled = false" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
public class RollbackInfoNegativeBase extends AbstractDbSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;
    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;
    @MockBean
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        when(vnfInstanceRepository.findById("NO_INSTANCE_ID_FOUND")).thenReturn(Optional.empty());
        VnfInstance vnfInstance = generateInstance("1.0.11");
        when(vnfInstanceRepository.findById("NO_TEMP_INSTANCE_FOUND")).thenReturn(Optional.of(vnfInstance));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private VnfInstance generateInstance(String vnfId) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(vnfId);
        return vnfInstance;
    }
}
