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
package com.ericsson.vnfm.orchestrator.presentation.services.oss;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.FTL_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.SUCCESS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.FAILED_OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.EXIT_STATUS_SUCCESS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.NOT_AVAILABLE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.ACTION_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESTORE_BACKUP_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESULT_INFO_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESULT_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RETURN_VALUE_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService.COMMAND_OUTPUT;
import static com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough.resolveTimeOut;
import static com.ericsson.vnfm.orchestrator.scheduler.CheckApplicationTimeout.TIME_OUT_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.utils.DelayUtils.delaySeconds;

import java.nio.file.Path;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.OssTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.vnfm.orchestrator.utils.SshResponseUtils;

@Component
public class RestoreBackupFromEnm {

    @Value("${oss.polling.delay}")
    private long delaySeconds;

    @Autowired
    private ObjectProvider<SshHelper> sshHelperProvider;

    @Autowired
    private OssTopologyService enmTopologyService;

    @Autowired
    private MessageUtility messageUtility;

    public void restoreLatestBackup(final Map<String, Object> topologyAttributes) {
        Path restoreLatest = enmTopologyService.generateRestoreScript(topologyAttributes,
                                                                      RESTORE_BACKUP_FILE,
                                                                      EnmOperationEnum.RESTORE_LATEST_BACKUP);
        SshHelper sshHelper = sshHelperProvider.getObject();
        SshResponse sshResponse = sshHelper.executeScript(restoreLatest);
        checkStatusExceptionally(topologyAttributes, sshResponse);
    }

    public Boolean restoreBackup(LifecycleOperation operation, Map<String, Object> topologyAttributes) {
        Path importBackup = enmTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, EnmOperationEnum.IMPORT_BACKUP);
        SshHelper sshHelper = sshHelperProvider.getObject();
        SshResponse importBackupResponse = sshHelper.executeScript(importBackup);
        if (!checkStatus(operation.getOperationOccurrenceId(), importBackupResponse, EnmOperationEnum.IMPORT_BACKUP)) {
            return false;
        }
        String actionId = getBackupCommandReturnValueAsString(importBackupResponse, EnmOperationEnum.IMPORT_BACKUP,
                                                              EnmOperationEnum.IMPORT_BACKUP.getImportBackupStatus(),
                                                              RETURN_VALUE_RESPONSE);
        topologyAttributes.put(ACTION_ID, actionId);

        Path showImportResultScript = enmTopologyService.generateRestoreScript(topologyAttributes,
                                                                               RESTORE_BACKUP_FILE,
                                                                               EnmOperationEnum.IMPORT_BACKUP_PROGRESS);
        SshResponse showImportResultScriptResponse = sshHelper.executeScript(showImportResultScript);
        if (!checkStatus(operation.getOperationOccurrenceId(), showImportResultScriptResponse, EnmOperationEnum.RESTORE_BACKUP)) {
            return false;
        }
        String result = getBackupCommandReturnValueAsString(showImportResultScriptResponse,
                                                            EnmOperationEnum.IMPORT_BACKUP_PROGRESS,
                                                            EnmOperationEnum.IMPORT_BACKUP_PROGRESS.getImportBackupProgress(), RESULT_RESPONSE);

        while (NOT_AVAILABLE.equals(result)) {
            delaySeconds(delaySeconds);
            if (Long.parseLong(resolveTimeOut(operation)) < 0) {
                messageUtility.lifecycleTimedOut(operation.getOperationOccurrenceId(), InstantiationState.INSTANTIATED,
                                                 String.format(TIME_OUT_ERROR_MESSAGE, operation.getOperationOccurrenceId()));
                return false;
            }
            showImportResultScriptResponse = sshHelper.executeScript(showImportResultScript);
            if (!checkStatus(operation.getOperationOccurrenceId(), showImportResultScriptResponse, EnmOperationEnum.RESTORE_BACKUP)) {
                return false;
            }
            result = getBackupCommandReturnValueAsString(showImportResultScriptResponse,
                                                         EnmOperationEnum.IMPORT_BACKUP_PROGRESS,
                                                         EnmOperationEnum.IMPORT_BACKUP_PROGRESS.getImportBackupProgress(),
                                                         RESULT_RESPONSE);
        }
        return processResult(operation.getOperationOccurrenceId(), topologyAttributes, showImportResultScriptResponse, result);
    }

    private boolean processResult(String operationId, Map<String, Object> topologyAttributes, SshResponse response, String result) {
        if (SUCCESS.equals(result)) {
            String resultInfo =
                    getBackupCommandReturnValueAsString(response,
                                                        EnmOperationEnum.IMPORT_BACKUP_PROGRESS,
                                                        EnmOperationEnum.IMPORT_BACKUP_PROGRESS.getImportBackupProgress(),
                                                        RESULT_INFO_RESPONSE);
            topologyAttributes.put(BACKUP_FILE, resultInfo);
            return restoreFromUrl(operationId, topologyAttributes);
        }
        String resultInfo = getBackupCommandReturnValueAsString(response, EnmOperationEnum.IMPORT_BACKUP_PROGRESS,
                                                                EnmOperationEnum.IMPORT_BACKUP_PROGRESS.getImportBackupProgress(),
                                                                RESULT_INFO_RESPONSE);
        messageUtility.updateOperationOnFail(operationId, resultInfo,
                                             String.format(FAILED_OPERATION,
                                                           operationId, result), InstantiationState.INSTANTIATED,
                                             LifecycleOperationState.FAILED);
        return false;
    }

    private boolean restoreFromUrl(String operationId, Map<String, Object> topologyAttributes) {
        Path restoreScript = enmTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, EnmOperationEnum.RESTORE_BACKUP);
        SshHelper sshHelper = sshHelperProvider.getObject();
        SshResponse restoreBackupResponse = sshHelper.executeScript(restoreScript);
        return checkStatus(operationId, restoreBackupResponse, EnmOperationEnum.RESTORE_BACKUP);
    }

    private static void checkStatusExceptionally(final Map<String, Object> topologyAttributes,
                                                 final SshResponse sshResponse) {
        if (sshResponse.getExitStatus() != EXIT_STATUS_SUCCESS) {
            String specificErrorMessage = SshResponseUtils.extractSpecificFailure(sshResponse, EnmOperationEnum.RESTORE_BACKUP);
            throw new FileExecutionException(String.format(FTL_EXCEPTION,
                                                           EnmOperationEnum.RESTORE_LATEST_BACKUP.getOperation(),
                                                           topologyAttributes.toString(),
                                                           specificErrorMessage));
        }
    }

    private boolean checkStatus(final String operationId,
                                final SshResponse sshResponse, final EnmOperationEnum operation) {
        if (sshResponse.getExitStatus() != EXIT_STATUS_SUCCESS) {
            String specificErrorMessage = SshResponseUtils.extractSpecificFailure(sshResponse, operation);
            messageUtility.updateOperationOnFail(operationId,
                                                 specificErrorMessage,
                                                 String.format(FAILED_OPERATION, operationId, specificErrorMessage),
                                                 InstantiationState.INSTANTIATED,
                                                 LifecycleOperationState.FAILED);
            return false;
        }
        return true;
    }

    private static String getBackupCommandReturnValueAsString(SshResponse sshResponse, EnmOperationEnum operation, String operationKey,
                                                              String responseKey) {
        try {
            JSONObject jsonOutput = new JSONObject(sshResponse.getOutput());
            return (String) jsonOutput
                    .getJSONObject(operation.getOperationResponse())
                    .getJSONObject(operationKey)
                    .getJSONObject(COMMAND_OUTPUT)
                    .get(responseKey);
        } catch (JSONException e) {
            throw new InternalRuntimeException(String.format("Cannot get import backup status from ssh response due to :: %s", e.getMessage()), e);
        }
    }
}
