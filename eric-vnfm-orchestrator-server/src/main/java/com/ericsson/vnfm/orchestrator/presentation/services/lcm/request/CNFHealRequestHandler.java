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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.IP_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler.parameterPresent;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.HealRequestType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;

@Component
public class CNFHealRequestHandler implements HealRequestService {

    private static final String INVALID_BU_REF = "%s is not an acceptable value for backupFileReference. It "
            + "must either be \"Latest\" or a valid url";

    @Autowired
    private UrlValidator validator;

    @Autowired
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    @Override
    public VnfInstance specificValidation(VnfInstance vnfInstance, HealVnfRequest healVnfRequest) {
        Map<String, Object> additionalParams = checkAndCastObjectToMap(healVnfRequest.getAdditionalParams());

        String backupFileReference = parameterPresent(additionalParams, RESTORE_BACKUP_FILE_REFERENCE) ?
                (String) additionalParams.get(RESTORE_BACKUP_FILE_REFERENCE) :
                "";

        String backupPassword = parameterPresent(additionalParams, RESTORE_PASSWORD) ?
                (String) additionalParams.get(RESTORE_PASSWORD) :
                "";
        validateBackupParams(backupFileReference, backupPassword);
        cmpEnrollmentHelper.generateAndSaveOssNodeProtocolFile(vnfInstance, additionalParams);
        return vnfInstance;
    }

    @Override
    public TerminateVnfRequest prepareTerminateRequest(final TerminateVnfRequest terminateVnfRequest,
                                                       final TerminateRequestHandler terminateRequestHandler,
                                                       final VnfInstance vnfInstance) {
        terminateRequestHandler.setAlarmSupervisionWithWarning(vnfInstance, DISABLE_ALARM_SUPERVISION);

        Map<String, Object> additionalParams = checkAndCastObjectToMap(terminateVnfRequest.getAdditionalParams());
        additionalParams.remove(RESTORE_BACKUP_FILE_REFERENCE);
        additionalParams.remove(IP_VERSION);

        return terminateVnfRequest;
    }

    @Override
    public HealRequestType getType() {
        return HealRequestType.CNF;
    }

    void validateBackupParams(final String backupFileReference, final String password) {
        if ("Latest".equalsIgnoreCase(backupFileReference) || StringUtils.isEmpty(backupFileReference)) {
            return;
        }

        if (validator.isValid(backupFileReference)) {
            if (StringUtils.isEmpty(password)) {
                throw new InvalidInputException("Password cannot be null or empty");
            }
        } else {
            throw new InvalidInputException(String.format(INVALID_BU_REF, backupFileReference));
        }
    }

}