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

import static com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupServiceHelper.getBackupsInfo;
import static com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupServiceHelper.validateBroEndpointUrl;
import static com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupServiceHelper.validateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.CreateBackupsRequest;
import com.ericsson.vnfm.orchestrator.model.backup.CreateBackupsInfo;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingMandatoryParameterException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.routing.bro.BroHttpRoutingClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BackupsServiceImpl implements BackupsService {
    @Autowired
    private BroHttpRoutingClient broHttpRoutingClient;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Override
    public void createBackup(CreateBackupsRequest createBackupsRequest, String vnfInstanceId) {
        validateAdditionalParametersPresent(createBackupsRequest);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);

        LifecycleOperation lifecycleOperation = databaseInteractionService
                .getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
        validateOperationState(lifecycleOperation);

        LOGGER.debug("Found VNF instance {}", vnfInstance.getVnfInstanceId());

        String broEndpointUrl = getBroEndpointUrl(vnfInstance);
        CreateBackupsInfo createBackupsInfo = getBackupsInfo(createBackupsRequest);
        if (createBackupsInfo.isRemoteAction()) {
            broHttpRoutingClient.exportBackup(broEndpointUrl,
                    createBackupsInfo.getScope(), createBackupsInfo.getBackupName(),
                    createBackupsInfo.getRemoteHost(), createBackupsInfo.getPassword());
        } else {
            broHttpRoutingClient.createBackup(broEndpointUrl,
                    createBackupsInfo.getScope(),
                    createBackupsInfo.getBackupName());
        }
    }

    public List<BackupsResponseDto> getAllBackups(final String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);
        String broUrl = getBroEndpointUrl(vnfInstance);
        List<String> scopes = broHttpRoutingClient.getAllScopes(broUrl);
        return scopes.parallelStream()
                .map(s -> broHttpRoutingClient.getBackupsByScope(broUrl, s))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static String getBroEndpointUrl(final VnfInstance vnfInstance) {
        validateBroEndpointUrl(vnfInstance);
        return vnfInstance.getBroEndpointUrl();
    }

    @Override
    public List<String> getScopes(String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        if (vnfInstance.getInstantiationState().equals(InstantiationState.NOT_INSTANTIATED)) {
            throw new NotInstantiatedException(vnfInstance);
        }
        return broHttpRoutingClient.getAllScopes(getBroEndpointUrl(vnfInstance));
    }

    @Override
    public void deleteBackup(final String vnfInstanceId, final String scope, final String backupName) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);

        LOGGER.debug("Found VNF instance {}", vnfInstance.getVnfInstanceId());

        broHttpRoutingClient.deleteBackup(getBroEndpointUrl(vnfInstance),
                                          scope,
                                          backupName);
    }

    private void validateAdditionalParametersPresent(CreateBackupsRequest createBackupsRequest) {
        if (createBackupsRequest.getAdditionalParams() == null) {
            throw new MissingMandatoryParameterException("additionalParams for Backup request must not be null");
        }
    }
}
