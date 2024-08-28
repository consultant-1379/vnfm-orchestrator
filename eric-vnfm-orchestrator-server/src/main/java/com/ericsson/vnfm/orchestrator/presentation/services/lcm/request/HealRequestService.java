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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HealRequestType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public interface HealRequestService {

    VnfInstance specificValidation(VnfInstance vnfInstance, HealVnfRequest healVnfRequest);

    TerminateVnfRequest prepareTerminateRequest(TerminateVnfRequest terminateVnfRequest, TerminateRequestHandler terminateRequestHandler,
                                                VnfInstance vnfInstance);

    HealRequestType getType();
}