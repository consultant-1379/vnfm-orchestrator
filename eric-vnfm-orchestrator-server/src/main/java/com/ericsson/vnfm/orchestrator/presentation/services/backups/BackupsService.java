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

import java.util.List;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.CreateBackupsRequest;

public interface BackupsService {
    /**
     * Creates a backup
     *
     * @param createBackupsRequest request object used for creating a backup
     * @param vnfInstanceId the vnfInstanceId
     */
    void createBackup(CreateBackupsRequest createBackupsRequest, String vnfInstanceId);

    /***
     * Returns a list of backups created for an instance
     *
     * @param vnfInstanceId
     * @return
     */
    List<BackupsResponseDto> getAllBackups(String vnfInstanceId);

    /**
     * Get list of scopes
     *
     * @param vnfInstanceId the vnfInstanceId
     */
    List<String> getScopes(String vnfInstanceId);

    /**
     * Delete backup for an instance, scope and backupName
     *
     * @param vnfInstanceId
     * @param scope
     * @param backupName
     * @return
     */
    void deleteBackup(String vnfInstanceId, String scope, String backupName);
}
