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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ericsson.vnfm.orchestrator.model.URILink;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOccLinks;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfLcmOperationsController;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyServiceImpl;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestPropertySource(properties = { "spring.flyway.enabled = false" })
public class PostRollbackOrFailOperationPositiveBase {

    @Mock
    private VnfLcmOperationService vnfLcmOperationService;

    @Mock
    private UsernameCalculationService usernameCalculationService;

    @Mock
    private IdempotencyServiceImpl idempotencyService;

    @InjectMocks
    private VnfLcmOperationsController vnfLcmOperationsController;

    @BeforeEach
    public void setUp() {
        given(idempotencyService.executeTransactionalIdempotentCall(any(), any())).willCallRealMethod();
        given(vnfLcmOperationService.failLifecycleOperationByOccId(anyString())).willReturn(getVnfLcmOpOccResponse());
        doNothing().when(vnfLcmOperationService).rollbackLifecycleOperationByOccId(anyString());

        doReturn("E2E_USERNAME").when(usernameCalculationService).calculateUsername();

        RestAssuredMockMvc.standaloneSetup(vnfLcmOperationsController);
    }

    private VnfLcmOpOcc getVnfLcmOpOccResponse() {
        VnfLcmOpOcc vnfLcmOpOcc = new VnfLcmOpOcc();
        vnfLcmOpOcc.setId("b08fcbc8-474f-4673-91ee-761fd83991e6");
        vnfLcmOpOcc.setVnfInstanceId("b08fcbc8-474f-4673-91ee-761fd83991e6");
        vnfLcmOpOcc.setOperationState(VnfLcmOpOcc.OperationStateEnum.FAILED);
        vnfLcmOpOcc.setStartTime(new Date());
        vnfLcmOpOcc.setStateEnteredTime(new Date());
        vnfLcmOpOcc.setGrantId(null);
        vnfLcmOpOcc.setOperation(VnfLcmOpOcc.OperationEnum.INSTANTIATE);
        vnfLcmOpOcc.setOperationParams(null);
        vnfLcmOpOcc.setIsAutomaticInvocation(false);
        vnfLcmOpOcc.setIsCancelPending(false);
        vnfLcmOpOcc.setCancelMode(null);
        vnfLcmOpOcc.setError(null);

        URILink vnfOppOccSelfLink = new URILink();
        vnfOppOccSelfLink.setHref("http://localhost/vnflcm/v1/vnf_lcm_op_occs/12345");

        URILink vnfOppOccInstanceLink = new URILink();
        vnfOppOccInstanceLink.setHref("http://localhost/vnflcm/v1/vnf_instances/54321");

        URILink vnfOppOccRollbackLink = new URILink();
        vnfOppOccRollbackLink.setHref("http://localhost/vnflcm/v1/vnf_lcm_op_occs/rollback");

        URILink vnfOppOccFailLink = new URILink();
        vnfOppOccFailLink.setHref("http://localhost/vnflcm/v1/vnf_lcm_op_occs/54321/fail");

        VnfLcmOpOccLinks vnfOppOccResponseLinks = new VnfLcmOpOccLinks();
        vnfOppOccResponseLinks.setSelf(vnfOppOccSelfLink);
        vnfOppOccResponseLinks.setVnfInstance(vnfOppOccInstanceLink);
        vnfOppOccResponseLinks.setRollback(vnfOppOccRollbackLink);
        vnfOppOccResponseLinks.setFail(vnfOppOccFailLink);
        vnfLcmOpOcc.setLinks(vnfOppOccResponseLinks);

        return vnfLcmOpOcc;
    }
}
