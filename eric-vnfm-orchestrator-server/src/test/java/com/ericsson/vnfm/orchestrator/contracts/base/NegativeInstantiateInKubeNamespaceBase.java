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

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractRedisSetupTest;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@TestPropertySource(properties = { "spring.flyway.locations = classpath:db/migration" })
public class NegativeInstantiateInKubeNamespaceBase extends AbstractRedisSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private PackageService packageService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws IOException{
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        value.setVnfInstanceId("kube-namespace");
        value.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(value);
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(value);
        given(vnfInstanceRepository.findById("kube-namespace")).willReturn(Optional.of(value));
        given(packageService.getVnfd(any())).willReturn(new JSONObject());
        given(packageService.getPackageInfoWithDescriptorModel(any())).willReturn(createPackageResponse());
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private PackageResponse createPackageResponse() throws IOException {
        final String vnfdString = TestUtils.readDataFromFile(Path.of("src/test/resources/descriptorModelWithoutParams.json"));
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(vnfdString);
        return packageResponse;
    }
}
