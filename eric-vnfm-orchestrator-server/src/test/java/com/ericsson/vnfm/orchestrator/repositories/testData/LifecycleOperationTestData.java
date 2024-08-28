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
package com.ericsson.vnfm.orchestrator.repositories.testData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

public class LifecycleOperationTestData {

    public static List<LifecycleOperation> buildLifecycleOperations() {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperation("350");
        LifecycleOperation secondLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("351");
        LifecycleOperation thirdLifecycleOperation = buildLifecycleOperation("352");
        LifecycleOperation fourthLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("353");
        LifecycleOperation fifthLifecycleOperation = buildLifecycleOperation("354");
        LifecycleOperation sixthLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("355");

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(firstLifecycleOperation);
        lifecycleOperations.add(secondLifecycleOperation);
        lifecycleOperations.add(thirdLifecycleOperation);
        lifecycleOperations.add(fourthLifecycleOperation);
        lifecycleOperations.add(fifthLifecycleOperation);
        lifecycleOperations.add(sixthLifecycleOperation);

        return lifecycleOperations;
    }

    public static List<LifecycleOperation> buildLifecycleOperationsByIds() {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperationWithAllFields("352");
        LifecycleOperation secondLifecycleOperation = buildGracefulLifecycleOperationWithAllFields("353");
        LifecycleOperation thirdLifecycleOperation = buildGracefulLifecycleOperationWithAllFields("355");

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(firstLifecycleOperation);
        lifecycleOperations.add(secondLifecycleOperation);
        lifecycleOperations.add(thirdLifecycleOperation);

        return lifecycleOperations;
    }

    public static List<LifecycleOperation> buildLifecycleOperationsForPagination() {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperation("352");
        LifecycleOperation secondLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("353");

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(firstLifecycleOperation);
        lifecycleOperations.add(secondLifecycleOperation);

        return lifecycleOperations;
    }

    public static List<LifecycleOperation> buildLifecycleOperationsForSpecificationAndPagination() {
        LifecycleOperation lifecycleOperation = buildLifecycleOperation("352");

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(lifecycleOperation);

        return lifecycleOperations;
    }

    public static List<LifecycleOperation> buildLifecycleOperationsForSpecificationAndSort() {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperation("350");
        LifecycleOperation secondLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("351");
        LifecycleOperation thirdLifecycleOperation = buildLifecycleOperation("352");
        LifecycleOperation fourthLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("353");
        LifecycleOperation fifthLifecycleOperation = buildLifecycleOperation("354");
        LifecycleOperation sixthLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode("355");

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(sixthLifecycleOperation);
        lifecycleOperations.add(fifthLifecycleOperation);
        lifecycleOperations.add(fourthLifecycleOperation);
        lifecycleOperations.add(thirdLifecycleOperation);
        lifecycleOperations.add(secondLifecycleOperation);
        lifecycleOperations.add(firstLifecycleOperation);

        return lifecycleOperations;
    }

    public static LifecycleOperation buildExpectedLifecycleOperationByIdWithAllFields() {
        return buildLifecycleOperationWithAllFields("354");
    }

    public static List<LifecycleOperation> buildLifecycleOperations(String firstIdSuffix, String secondIdSuffix) {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperation(firstIdSuffix);
        LifecycleOperation secondLifecycleOperation = buildLifecycleOperationWithGracefulCancelMode(secondIdSuffix);

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(secondLifecycleOperation);
        lifecycleOperations.add(firstLifecycleOperation);

        return lifecycleOperations;
    }

    public static List<LifecycleOperation> buildExpectedLifecycleOperationsWithAllFields(String firstIdSuffix, String secondIdSuffix) {
        LifecycleOperation firstLifecycleOperation = buildLifecycleOperationWithAllFields(firstIdSuffix);
        LifecycleOperation secondLifecycleOperation = buildGracefulLifecycleOperationWithAllFields(secondIdSuffix);

        List<LifecycleOperation> lifecycleOperations = new ArrayList<>();
        lifecycleOperations.add(firstLifecycleOperation);
        lifecycleOperations.add(secondLifecycleOperation);

        return lifecycleOperations;
    }

    private static LifecycleOperation buildLifecycleOperationWithAllFields(String idSuffix) {
        LifecycleOperation lifecycleOperation = buildLifecycleOperation(idSuffix);
        lifecycleOperation.setOperationParams("{\"testOperationParams" + idSuffix + "\":\"test\"}");
        lifecycleOperation.setValuesFileParams("testValuesFileParams" + idSuffix);
        lifecycleOperation.setCombinedValuesFile("testCombinedValuesFile" + idSuffix);
        return lifecycleOperation;
    }

    private static LifecycleOperation buildGracefulLifecycleOperationWithAllFields(String idSuffix) {
        LifecycleOperation lifecycleOperation = buildLifecycleOperationWithGracefulCancelMode(idSuffix);
        lifecycleOperation.setOperationParams("{\"testOperationParams" + idSuffix + "\":\"test\"}");
        lifecycleOperation.setValuesFileParams("testValuesFileParams" + idSuffix);
        lifecycleOperation.setCombinedValuesFile("testCombinedValuesFile" + idSuffix);
        return lifecycleOperation;
    }

    private static LifecycleOperation buildLifecycleOperation(String idSuffix) {
        return LifecycleOperation.builder()
                .operationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a" + idSuffix)
                .operationState(LifecycleOperationState.STARTING)
                .stateEnteredTime(LocalDateTime.of(2024, 3, 31, 12, 30, 45))
                .startTime(LocalDateTime.of(2024, 3, 31, 12, 30, 45))
                .grantId("testGrantId" + idSuffix)
                .lifecycleOperationType(LifecycleOperationType.INSTANTIATE)
                .automaticInvocation(true)
                .cancelPending(false)
                .cancelMode(CancelModeType.FORCEFUL)
                .vnfSoftwareVersion("testVnfSoftwareVersion" + idSuffix)
                .vnfProductName("testVnfProductName" + idSuffix)
                .combinedAdditionalParams("testCombinedAdditionalParams" + idSuffix)
                .resourceDetails("{\"testResourceDetails" + idSuffix + "\":1}")
                .scaleInfoEntities("{\"testScaleInfoEntities" + idSuffix + "\":\"test\"}")
                .sourceVnfdId("testSourceVnfdId" + idSuffix)
                .targetVnfdId("testTargetVnfdId" + idSuffix)
                .deleteNodeFailed(true)
                .deleteNodeErrorMessage("testDeleteNodeErrorMessage" + idSuffix)
                .deleteNodeFinished(true)
                .applicationTimeout("1500")
                .expiredApplicationTime(LocalDateTime.of(2024, 3, 31, 12, 31, 45))
                .setAlarmSupervisionErrorMessage("testSetAlarmSupervisionErrorMessage" + idSuffix)
                .downsizeAllowed(true)
                .isAutoRollbackAllowed(true)
                .vnfInfoModifiableAttributesExtensions("testVnfInfoModifiableAttributesExtensions" + idSuffix)
                .instantiationLevel("testInstantiationLevel" + idSuffix)
                .rollbackPattern("testRollbackPattern" + idSuffix)
                .failurePattern("testRollbackFailurePattern" + idSuffix)
                .username("testUsername" + idSuffix)
                .helmClientVersion("testHelmClientVersion" + idSuffix)
                .build();
    }

    private static LifecycleOperation buildLifecycleOperationWithGracefulCancelMode(String idSuffix) {
        final LifecycleOperation lifecycleOperation = buildLifecycleOperation(idSuffix);
        lifecycleOperation.setCancelMode(CancelModeType.GRACEFUL);
        return lifecycleOperation;
    }
}
