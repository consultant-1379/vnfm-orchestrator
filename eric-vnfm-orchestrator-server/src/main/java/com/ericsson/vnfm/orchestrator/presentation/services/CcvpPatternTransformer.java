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
package com.ericsson.vnfm.orchestrator.presentation.services;

import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;

import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public interface CcvpPatternTransformer {
    void saveRollbackPatternInOperationForDowngradeCcvp(ChangeOperationContext context);

    void saveUpgradePatternInOperationCcvp(ChangeOperationContext context);

    List<MutablePair<String, String>> saveRollbackFailurePatternInOperationForOperationRollback(VnfInstance actualInstance,
                                                                                                VnfInstance tempInstance,
                                                                                                LifecycleOperation operation,
                                                                                                HelmChart failedHelmChart);
}
