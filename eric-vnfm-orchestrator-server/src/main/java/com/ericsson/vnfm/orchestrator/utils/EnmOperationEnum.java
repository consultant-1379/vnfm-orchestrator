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
package com.ericsson.vnfm.orchestrator.utils;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.IMPORT_OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.IMPORT_PROGRESS_OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESTORE_LATEST_OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESTORE_OPERATION_RESPONSE;

public enum EnmOperationEnum {
    ADD_NODE(new EnmOperationBuilder("addNode", "add_node_data")
                     .isAddedToOss(true)),
    DELETE_NODE(new EnmOperationBuilder("deleteNode", "delete_node_data")
                        .isAddedToOss(false)),
    CHECK_NODE(new EnmOperationBuilder("checkNodePresent", "check_node_data")),
    ENABLE_SUPERVISION(new EnmOperationBuilder("enableSupervisions", "enable_supervisions_data")),
    ENABLE_ALARM_SUPERVISION(new EnmOperationBuilder("enableAlarmSupervision", "set_alarm_data")
                                     .setValue("on")),
    DISABLE_ALARM_SUPERVISION(new EnmOperationBuilder("disableAlarmSupervision", "set_alarm_data")
                                      .setValue("off")),
    ENROLLMENT_INFO(new EnmOperationBuilder("generateEnrollmentInfo", "enrollment_info_data")
                            .getLdapDetails("getLdapInfoStatus")),
    RESTORE_BACKUP(new EnmOperationBuilder("restoreBackup", RESTORE_OPERATION_RESPONSE)),
    RESTORE_LATEST_BACKUP(new EnmOperationBuilder("restoreLatestBackup", RESTORE_LATEST_OPERATION_RESPONSE)),
    IMPORT_BACKUP(new EnmOperationBuilder("importBackup", IMPORT_OPERATION_RESPONSE)
                          .getImportBackupStatus("importBackupStatus")),
    IMPORT_BACKUP_PROGRESS(new EnmOperationBuilder("importBackupProgress", IMPORT_PROGRESS_OPERATION_RESPONSE)
                                   .getImportBackupProgress("importBackupProgress"));

    private final String operation;
    private final String operationResponse;
    private final Boolean addedToOss;
    private final String setValue;
    private final String ldapDetails;
    private final String importBackupStatus;
    private final String importBackupProgress;

    EnmOperationEnum(EnmOperationBuilder enmOperationBuilder) {
        this.operation = enmOperationBuilder.operation;
        this.operationResponse = enmOperationBuilder.operationResponse;
        this.addedToOss = enmOperationBuilder.addedToOss;
        this.setValue = enmOperationBuilder.setValue;
        this.ldapDetails = enmOperationBuilder.ldapDetails;
        this.importBackupStatus = enmOperationBuilder.importBackupStatus;
        this.importBackupProgress = enmOperationBuilder.importBackupProgress;
    }

    public String getSetValue() {
        return setValue;
    }

    public Boolean getAddedToOss() {
        return addedToOss;
    }

    public String getOperationResponse() {
        return operationResponse;
    }

    public String getOperation() {
        return operation;
    }

    public String getLdapDetails() {
        return ldapDetails;
    }

    public String  getImportBackupStatus() {
        return importBackupStatus;
    }

    public String  getImportBackupProgress() {
        return importBackupProgress;
    }

    private static class EnmOperationBuilder {
        private final String operation;
        private final String operationResponse;
        private Boolean addedToOss;
        private String setValue;
        private String ldapDetails;
        private String importBackupStatus;
        private String importBackupProgress;

        EnmOperationBuilder(String operation, String operationResponse) {
            this.operation = operation;
            this.operationResponse = operationResponse;
        }

        public EnmOperationBuilder isAddedToOss(Boolean addedToOss) {
            this.addedToOss = addedToOss;
            return this;
        }

        public EnmOperationBuilder setValue(String setValue) {
            this.setValue = setValue;
            return this;
        }

        public EnmOperationBuilder getLdapDetails(String ldapDetails) {
            this.ldapDetails = ldapDetails;
            return this;
        }

        public EnmOperationBuilder getImportBackupStatus(String importBackupStatus) {
            this.importBackupStatus = importBackupStatus;
            return this;
        }

        public EnmOperationBuilder getImportBackupProgress(String importBackupProgress) {
            this.importBackupProgress = importBackupProgress;
            return this;
        }
    }

    @Override
    public String toString() {
        return String.format("operation: %s, operationResponse: %s, addedToOss: %s", this.operation, this.operationResponse, this.addedToOss);
    }
}
