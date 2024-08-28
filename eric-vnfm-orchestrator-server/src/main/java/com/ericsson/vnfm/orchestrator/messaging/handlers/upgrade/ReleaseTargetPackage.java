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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;

import java.util.Optional;

import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateUsageState;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

public class ReleaseTargetPackage extends UpdateUsageState {

    private final ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;
    private final PackageService packageService;

    public ReleaseTargetPackage(final InstanceService instanceService, final boolean inUse,
                                ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository,
                                PackageService packageService) {
        super(instanceService, inUse);
        this.changePackageOperationDetailsRepository = changePackageOperationDetailsRepository;
        this.packageService = packageService;
    }

    @Override
    protected String getTargetPackageId(final MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository
                .findById(context.getOperation().getOperationOccurrenceId());
        if (isDowngradeOperation(changePackageOperationDetails)) {
            return packageService.getPackageInfo(context.getOperation().getTargetVnfdId()).getId();
        }
        return super.getTargetPackageId(context);
    }
}
