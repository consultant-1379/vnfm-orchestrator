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

import static java.util.stream.Collectors.toMap;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DOWNSIZE_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.removeExcessAdditionalParams;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.isResourcesAllowedByVnfd;

import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangeOperationContext;
import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.utils.VnfdUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EnhancedChangeOperationContextBuilder implements ChangeOperationContextBuilder {

    @Autowired
    private MessageUtility messageUtility;

    @Autowired
    private ChangeVnfPackageService changeVnfPackageService;

    @Autowired
    private PackageService packageService;

    @Override
    public ChangeOperationContext build(final VnfInstance vnfInstance, LifecycleOperation operation, ChangeCurrentVnfPkgRequest request) {
        ChangeOperationContext context = new ChangeOperationContext(vnfInstance, request);
        context.setOperation(operation);

        final VnfInstance tempInstance = createTempVnfInstance(operation, vnfInstance);
        context.setTempInstance(tempInstance);

        updateContextWithOperationType(context);

        Map<String, Object> additionalParams = checkAndCastObjectToMap(request.getAdditionalParams());
        context.setDownsize(getParameterFromAdditionalParams(additionalParams, context.getSourcePackageInfo(), DOWNSIZE_VNFD_KEY));
        context.setAutoRollbackAllowed(
                getParameterFromAdditionalParams(additionalParams, context.getSourcePackageInfo(), IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY));
        updateContextWithRequestAdditionalParams(context, additionalParams);
        return context;
    }

    private void updateContextWithOperationType(final ChangeOperationContext context) {
        try {
            final PackageResponse targetPackageInfo = packageService.getPackageInfoWithDescriptorModel(context.getTargetVnfdId());
            context.setTargetPackageInfo(targetPackageInfo);
            changeVnfPackageService.getSuitableTargetDowngradeOperationFromVnfInstance(context.getTargetVnfdId(), context.getSourceVnfInstance())
                    .ifPresentOrElse(downgradeOperation -> updateContextForDowngrade(context, downgradeOperation), () ->
                            updateContextForUpgrade(context));
        } catch (PackageDetailsNotFoundException pe) {
            messageUtility.updateOperation(pe.getMessage(), context.getOperation(), LifecycleOperationState.FAILED);
            throw new PackageDetailsNotFoundException(pe.getMessage(), pe);
        }
    }

    private void updateContextWithRequestAdditionalParams(final ChangeOperationContext context,
                                                          final Map<String, Object> additionalParams) {
        removeExcessAdditionalParams(additionalParams);
        if (context.getAdditionalParams() == null) {
            context.setAdditionalParams(additionalParams);
        } else {
            context.getAdditionalParams().putAll(additionalParams);
        }
    }

    private boolean getParameterFromAdditionalParams(final Map<String, Object> additionalParams, final PackageResponse packageInfo,
                                                     final String parameter) {
        final JSONObject vnfd = new JSONObject(packageInfo.getDescriptorModel());
        if (additionalParams.get(parameter) == null) {
            return isResourcesAllowedByVnfd(vnfd, parameter);
        } else {
            return parameter.equals(DOWNSIZE_VNFD_KEY) ?
                    toBooleanDefaultIfNull(Boolean.valueOf(additionalParams.get(parameter).toString()), false) :
                    toBooleanDefaultIfNull(Boolean.valueOf(additionalParams.get(parameter).toString()), true);
        }
    }

    private void updateContextForUpgrade(final ChangeOperationContext context) {
        context.setSourcePackageInfo(context.getTargetPackageInfo());
        context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.UPGRADE);
    }

    private void updateContextForDowngrade(final ChangeOperationContext context,
                                           final LifecycleOperation downgradeOperation) {
        final String sourceVnfdId = context.getSourceVnfInstance().getVnfDescriptorId();
        final PackageResponse sourceVnfPackage = packageService.getPackageInfoWithDescriptorModel(sourceVnfdId);
        String sourceSoftwareVersion = context.getSourceVnfInstance().getVnfSoftwareVersion();
        String targetSoftwareVersion = context.getTargetPackageInfo().getVnfSoftwareVersion();

        Map<String, Property> downgradeParams = getVnfdDowngradeParams(context, sourceVnfPackage,
                sourceSoftwareVersion, targetSoftwareVersion);
        context.setSourcePackageInfo(sourceVnfPackage);
        if (downgradeParams == null) {
            LOGGER.info("No downgrade policy presents for source id {} and target id {}, upgrade will be performed", sourceVnfdId,
                        context.getTargetVnfdId());
            updateContextForUpgrade(context);
        } else {
            context.setTargetOperationOccurrenceId(downgradeOperation.getOperationOccurrenceId());
            context.setChangePackageOperationSubtype(ChangePackageOperationSubtype.DOWNGRADE);
            Map<String, Object> additionalParams = buildAdditionalParams(downgradeParams);
            context.setAdditionalParams(additionalParams);
        }
    }

    private Map<String, Property> getVnfdDowngradeParams(ChangeOperationContext context, PackageResponse sourceVnfPackage,
                                                         String sourceSoftwareVersion, String targetSoftwareVersion) {
        JSONObject vnfd = new JSONObject(sourceVnfPackage.getDescriptorModel());
        String sourceVnfdId = context.getSourceVnfInstance().getVnfDescriptorId();
        String targetVnfdId = context.getTargetVnfdId();
        return VnfdUtils.getVnfdDowngradeParams(vnfd, sourceVnfdId, targetVnfdId, sourceSoftwareVersion, targetSoftwareVersion);
    }

    private VnfInstance createTempVnfInstance(LifecycleOperation operation, VnfInstance vnfInstance) {
        final VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);
        tempInstance.setOperationOccurrenceId(operation.getOperationOccurrenceId());
        return tempInstance;
    }

    private Map<String, Object> buildAdditionalParams(Map<String, Property> downgradeParams) {
        return downgradeParams.entrySet().stream()
                .filter(entry -> entry.getValue().getDefaultValue() != null)
                .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().getDefaultValue()));
    }
}
