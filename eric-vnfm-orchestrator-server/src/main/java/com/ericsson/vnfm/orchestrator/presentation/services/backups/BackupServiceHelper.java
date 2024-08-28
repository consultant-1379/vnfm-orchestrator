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
package com.ericsson.vnfm.orchestrator.presentation.services.backups;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.REMOTE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.REMOTE_HOST;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.SCOPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.INVALID_REMOTE_OBJ;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.MISSING_VNF_INSTANCE_PARAMS_MESSAGE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.CreateBackupsRequest;
import com.ericsson.vnfm.orchestrator.model.backup.CreateBackupsInfo;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidOperationStateException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingMandatoryParameterException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackupServiceHelper {

    @SuppressWarnings("unchecked")
    public static CreateBackupsInfo getBackupsInfo(final CreateBackupsRequest createBackupsRequest) {
        CreateBackupsInfo createBackupsInfo = new CreateBackupsInfo();
        Map<String, Object> additionalParams = checkAndCastObjectToMap(createBackupsRequest.getAdditionalParams());
        createBackupsInfo.setScope(getStringValue(additionalParams.get(SCOPE)));
        createBackupsInfo.setBackupName(getStringValue(additionalParams.get(BACKUP_NAME)));

        Optional<Object> remote = Optional.ofNullable(additionalParams.get(REMOTE));
        remote.ifPresent(remoteObj -> {
            if (!(remoteObj instanceof Map)) {
                throw new InvalidInputException(INVALID_REMOTE_OBJ);
            }
            Map<String, Object> remoteParams = (Map<String, Object>) remoteObj;
            createBackupsInfo.setRemoteAction(true);
            createBackupsInfo.setRemoteHost(getStringValue(remoteParams.get(REMOTE_HOST)));
            createBackupsInfo.setPassword(getStringValue(remoteParams.get(PASSWORD)));
        });
        return createBackupsInfo;
    }

    public static void validateOperationState(LifecycleOperation lifecycleOperation) {
        if (!lifecycleOperation.getOperationState().equals(LifecycleOperationState.COMPLETED)) {
            throw new InvalidOperationStateException("Operation state has to be in COMPLETED in order to create a Snapshot");
        }
    }

    public static void validateBroEndpointUrl(VnfInstance vnfInstance) {
        if (StringUtils.isEmpty(vnfInstance.getBroEndpointUrl())) {
            throw new MissingMandatoryParameterException(MISSING_VNF_INSTANCE_PARAMS_MESSAGE);
        }
    }

    private static String getStringValue(Object val) {
        return val == null ? null : String.valueOf(val);
    }

}
