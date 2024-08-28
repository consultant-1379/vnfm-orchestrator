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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.FTL_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESTORE_BACKUP_FILE;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;


@SpringBootTest(classes = RestoreBackupFromEnm.class)
public class RestoreBackupFromEnmTest {

    private final Path importPath = Path.of("tmp/importPath.py");
    private final Path importProgressPath = Path.of("tmp/importProgressPath.py");
    private final Path restorePath = Path.of("tmp/restorePath.py");
    private final Path restoreLatestPath = Path.of("tmp/restoreLatestPath.py");

    @Autowired
    private RestoreBackupFromEnm restoreBackupFromEnm;

    @MockBean
    private SshHelper sshHelper;

    @MockBean
    private EnmTopologyService enmTopologyService;

    @MockBean
    private MessageUtility messageUtility;

    @Test
    public void shouldRestoreLatestBackupSuccessfully() {
        String restoreLatestBackupCommandOutput = getFile("restore-backup/restoreLatestBackupCommandOutput.json");
        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.RESTORE_LATEST_BACKUP);
        SshResponse restoreLatestSshResponse = buildSshResponse(0, restoreLatestBackupCommandOutput);
        when(enmTopologyService.generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE), eq(EnmOperationEnum.RESTORE_LATEST_BACKUP))).thenReturn(
                restoreLatestPath);
        doReturn(restoreLatestSshResponse).when(sshHelper).executeScript(restoreLatestPath);
        assertThatNoException().isThrownBy(() -> restoreBackupFromEnm.restoreLatestBackup(topologyAttr));
    }

    @Test
    public void shouldFailRestoreLatestBackup() {
        String restoreLatestBackupCommandOutput = getFile("restore-backup/restoreLatestBackupFailureCommandOutput.json");
        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.RESTORE_LATEST_BACKUP);
        SshResponse restoreLatestSshResponse = buildSshResponse(1, restoreLatestBackupCommandOutput);
        when(enmTopologyService.generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE), eq(EnmOperationEnum.RESTORE_LATEST_BACKUP))).thenReturn(
                restoreLatestPath);
        doReturn(restoreLatestSshResponse).when(sshHelper).executeScript(restoreLatestPath);
        assertThatThrownBy(() -> {
            restoreBackupFromEnm.restoreLatestBackup(topologyAttr);
        }).isInstanceOf(FileExecutionException.class)
                .hasMessage(String.format(FTL_EXCEPTION,
                                          EnmOperationEnum.RESTORE_LATEST_BACKUP.getOperation(),
                                          topologyAttr.toString(),
                                          restoreLatestBackupCommandOutput));
    }

    @Test
    public void shouldRestoreBackupFromEnmSuccessfully() {
        String importBackupCommandOutput = getFile("restore-backup/importBackupCommandOutput.json");
        String importBackupProgressCommandOutput = getFile("restore-backup/importBackupProgressCommandOutput.json");
        String importBackupProgressCompletedCommandOutput = getFile("restore-backup/importBackupCompletedCommandOutput.json");
        String restoreBackupCommandOutput = getFile("restore-backup/restoreBackupCommandOutput.json");

        SshResponse importSshResponse = buildSshResponse(0, importBackupCommandOutput);
        SshResponse importProgressSshResponse = buildSshResponse(0, importBackupProgressCommandOutput);
        SshResponse importProgressCompletedSshResponse = buildSshResponse(0, importBackupProgressCompletedCommandOutput);
        SshResponse restoreSshResponse = buildSshResponse(0, restoreBackupCommandOutput);
        doReturn(importPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                            eq(EnmOperationEnum.IMPORT_BACKUP));
        doReturn(importProgressPath).doReturn(importProgressPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                                                                 eq(EnmOperationEnum.IMPORT_BACKUP_PROGRESS));
        doReturn(restorePath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                             eq(EnmOperationEnum.RESTORE_BACKUP));

        doReturn(importSshResponse).when(sshHelper).executeScript(importPath);
        doReturn(importProgressSshResponse).doReturn(importProgressCompletedSshResponse).when(sshHelper).executeScript(importProgressPath);
        doReturn(restoreSshResponse).when(sshHelper).executeScript(restorePath);
        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.IMPORT_BACKUP);
        LifecycleOperation lifecycleOperation = buildLifecycleOperation("operation-id",
                                                                        LocalDateTime.now().plusMinutes(10));

        restoreBackupFromEnm.restoreBackup(lifecycleOperation, topologyAttr);

        verify(sshHelper, times(1)).executeScript(importPath);
        verify(sshHelper, times(2)).executeScript(importProgressPath);
        verify(sshHelper, times(1)).executeScript(restorePath);

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void shouldRestoreBackupFromEnmSuccessfullyWhenImportedImmediately() {
        String importBackupCommandOutput = getFile("restore-backup/importBackupCommandOutput.json");
        String importBackupProgressCompletedCommandOutput = getFile("restore-backup/importBackupCompletedCommandOutput.json");
        String restoreBackupCommandOutput = getFile("restore-backup/restoreBackupCommandOutput.json");

        SshResponse importSshResponse = buildSshResponse(0, importBackupCommandOutput);
        SshResponse importProgressCompletedSshResponse = buildSshResponse(0, importBackupProgressCompletedCommandOutput);
        SshResponse restoreSshResponse = buildSshResponse(0, restoreBackupCommandOutput);

        doReturn(importPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                            eq(EnmOperationEnum.IMPORT_BACKUP));
        doReturn(importProgressPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                                    eq(EnmOperationEnum.IMPORT_BACKUP_PROGRESS));
        doReturn(restorePath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                             eq(EnmOperationEnum.RESTORE_BACKUP));

        doReturn(importSshResponse).when(sshHelper).executeScript(importPath);
        doReturn(importProgressCompletedSshResponse).when(sshHelper).executeScript(importProgressPath);
        doReturn(restoreSshResponse).when(sshHelper).executeScript(restorePath);
        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.IMPORT_BACKUP);
        LifecycleOperation lifecycleOperation = buildLifecycleOperation("operation-id",
                                                                        LocalDateTime.now().plusMinutes(10));

        restoreBackupFromEnm.restoreBackup(lifecycleOperation, topologyAttr);

        verify(sshHelper, times(1)).executeScript(importPath);
        verify(sshHelper, times(1)).executeScript(importProgressPath);
        verify(sshHelper, times(1)).executeScript(restorePath);

        verifyNoInteractions(messageUtility);
    }

    @Test
    public void shouldFailRestoreBackupFromEnmWhenImportFailed() {
        String importBackupCommandOutput = getFile("restore-backup/importBackupFailureCommandOutput.json");
        String importBackupProgressFailureCommandOutput = getFile("restore-backup/importBackupProgressFailureCommandOutput.json");
        SshResponse importSshResponse = buildSshResponse(1, importBackupCommandOutput);
        SshResponse importProgressSshResponse = buildSshResponse(1, importBackupProgressFailureCommandOutput);

        doReturn(importPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                            eq(EnmOperationEnum.IMPORT_BACKUP));

        doReturn(importSshResponse).when(sshHelper).executeScript(importPath);
        doReturn(importProgressSshResponse).when(sshHelper).executeScript(importProgressPath);

        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.IMPORT_BACKUP);
        LifecycleOperation lifecycleOperation = buildLifecycleOperation("operation-id",
                                                                        LocalDateTime.now().plusMinutes(10));

        restoreBackupFromEnm.restoreBackup(lifecycleOperation, topologyAttr);

        verify(sshHelper, times(1)).executeScript(importPath);
        verify(sshHelper, never()).executeScript(importProgressPath);

        verify(messageUtility).updateOperationOnFail(eq("operation-id"),
                                                     eq(importBackupCommandOutput),
                                                     eq(String.format("Mark operation with id : operation-id as failed due to %s",
                                                                      importBackupCommandOutput)),
                                                     eq(InstantiationState.INSTANTIATED),
                                                     eq(LifecycleOperationState.FAILED));
    }

    @Test
    public void shouldFailRestoreBackupFromEnmWhenPollImportFailed() {
        String importBackupCommandOutput = getFile("restore-backup/importBackupCommandOutput.json");
        String importBackupProgressFailureCommandOutput = getFile("restore-backup/importBackupProgressFailureCommandOutput.json");
        String restoreBackupCommandOutput = getFile("restore-backup/restoreBackupCommandOutput.json");
        SshResponse importSshResponse = buildSshResponse(0, importBackupCommandOutput);
        SshResponse importProgressSshResponse = buildSshResponse(1, importBackupProgressFailureCommandOutput);
        SshResponse restoreSshResponse = buildSshResponse(0, restoreBackupCommandOutput);

        doReturn(importPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                            eq(EnmOperationEnum.IMPORT_BACKUP));
        doReturn(importProgressPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                                    eq(EnmOperationEnum.IMPORT_BACKUP_PROGRESS));

        doReturn(importSshResponse).when(sshHelper).executeScript(importPath);
        doReturn(importProgressSshResponse).when(sshHelper).executeScript(importProgressPath);
        doReturn(restoreSshResponse).when(sshHelper).executeScript(restorePath);

        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.IMPORT_BACKUP);

        LifecycleOperation lifecycleOperation = buildLifecycleOperation("operation-id",
                                                                        LocalDateTime.now().plusMinutes(10));

        restoreBackupFromEnm.restoreBackup(lifecycleOperation, topologyAttr);

        verify(sshHelper, times(1)).executeScript(importPath);
        verify(sshHelper, times(1)).executeScript(importProgressPath);
        verify(sshHelper, never()).executeScript(restorePath);

        verify(messageUtility).updateOperationOnFail(eq("operation-id"),
                                                     eq(importBackupProgressFailureCommandOutput),
                                                     eq(String.format("Mark operation with id : operation-id as failed due to %s",
                                                                      importBackupProgressFailureCommandOutput)),
                                                     eq(InstantiationState.INSTANTIATED),
                                                     eq(LifecycleOperationState.FAILED));
    }

    @Test
    public void shouldFailRestoreBackupFromEnmWhenTimeoutExpires() {
        String importBackupCommandOutput = getFile("restore-backup/importBackupCommandOutput.json");
        String importBackupProgressCommandOutput = getFile("restore-backup/importBackupProgressCommandOutput.json");
        SshResponse importSshResponse = buildSshResponse(0, importBackupCommandOutput);
        SshResponse importProgressSshResponse = buildSshResponse(0, importBackupProgressCommandOutput);

        doReturn(importSshResponse).when(sshHelper).executeScript(importPath);
        doReturn(importProgressSshResponse).when(sshHelper).executeScript(importProgressPath);

        doReturn(importPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                            eq(EnmOperationEnum.IMPORT_BACKUP));
        doReturn(importProgressPath).when(enmTopologyService).generateRestoreScript(anyMap(), eq(RESTORE_BACKUP_FILE),
                                                                                    eq(EnmOperationEnum.IMPORT_BACKUP_PROGRESS));

        Map<String, Object> topologyAttr = addCommonScriptAttributes(EnmOperationEnum.IMPORT_BACKUP);
        LifecycleOperation lifecycleOperation = buildLifecycleOperation("operation-id",
                                                                        LocalDateTime.now());

        restoreBackupFromEnm.restoreBackup(lifecycleOperation, topologyAttr);

        verify(sshHelper, times(1)).executeScript(importPath);
        verify(sshHelper, times(1)).executeScript(importProgressPath);

        verify(messageUtility).lifecycleTimedOut(eq("operation-id"),
                                                 eq(InstantiationState.INSTANTIATED),
                                                 eq("Lifecycle operation operation-id failed due to timeout"));
    }

    private SshResponse buildSshResponse(int exitStatus, String output) {
        SshResponse sshResponse = new SshResponse();
        sshResponse.setExitStatus(exitStatus);
        sshResponse.setOutput(output);
        return sshResponse;
    }

    private LifecycleOperation buildLifecycleOperation(String id, LocalDateTime expiredTime) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationOccurrenceId(id);
        lifecycleOperation.setExpiredApplicationTime(expiredTime);
        return lifecycleOperation;
    }

    private Map<String, Object> addCommonScriptAttributes(EnmOperationEnum operation) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(MANAGED_ELEMENT_ID, "restore-backup-id");
        attributes.put(OPERATION, EnmOperationEnum.RESTORE_BACKUP);
        attributes.put(OPERATION_RESPONSE, operation.getOperationResponse());
        return attributes;
    }

    private String getFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }
}
