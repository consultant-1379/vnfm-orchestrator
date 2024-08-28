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
package com.ericsson.vnfm.orchestrator.routing.bro;

import java.util.List;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionResponse;

public interface BroHttpRoutingClient {

    /**
     * Return list of backup scopes.
     * @param broUrl url for bro service.
     * @return
     */
    List<String> getAllScopes(String broUrl);

    /**
     * Return list of backups for each scope.
     * @param broUrl
     * @param scope
     * @return
     */
    List<BackupsResponseDto> getBackupsByScope(String broUrl, String scope);

    /***
     * Exports the created backup to an external location
     * @param broUrl
     * @param scope
     * @param backupName
     * @return
     */
    BroActionResponse exportBackup(String broUrl, String scope, String backupName, String remoteHost, String password);

    /**
     * Create a backup for given name and scope.
     * @param broUrl
     * @param scope
     * @param backupName
     * @return
     */
    BroActionResponse createBackup(String broUrl, String scope, String backupName);

    /**
     * Delete the backup for given name and scope.
     * @param broUrl
     * @param scope
     * @param backupName
     * @return
     */
    BroActionResponse deleteBackup(String broUrl, String scope, String backupName);
}
