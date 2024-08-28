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

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.FTL_EXCEPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.MUTEX_ENM_OPERATION_DEDUPLICATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.ALARM_SET_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT_CONFIGURATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.LDAP_DETAILS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.ACTION_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE_REF;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE_REF_PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.services.InstanceService.getManagedElementId;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.CHECK_NODE;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.dontLogPasswords;
import static com.ericsson.vnfm.orchestrator.utils.SshResponseUtils.extractSpecificFailure;

import java.net.StandardProtocolFamily;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.notification.OperationState;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyContext;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.EnrollmentInfoService;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.RestoreBackupFromEnm;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OssNodeService {

    public static final String EXIT_STATUS = "exitStatus";
    public static final String COMMAND_OUTPUT = "commandOutput";
    @Value("${idempotency.eventsDedupExpirationSeconds}")
    private Integer eventsDedupExpirationSeconds;

    @Autowired
    private EnmTopologyService enmTopologyService;

    @Autowired
    private ObjectProvider<SshHelper> sshHelperProvider;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private RestoreBackupFromEnm restoreBackupFromEnm;

    @Autowired
    private EnrollmentInfoService enrollmentInfoService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EnmMetricsExposers enmMetricsExposers;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IdempotencyContext idempotencyContext;

    public void deleteNode(final VnfInstance vnfInstance, final Map<String, Object> topologyAttributes,
                           final Path file, final boolean sendNodeEvent) {
        SshHelper sshHelper = sshHelperProvider.getObject();
        SshResponse sshResponse = sshHelper.executeScript(file);
        checkStatus(vnfInstance, topologyAttributes, sshResponse, EnmOperationEnum.DELETE_NODE, sendNodeEvent);
    }

    public void addNode(final VnfInstance vnfInstance) {
        LOGGER.info("Adding Node to ENM");
        Map<String, Object> topologyAttributes = getOssTopologyParams(vnfInstance);
        try {
            Path addNodeScript = enmTopologyService.generateAddNodeScript(topologyAttributes);
            addNode(vnfInstance, topologyAttributes, addNodeScript);
            LOGGER.info("Node was successfully added to ENM");
        } catch (Exception e) {
            String errorMessage = String.format("Adding node to ENM failed for vnf [{%s}] with error: {%s}",
                                                vnfInstance.getVnfInstanceName(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new InternalRuntimeException(errorMessage);
        }
    }

    public void addNode(final VnfInstance vnfInstance, final Map<String, Object> topologyAttributes, final Path file) {
        enmMetricsExposers.getAddNodeMetric().increment();
        try {
            String enmOperationKey = buildRedisKey();
            SshResponse sshResponse = Optional.ofNullable(redisTemplate.opsForValue().get(enmOperationKey))
                    .map(response -> Utility.parseJson(response, SshResponse.class))
                    .orElseGet(() -> {
                        SshHelper sshHelper = sshHelperProvider.getObject();
                        SshResponse response = sshHelper.executeScript(file);

                        redisTemplate.opsForValue().setIfAbsent(enmOperationKey, Utility.convertObjToJsonString(response),
                                                                Duration.ofSeconds(eventsDedupExpirationSeconds));

                        return response;
                    });

            checkStatus(vnfInstance, topologyAttributes, sshResponse, EnmOperationEnum.ADD_NODE, true);
        } finally {
            enmMetricsExposers.getAddNodeMetric().decrement();
        }
    }

    public boolean checkNodePresent(final VnfInstance vnfInstance) {
        LOGGER.info("Checking if Node is managed by ENM");
        Map<String, Object> topologyAttributes = new JSONObject(vnfInstance.getAddNodeOssTopology()).toMap();
        try {
            Path checkNodeScript = enmTopologyService.generateCheckNodePresentScript(topologyAttributes);
            SshHelper sshHelper = sshHelperProvider.getObject();
            SshResponse sshResponse = sshHelper.executeScript(checkNodeScript);
            return isNodePresent(vnfInstance, topologyAttributes, sshResponse);
        } catch (Exception e) {
            String errorMessage = String.format("Checking if node is present in ENM failed for vnf [{%s}] with error: {%s}",
                                                vnfInstance.getVnfInstanceName(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new InternalRuntimeException(errorMessage);
        }
    }

    public String generateOssNodeProtocolFile(VnfInstance vnfInstance, StandardProtocolFamily ipVersion) {
        final Map<String, String> enrollmentDetails = enrollmentInfoService.getEnrollmentInfoFromENM(vnfInstance);
        String enrollmentFile = enrollmentDetails.get(ENROLLMENT_CONFIGURATION);
        String ldapDetails = enrollmentDetails.get(LDAP_DETAILS);
        return enmTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails, ipVersion);
    }

    public String generateLDAPServerConfiguration(VnfInstance vnfInstance, StandardProtocolFamily ipVersion) {
        final Map<String, String> enrollmentDetails = enrollmentInfoService.getEnrollmentInfoFromENM(vnfInstance);
        String ldapDetails = enrollmentDetails.get(LDAP_DETAILS);
        return enmTopologyService.generateLdapConfigurationJSONString(ldapDetails, ipVersion);
    }

    public String generateCertificateEnrollmentConfiguration(VnfInstance vnfInstance) {
        final Map<String, String> enrollmentDetails = enrollmentInfoService.getEnrollmentInfoFromENM(vnfInstance);
        String enrollmentConfig = enrollmentDetails.get(ENROLLMENT_CONFIGURATION);
        return enmTopologyService.generateEnrollmentConfigurationJSONString(enrollmentConfig);
    }

    public void deleteNodeFromENM(VnfInstance vnfInstance, boolean sendNodeEvent) {
        LOGGER.info("Deleting Node from ENM");
        Map<String, Object> topologyAttributes = getCommonScriptAttributes(vnfInstance, EnmOperationEnum.DELETE_NODE);
        Path deleteNodeScript = enmTopologyService.generateDeleteNodeScript(topologyAttributes);
        deleteNode(vnfInstance, topologyAttributes, deleteNodeScript, sendNodeEvent);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

        // Will be deleted only in execution of deleteNode flow, will not impact task execution
        if (sendNodeEvent) {
            databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstance.getVnfInstanceId(), DELETE_NODE);
        }
    }

    @Async
    public CompletableFuture<Boolean> restoreAsyncBackupFromENM(VnfInstance vnfInstance, LifecycleOperation operation, String backupFileRef,
                                                                String password) {
        Map<String, Object> topologyAttributes = getRestoreScriptAttributes(vnfInstance, backupFileRef, password);
        return CompletableFuture.completedFuture(restoreBackupFromEnm.restoreBackup(operation, topologyAttributes));
    }

    public void restoreBackupFromENM(VnfInstance vnfInstance, String backupFileRef, String password) {
        Map<String, Object> topologyAttributes = getRestoreScriptAttributes(vnfInstance, backupFileRef, password);
        restoreBackupFromEnm.restoreLatestBackup(topologyAttributes);
    }

    public void enableSupervisionsInENM(VnfInstance vnfInstance) {
        LOGGER.info("Enabling supervision");
        Map<String, Object> topologyAttributes = getOssTopologyParams(vnfInstance);
        Path setAlarmScript = enmTopologyService.generateEnableSupervisionsScript(topologyAttributes);
        enableSupervision(vnfInstance, topologyAttributes, setAlarmScript);
    }

    public void enableSupervisionsInENM(VnfInstance vnfInstance, Map<String, Object> topologyAttributes) {
        LOGGER.info("Enabling supervision");
        Path setAlarmScript = enmTopologyService.generateEnableSupervisionsScript(topologyAttributes);
        enableSupervision(vnfInstance, topologyAttributes, setAlarmScript);
    }

    public void setAlarmSuperVisionInENM(VnfInstance vnfInstance, EnmOperationEnum operationEnum) {
        LOGGER.info("Setting alarm supervision");
        if (checkAlarmStatus(vnfInstance, operationEnum)) {
            Map<String, Object> topologyAttributes = getCommonScriptAttributes(vnfInstance, operationEnum);
            topologyAttributes.put(ALARM_SET_VALUE, operationEnum.getSetValue());
            Path setAlarmScript = enmTopologyService.generateSetAlarmScript(topologyAttributes, operationEnum);
            setAlarm(vnfInstance, topologyAttributes, setAlarmScript, operationEnum);
            instanceService.setAlarmStatusOnInstance(vnfInstance, operationEnum);
        } else {
            LOGGER.warn("Skipping setting alarm supervision to {}. Resource is not added to ENM or alarm status is already turned {}",
                        operationEnum.getSetValue(), operationEnum.getSetValue());
        }
    }

    public static Map<String, Object> getCommonScriptAttributes(VnfInstance vnfInstance, EnmOperationEnum operation) {
        String managedElementId = getManagedElementId(vnfInstance);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(MANAGED_ELEMENT_ID, managedElementId);
        attributes.put(OPERATION, operation.getOperation());
        attributes.put(OPERATION_RESPONSE, operation.getOperationResponse());
        return attributes;
    }

    public Map<String, Object> getRestoreScriptAttributes(VnfInstance vnfInstance, String backupFileRef, String password) {
        Map<String, Object> topologyAttributes = getCommonScriptAttributes(vnfInstance, EnmOperationEnum.RESTORE_BACKUP);
        topologyAttributes.put(BACKUP_FILE_REF, backupFileRef);
        topologyAttributes.put(BACKUP_FILE_REF_PASSWORD, password);
        topologyAttributes.put(ACTION_ID, "");
        topologyAttributes.put(BACKUP_FILE, "");
        return topologyAttributes;
    }

    public Map<String, Object> getOssTopologyParams(VnfInstance vnfInstance) {
        Optional<String> addNodeOssTopology = Optional.ofNullable(vnfInstance.getInstantiateOssTopology());
        if (addNodeOssTopology.isPresent()) {
            return instanceService.extractOssTopologyFromParams(vnfInstance);
        } else {
            throw new IllegalArgumentException("OSS topology parameters not found in request");
        }
    }

    private void enableSupervision(final VnfInstance vnfInstance,
                                   final Map<String, Object> topologyAttributes,
                                   final Path file) {
        try {
            SshResponse sshResponse = sshHelperProvider.getObject().executeScript(file);
            checkStatus(vnfInstance, topologyAttributes, sshResponse, EnmOperationEnum.ENABLE_SUPERVISION, true);
        } catch (Exception exception) {
            instanceService.persistAlarmErrorMessage(vnfInstance, EnmOperationEnum.ENABLE_SUPERVISION, exception.getMessage());
            throw exception;
        }
    }

    private void setAlarm(final VnfInstance vnfInstance,
                          final Map<String, Object> topologyAttributes,
                          final Path file,
                          final EnmOperationEnum operationEnum) {
        try {
            SshHelper sshHelper = sshHelperProvider.getObject();
            SshResponse sshResponse = sshHelper.executeScript(file);
            checkStatus(vnfInstance, topologyAttributes, sshResponse, operationEnum, true);
        } catch (Exception exception) {
            instanceService.persistAlarmErrorMessage(vnfInstance, operationEnum, exception.getMessage());
            throw exception;
        }
    }

    private static boolean checkAlarmStatus(VnfInstance vnfInstance, EnmOperationEnum operationEnum) {
        if (vnfInstance.isAddedToOss()) {
            if (vnfInstance.getAlarmSupervisionStatus() == null) {
                return true;
            }
            return !vnfInstance.getAlarmSupervisionStatus().equals(operationEnum.getSetValue());
        }
        return false;
    }

    private boolean isNodePresent(final VnfInstance vnfInstance, final Map<String, Object> topologyAttributes,
                                  final SshResponse sshResponse) {
        checkStatus(vnfInstance, topologyAttributes, sshResponse, CHECK_NODE, false);
        JSONObject response = new JSONObject(sshResponse.getOutput());
        JSONObject checkCommandResponse = response.getJSONObject(CHECK_NODE.getOperationResponse());
        return checkCommandResponse.getString(COMMAND_OUTPUT).contains("1 instance(s)");
    }

    private void checkStatus(final VnfInstance vnfInstance, final Map<String, Object> topologyAttributes,
                             final SshResponse sshResponse, final EnmOperationEnum operation, final boolean sendNodeEvent) {
        LOGGER.info("Command completed with status {}", sshResponse.getExitStatus());
        if (sshResponse.getExitStatus() == 0) {
            instanceService.addCommandResultToInstance(vnfInstance, operation);
            if (sendNodeEvent) {
                notificationService.sendNodeEvent(vnfInstance.getVnfInstanceId(), OperationState.COMPLETED, operation);
                databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstance.getVnfInstanceId(), SEND_NOTIFICATION);
            }
        } else {
            Map<String, Object> fullSetOfAttributes = topologyAttributes;
            if (operation.equals(EnmOperationEnum.ADD_NODE)) {
                fullSetOfAttributes = enmTopologyService.getFullSetOfAttributes(topologyAttributes);
            }
            if (sendNodeEvent) {
                notificationService.sendNodeEvent(vnfInstance.getVnfInstanceId(), OperationState.FAILED, operation);
                databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstance.getVnfInstanceId(), SEND_NOTIFICATION);
            }
            String specificErrorMessage = extractSpecificFailure(sshResponse, operation);
            throw new FileExecutionException(String.format(FTL_EXCEPTION, operation.getOperation(), dontLogPasswords(fullSetOfAttributes),
                                                           specificErrorMessage));
        }
    }

    private String buildRedisKey() {
        String enmOperationId = idempotencyContext.getIdempotencyId().orElseGet(() -> UUID.randomUUID().toString());
        return MUTEX_ENM_OPERATION_DEDUPLICATION.formatted(enmOperationId);
    }
}
