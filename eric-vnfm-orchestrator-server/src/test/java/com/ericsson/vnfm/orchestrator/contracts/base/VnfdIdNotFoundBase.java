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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.ChangeVnfPackageRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {
        "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}" })
@AutoConfigureStubRunner(ids = { "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
public class VnfdIdNotFoundBase extends AbstractDbSetupTest {
    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private PackageService packageService;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ChangeVnfPackageRequestHandler changeVnfPackageRequestHandler;

    @SpyBean
    private ChangeVnfPackageService changeVnfPackageService;

    @BeforeEach
    public void setUp() {
        LifecycleOperation operation = new LifecycleOperation();
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.INSTANTIATED);
        value.setVnfInstanceId("vnfdId-not-found");
        value.setVnfDescriptorId("existing-vnfdId");
        given(vnfInstanceRepository.findById("vnfdId-not-found")).willReturn(Optional.of(value));
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(value);
        given(changeVnfPackageRequestHandler.persistOperation(any(), any(), any(), any(), any(), any())).willReturn(operation);
        doReturn(Optional.empty()).when(changeVnfPackageService).getSuitableTargetDowngradeOperationFromOperation(any(), any());
        doThrow(new PackageDetailsNotFoundException("Package not found with id a-non-existent-id"))
                .when(packageService).getPackageInfoWithDescriptorModel(any());
        RestAssuredMockMvc.webAppContextSetup(context);
    }
}
