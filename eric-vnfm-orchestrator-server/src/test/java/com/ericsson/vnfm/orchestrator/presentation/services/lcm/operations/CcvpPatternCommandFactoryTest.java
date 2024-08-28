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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;


@SpringBootTest(classes = {
        CcvpPatternCommandFactory.class,
        DeletePvc.class,
        Instantiate.class,
        Rollback.class,
        Upgrade.class,
        Terminate.class
})
@MockBean({
        WorkflowRoutingService.class,
        EvnfmDowngrade.class,
        EvnfmManualRollback.class,
        InstanceService.class,
        AdditionalParamsUtils.class,
        OperationsUtils.class,
        HelmChartHistoryService.class,
        ChangePackageOperationDetailsRepository.class,
        PackageService.class,
})
public class CcvpPatternCommandFactoryTest {

    @Autowired
    private CcvpPatternCommandFactory ccvpPatternCommandFactory;

    @Test
    public void testGetServiceForDeleteOperations() {
        final Command delete = ccvpPatternCommandFactory.getService("delete");
        final Command deletePvc = ccvpPatternCommandFactory.getService("delete_PVC");
        final Command deletePvc1 = ccvpPatternCommandFactory.getService("delete_pvc");
        final Command deletePvcWithBrackets = ccvpPatternCommandFactory.getService("delete_PVC[]");
        final Command deletePvcWithServices = ccvpPatternCommandFactory.getService("delete_PVC[nginx,etc]");
        assertEquals(CommandType.TERMINATE, delete.getType());
        assertEquals(CommandType.DELETE_PVC, deletePvc.getType());
        assertEquals(CommandType.DELETE_PVC, deletePvc1.getType());
        assertEquals(CommandType.DELETE_PVC, deletePvcWithBrackets.getType());
        assertEquals(CommandType.DELETE_PVC, deletePvcWithServices.getType());
    }
}
