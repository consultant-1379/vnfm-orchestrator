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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype.DOWNGRADE;
import static com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype.UPGRADE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.CHANGE_VNFPKG;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.SCALE;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.TERMINATE;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

class LifecycleOperationHelperTest {

    private final LifecycleOperationHelper lifecycleOperationHelper = new LifecycleOperationHelper();

    @Test
    void findLatestUpgradeOperationWhenOperationsIsEmpty() {
        // given
        final List<LifecycleOperation> operations = List.of();
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of();

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    @Test
    void findLatestUpgradeOperationWhenLastOperationFailed() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(SCALE, FAILED, "scale-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    @Test
    void findLatestUpgradeOperationWhenContainsTerminate() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(TERMINATE, PROCESSING, "terminate-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    @Test
    void findLatestUpgradeOperationWhenNoSourceVnfd() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-2", null, "target-vnfd-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-1", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id-2", UPGRADE),
                changePackageDetails("change-vnfpkg-id-1", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    @Test
    void findLatestUpgradeOperationWhenNoTargetVnfd() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-2", "source-vnfd-id", null),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-1", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id-2", UPGRADE),
                changePackageDetails("change-vnfpkg-id-1", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    @Test
    void findLatestUpgradeOperationWhenSelfUpgradePresent() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-2", "target-vnfd-id", "target-vnfd-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-1", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id-2", UPGRADE),
                changePackageDetails("change-vnfpkg-id-1", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation)
                .hasValueSatisfying(operation -> assertThat(operation.getOperationOccurrenceId()).isEqualTo("change-vnfpkg-id-1"));
    }

    @Test
    void findLatestUpgradeOperationWhenNotFinishedUpgradePresent() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(CHANGE_VNFPKG, PROCESSING, "change-vnfpkg-id-2", "source-vnfd-id", "target-vnfd-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-1", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id-2", UPGRADE),
                changePackageDetails("change-vnfpkg-id-1", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation)
                .hasValueSatisfying(operation -> assertThat(operation.getOperationOccurrenceId()).isEqualTo("change-vnfpkg-id-1"));
    }

    @Test
    void findLatestUpgradeOperationWhenDowngradePresent() {
        // given
        final List<LifecycleOperation> operations = List.of(
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-2", "source-vnfd-id", "target-vnfd-id"),
                operation(CHANGE_VNFPKG, COMPLETED, "change-vnfpkg-id-1", "source-vnfd-id", "target-vnfd-id"),
                operation(INSTANTIATE, COMPLETED, "instantiate-id"));
        final List<ChangePackageOperationDetails> changePackageOperationDetails = List.of(
                changePackageDetails("change-vnfpkg-id-2", DOWNGRADE),
                changePackageDetails("change-vnfpkg-id-1", UPGRADE));

        // when
        final Optional<LifecycleOperation> latestUpgradeOperation =
                lifecycleOperationHelper.findLatestUpgradeOperation(operations, "instance-id", changePackageOperationDetails);

        // then
        assertThat(latestUpgradeOperation).isEmpty();
    }

    private static LifecycleOperation operation(final LifecycleOperationType type,
                                                final LifecycleOperationState state,
                                                final String operationId,
                                                final String sourceVnfdId,
                                                final String targetVnfdId) {

        final LifecycleOperation operation = new LifecycleOperation();
        operation.setOperationOccurrenceId(operationId);
        operation.setLifecycleOperationType(type);
        operation.setOperationState(state);
        operation.setSourceVnfdId(sourceVnfdId);
        operation.setTargetVnfdId(targetVnfdId);

        return operation;
    }

    private static LifecycleOperation operation(final LifecycleOperationType type, final LifecycleOperationState state, final String operationId) {
        return operation(type, state, operationId, null, null);
    }

    private static ChangePackageOperationDetails changePackageDetails(final String operationId, final ChangePackageOperationSubtype subtype) {
        final ChangePackageOperationDetails details = new ChangePackageOperationDetails();
        details.setOperationOccurrenceId(operationId);
        details.setChangePackageOperationSubtype(subtype);

        return details;
    }
}
