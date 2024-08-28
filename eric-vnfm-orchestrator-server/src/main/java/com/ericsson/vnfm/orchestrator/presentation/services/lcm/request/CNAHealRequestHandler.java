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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_SCOPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.parameterPresent;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HealRequestType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CNAHealRequestHandler implements HealRequestService {

    @Autowired
    private Day0ConfigurationService day0ConfigurationService;

    @Override
    public VnfInstance specificValidation(VnfInstance vnfInstance, HealVnfRequest healVnfRequest) {
        Map<String, Object> additionalParams = checkAndCastObjectToMap(healVnfRequest.getAdditionalParams());

        validateRequiredParams(additionalParams);
        Map<String, Object> day0Params = new HashMap<>();

        day0Params.computeIfAbsent(DAY0_CONFIGURATION_PREFIX,
            day0 -> {
                Map<String, String> formattedParams =  day0ConfigurationService.retrieveDay0ConfigurationParams(additionalParams);
                return !formattedParams.isEmpty() ?  formattedParams : null;
            });
        if (!CollectionUtils.isEmpty(day0Params)) {
            additionalParams.putAll(day0Params);
            healVnfRequest.setAdditionalParams(additionalParams);
        }

        return vnfInstance;
    }

    @Override
    public TerminateVnfRequest prepareTerminateRequest(final TerminateVnfRequest terminateVnfRequest,
                                                       final TerminateRequestHandler terminateRequestHandler,
                                                       final VnfInstance vnfInstance) {
        LOGGER.info("No specific termination validation for CNA needed");
        return terminateVnfRequest;
    }

    private static void validateRequiredParams(Map additionalParams) {
        if (!paramIsPresentAndNotNullOrEmpty(additionalParams, RESTORE_BACKUP_NAME)) {
            throw new InvalidHealRequestException(String.format("Invalid CNA Restore request. %s is not present or an invalid value.",
                                                                RESTORE_BACKUP_NAME));
        } else if (!paramIsPresentAndNotNullOrEmpty(additionalParams, RESTORE_SCOPE)) {
            throw new InvalidHealRequestException(String.format("Invalid CNA Restore request. %s is not present or an invalid value.",
                                                                RESTORE_SCOPE));
        }
    }

    private static boolean paramIsPresentAndNotNullOrEmpty(Map additionalParams, String paramName) {
        if (parameterPresent(additionalParams, paramName)) {
            return StringUtils.isNotEmpty((String) additionalParams.get(paramName));
        }
        return false;
    }

    @Override
    public HealRequestType getType() {
        return HealRequestType.CNA;
    }
}
