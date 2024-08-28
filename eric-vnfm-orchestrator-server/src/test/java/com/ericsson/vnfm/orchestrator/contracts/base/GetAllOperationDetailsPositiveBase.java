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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.ResourceOperationsServiceImpl;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = {"spring.flyway.enabled = false"})
public class GetAllOperationDetailsPositiveBase extends AbstractDbSetupTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2012, 9, 21, 19, 47);

    @MockBean
    private ResourceOperationsServiceImpl allOperationsService;

    @Inject
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() {
        given(allOperationsService.getAllOperationDetails()).willReturn(getAllOperationDetailsResponse());
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<OperationDetails> getAllOperationDetailsResponse() {
        List<OperationDetails> allOperations = new ArrayList<>();

        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setResourceInstanceName("testInstance1");
        operationDetails.setResourceID("1234");
        operationDetails.setOperation("INSTANTIATE");
        operationDetails.setEvent("COMPLETED");
        operationDetails.setVnfProductName("vEPG");
        operationDetails.setVnfSoftwareVersion("1.23.2");
        operationDetails.setTimestamp(Utility.convertToDate(TIMESTAMP));
        allOperations.add(operationDetails);

        OperationDetails operationDetails2 = new OperationDetails();
        operationDetails2.setResourceInstanceName("testInstance2");
        operationDetails2.setResourceID("11111");
        operationDetails2.setOperation("CHANGE_VNFPKG");
        operationDetails2.setEvent("FAILED");
        operationDetails2.setVnfProductName("vEPG");
        operationDetails2.setVnfSoftwareVersion("1.13.2");
        operationDetails2.setTimestamp(Utility.convertToDate(TIMESTAMP));
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setStatus(404);
        problemDetails.setDetail("some error happened");
        problemDetails.setType(URI.create(TYPE_BLANK));
        problemDetails.setTitle("someTitle");
        problemDetails.setInstance(URI.create(TYPE_BLANK));
        operationDetails2.setError(problemDetails);
        allOperations.add(operationDetails2);
        return allOperations;
    }
}
