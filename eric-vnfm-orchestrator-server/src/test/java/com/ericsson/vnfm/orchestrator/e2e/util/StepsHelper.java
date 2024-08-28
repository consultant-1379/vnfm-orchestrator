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
package com.ericsson.vnfm.orchestrator.e2e.util;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FOR_ROLLBACK;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

@TestComponent
public class StepsHelper {

    @Autowired
    private RequestHelper requestHelper;

    @Autowired
    private MessageHelper messageHelper;

    public void setupPreRollbackOperationHistory(final String releaseName, final VnfInstanceResponse vnfInstanceResponse) throws Exception {
        //Upgrade - future target state for downgrade/rollback
        MvcResult result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(releaseName, HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                                               HelmReleaseOperationType.CHANGE_VNFPKG, "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, LifecycleOperationState.COMPLETED);

        //Upgrade with version change before downgrade/rollback
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FOR_ROLLBACK,
                                                                                false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(releaseName, HelmReleaseState.COMPLETED, lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG, "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, LifecycleOperationState.COMPLETED);
    }
}
