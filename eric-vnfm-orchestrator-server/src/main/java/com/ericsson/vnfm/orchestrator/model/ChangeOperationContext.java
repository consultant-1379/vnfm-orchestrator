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
package com.ericsson.vnfm.orchestrator.model;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeOperationContext {
    private VnfInstance sourceVnfInstance;
    private VnfInstance tempInstance;
    private LifecycleOperation operation;
    private ChangePackageOperationSubtype changePackageOperationSubtype;
    private Map<String, Object> additionalParams;
    private String targetOperationOccurrenceId;
    private PackageResponse sourcePackageInfo;
    private PackageResponse targetPackageInfo;
    private String targetVnfdId;
    private ChangeCurrentVnfPkgRequest operationRequest;
    private boolean downsize;
    private boolean isAutoRollbackAllowed;

    public ChangeOperationContext(final VnfInstance sourceVnfInstance, ChangeCurrentVnfPkgRequest operationRequest) {
        this.operationRequest = operationRequest;
        this.sourceVnfInstance = sourceVnfInstance;
        this.targetVnfdId = operationRequest.getVnfdId();
    }

    public ChangeOperationContext(final VnfInstance sourceVnfInstance, ChangeCurrentVnfPkgRequest operationRequest, LifecycleOperation operation) {
        this.operationRequest = operationRequest;
        this.sourceVnfInstance = sourceVnfInstance;
        this.targetVnfdId = operationRequest.getVnfdId();
        this.operation = operation;
        this.targetOperationOccurrenceId = operation.getOperationOccurrenceId();
    }

    public boolean isDowngrade() {
        return ChangePackageOperationSubtype.DOWNGRADE == changePackageOperationSubtype;
    }

    public boolean isUpgrade() {
        return ChangePackageOperationSubtype.UPGRADE == changePackageOperationSubtype;
    }
}
