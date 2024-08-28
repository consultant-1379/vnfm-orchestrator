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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.createDuplicateResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetValuesPositiveBase extends ContractTestRunner {

    @SpyBean
    private InstanceService instanceService;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @Inject
    private WebApplicationContext context;

    @TempDir
    public File temporaryFolder;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("dummy_id");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        when(vnfInstanceRepository.findById(anyString())).thenReturn(Optional.of(vnfInstance));
        doReturn(returnValues()).when(instanceService).getCombinedAdditionalValuesWithoutEVNFMParams(vnfInstance);
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private Path returnValues() throws URISyntaxException, IOException {
        return createDuplicateResource("contracts/api/getValues/positive/sample_values.yaml.properties", temporaryFolder);
    }
}
