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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.service;

import java.util.List;
import java.util.Optional;

import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

/**
 * Interface is responsible for searching LCM operations. It provides API for searching with different set of criteria
 */
public interface LcmOpSearchService {
    List<LifecycleOperation> searchAllNotFailedInstallOrUpgradeOperations(VnfInstance vnfInstance);

    List<LifecycleOperation> searchAllNotFailedInstallOrUpgradeOrScaleOperations(VnfInstance vnfInstance);

    Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOrScaleOperation(VnfInstance vnfInstance, int skip);

    Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOperation(VnfInstance vnfInstance, int skip);

    Optional<LifecycleOperation> searchLastCompletedInstallOrUpgradeOperationForRollbackTo(VnfInstance vnfInstance);

    List<LifecycleOperation> searchAllBefore(LifecycleOperation lifecycleOperation);

    Optional<LifecycleOperation> searchLastSuccessfulInstallOperation(VnfInstance vnfInstance);

    Optional<LifecycleOperation> searchLastOperation(VnfInstance vnfInstance);

    Optional<LifecycleOperation> searchLastChangingOperation(VnfInstance vnfInstance,
                                                             List<ChangePackageOperationDetails> changePackageOperationDetails);

    List<LifecycleOperation> searchAllCompleted(VnfInstance vnfInstance);
}
