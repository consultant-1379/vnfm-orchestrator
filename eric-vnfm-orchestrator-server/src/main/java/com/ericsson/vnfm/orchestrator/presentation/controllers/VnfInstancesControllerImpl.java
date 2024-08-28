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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import static org.slf4j.LoggerFactory.getLogger;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.LIFECYCLE_OPERATION_TYPE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.MDC.VNF_INSTANCE_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_WITHOUT_LICENSE_ATTRIBUTE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.DEFAULT_PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.BACKUP_OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.BACKUP_OPERATION_PERFORMED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_PERFORMED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_INSTANTIATED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.VNF_OPERATION_PERFORMED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfInstances.SORT_COLUMNS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfInstances.VNF_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkAddedToOss;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.checkVnfNotInState;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildPaginationInfo;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.CREATE_BACKUP;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.CREATE_VNF_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.DELETE_BACKUP;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.DELETE_VNF_IDENTIFIER;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareOssNodeRecovery;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareRecovery;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.createPaginationHttpHeaders;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.getHttpHeaders;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkPackageOperationalState;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.validateYamlFileAndConvertToMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.api.VnfInstanceOperationsApi;
import com.ericsson.vnfm.orchestrator.api.VnfInstanceOperationsWithValuesApi;
import com.ericsson.vnfm.orchestrator.model.AddNodeToVnfInstanceByIdRequest;
import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ChangePackageInfoVnfRequest;
import com.ericsson.vnfm.orchestrator.model.CleanupVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.CreateBackupsRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.SyncVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModificationRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.OrchestratorLimitsCalculator;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vnflcm/v1")
public class VnfInstancesControllerImpl implements VnfInstanceOperationsApi, VnfInstanceOperationsWithValuesApi {

    private static final Logger LOGGER = getLogger(VnfInstancesControllerImpl.class);

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private LifeCycleManagementService lifeCycleManagementService;

    @Autowired
    private EnmTopologyService enmTopologyService;

    @Autowired
    private OssNodeService ossNodeService;

    @Autowired
    private BackupsService backupsService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private UsernameCalculationService usernameCalculationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WorkflowRoutingService workflowRoutingService;

    @Autowired
    private VnfInstanceMapper vnfInstanceMapper;

    @Autowired
    private PackageService packageService;

    @Autowired
    private VnfdHelper vnfdHelper;

    @Autowired
    private OrchestratorLimitsCalculator orchestratorLimitsCalculator;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private IdempotencyService idempotencyService;

    @Value("${enm.scripting.cluster.ssh.connection.timeout}")
    private int connectionToEnmTimeout;

    @Value("${enm.scripting.cluster.ssh.connection.retry}")
    private int connectionToEnmRetry;

    @Value("${enm.scripting.cluster.ssh.connection.delay}")
    private int connectionToEnmDelay;

    @Override
    public ResponseEntity<VnfInstanceResponse> getVnfInstanceById(final String accept, final String vnfInstanceId) {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

        final ComponentStatusResponse componentStatusRequest = workflowRoutingService.getComponentStatusRequest(vnfInstance);
        VnfInstanceResponse response = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstance, componentStatusRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<VnfInstanceResponse>> getAllVnfInstances(final String accept,
                                                                        final String filter,
                                                                        final String nextpageOpaqueMarker,
                                                                        final Integer size,
                                                                        final List<String> sort,
                                                                        final String type) {

        Pageable pageable = new PaginationUtils.PageableBuilder()
                .defaults(DEFAULT_PAGE_SIZE, VNF_INSTANCE_NAME)
                .page(nextpageOpaqueMarker).size(size).sort(sort, SORT_COLUMNS)
                .build();
        Page<VnfInstance> vnfInstancePage = instanceService.getVnfInstancePage(filter, pageable);

        PaginationInfo paginationInfo = buildPaginationInfo(vnfInstancePage);
        HttpHeaders httpHeaders = createPaginationHttpHeaders(paginationInfo);

        List<VnfInstance> vnfInstances = vnfInstancePage.getContent();
        List<ComponentStatusResponse> componentStatusRequest = workflowRoutingService.getComponentStatusRequest(vnfInstances);
        List<VnfInstanceResponse> vnfInstanceResponses = vnfInstanceMapper.toOutputModelWithResourceInfo(vnfInstances, componentStatusRequest);

        return new ResponseEntity<>(vnfInstanceResponses, httpHeaders, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<VnfInstanceResponse> createVnfInstance(final String accept, final String contentType,
                                                                 final String idempotencyKey,
                                                                 final CreateVnfRequest createVnfRequest) {

        Supplier<ResponseEntity<VnfInstanceResponse>> createVnfInstanceSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(OPERATION_PERFORMED_TEXT, "Create VNF Instance", "name", createVnfRequest.getVnfInstanceName(), requestUsername);

            final PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(createVnfRequest.getVnfdId());
            checkPackageOperationalState(packageInfo);
            final Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(packageInfo.getDescriptorModel()));

            final VnfInstance collectedVnfInstanceToSave = instanceService.createVnfInstanceEntity(packageInfo, createVnfRequest, policies);
            orchestratorLimitsCalculator.checkOrchestratorLimitsForInstances(httpServletRequest.getAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE));
            final VnfInstance savedVnfInstance = databaseInteractionService.saveVnfInstanceToDB(collectedVnfInstanceToSave);

            List<Task> tasks = prepareRecovery(CREATE_VNF_IDENTIFIER, savedVnfInstance);
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            instanceService.createAndSaveAssociationBetweenPackageAndVnfInstance(collectedVnfInstanceToSave.getVnfPackageId(),
                                                                                 collectedVnfInstanceToSave.getVnfInstanceId(), true);

            final VnfInstanceResponse vnfInstanceResponse = vnfInstanceMapper.toOutputModel(savedVnfInstance);

            notificationService.sendVnfIdentifierCreationEvent(collectedVnfInstanceToSave.getVnfInstanceId());

            databaseInteractionService.deleteTasks(tasks);

            LOGGER.info(OPERATION_IS_FINISHED_TEXT, "Create VNF Instance", "name", createVnfRequest.getVnfInstanceName());

            return new ResponseEntity<>(vnfInstanceResponse, HttpStatus.CREATED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(createVnfInstanceSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> addNodeToVnfInstanceById(final String accept,
                                                         final String contentType,
                                                         final String idempotencyKey,
                                                         final String vnfInstanceId,
                                                         AddNodeToVnfInstanceByIdRequest request) {

        Supplier<ResponseEntity<Void>> addNodeSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Add Node", vnfInstanceId, requestUsername);

            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);

            Map<String, Object> topologyAttributes = instanceService
                    .extractOssTopologyFromParams(vnfInstance, request);

            List<Task> tasks = prepareOssNodeRecovery(ADD_NODE, vnfInstance, topologyAttributes, getPerformAtTime());
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            Path addNodeScript = enmTopologyService.generateAddNodeScript(topologyAttributes);

            ossNodeService.addNode(vnfInstance, topologyAttributes, addNodeScript);
            ossNodeService.enableSupervisionsInENM(vnfInstance, topologyAttributes);

            databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

            databaseInteractionService.deleteTasks(tasks);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Add Node", vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.OK);
        };

        return idempotencyService.executeTransactionalIdempotentCall(addNodeSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> addNodeToVnfInstanceById(final String accept,
                                                         final String contentType,
                                                         final String idempotencyKey,
                                                         final String vnfInstanceId,
                                                         final MultipartFile values) {
        Supplier<ResponseEntity<Void>> addNodeSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Add Node", vnfInstanceId, requestUsername);

            Map<String, Object> valuesYamlMap = validateYamlFileAndConvertToMap(values);

            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);

            Map<String, Object> topologyAttributes = instanceService.extractOssTopologyFromValuesYamlMap(vnfInstance, valuesYamlMap);

            List<Task> tasks = prepareOssNodeRecovery(ADD_NODE, vnfInstance, topologyAttributes, getPerformAtTime());
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            Path addNodeScript = enmTopologyService.generateAddNodeScript(topologyAttributes);

            ossNodeService.addNode(vnfInstance, topologyAttributes, addNodeScript);
            ossNodeService.enableSupervisionsInENM(vnfInstance, topologyAttributes);

            databaseInteractionService.saveVnfInstanceToDB(vnfInstance);

            databaseInteractionService.deleteTasks(tasks);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Add Node", vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.OK);
        };

        return idempotencyService.executeTransactionalIdempotentCall(addNodeSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> deleteVnfInstanceById(final String accept,
                                                      final String vnfInstanceId,
                                                      final String idempotencyKey) {

        Supplier<ResponseEntity<Void>> deleteVnfInstanceSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Delete VNF Instance", vnfInstanceId, requestUsername);

            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            List<Task> tasks = prepareRecovery(DELETE_VNF_IDENTIFIER, vnfInstance);
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            instanceService.deleteInstanceEntity(vnfInstanceId, true);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Delete VNF Instance", vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deleteVnfInstanceSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> deleteNodeFromVnfInstanceById(final String accept,
                                                              final String idempotencyKey,
                                                              final String vnfInstanceId) {

        Supplier<ResponseEntity<Void>> deleteNodeSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Delete Node", vnfInstanceId, requestUsername);

            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            checkVnfNotInState(vnfInstance, InstantiationState.NOT_INSTANTIATED);
            checkAddedToOss(vnfInstance);

            List<Task> tasks = prepareOssNodeRecovery(DELETE_NODE, vnfInstance, Collections.emptyMap(), getPerformAtTime());
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            ossNodeService.deleteNodeFromENM(vnfInstance, true);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Delete Node", vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.OK);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deleteNodeSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> instantiateVnfInstance(final String vnfInstanceId,
                                                       final String accept,
                                                       final String contentType,
                                                       final String idempotencyKey,
                                                       final InstantiateVnfRequest instantiateVnfRequest) {

        Supplier<ResponseEntity<Void>> instantiateVnfInstanceSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.INSTANTIATE.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, VNF_OPERATION_INSTANTIATED_TEXT, vnfInstanceId, requestUsername);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, instantiateVnfRequest, requestUsername, new HashMap<>());
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(instantiateVnfInstanceSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> instantiateVnfInstance(final String vnfInstanceId,
                                                       final String accept,
                                                       final String contentType,
                                                       final String idempotencyKey,
                                                       final MultipartFile values,
                                                       final InstantiateVnfRequest instantiateVnfRequest) {
        Supplier<ResponseEntity<Void>> instantiateVnfInstanceSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.INSTANTIATE.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, VNF_OPERATION_INSTANTIATED_TEXT, vnfInstanceId, requestUsername);

            Map<String, Object> valuesYamlMap = validateYamlFileAndConvertToMap(values);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.INSTANTIATE, vnfInstanceId, instantiateVnfRequest, requestUsername, valuesYamlMap);
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(instantiateVnfInstanceSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> scaleVnfInstanceById(final String vnfInstanceId, final String accept,
                                                     final String contentType, final String idempotencyKey,
                                                     final ScaleVnfRequest scaleVnfRequest) {
        Supplier<ResponseEntity<Void>> scaleVnfInstanceSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.SCALE.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Scale VNF Instance", vnfInstanceId, requestUsername);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.SCALE, vnfInstanceId, scaleVnfRequest, requestUsername, new HashMap<>());
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(scaleVnfInstanceSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> syncVnfInstanceById(final String vnfInstanceId,
                                                    final String idempotencyKey,
                                                    final SyncVnfRequest syncVnfRequest) {

        Supplier<ResponseEntity<Void>> scaleVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.SYNC.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Sync VNF Instance", vnfInstanceId, requestUsername);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.SYNC, vnfInstanceId, syncVnfRequest, requestUsername, new HashMap<>());

            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);
            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(scaleVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Object> getBackupScopesForVnfInstanceById(String vnfInstanceId, final String accept) {
        List<String> scopes = backupsService.getScopes(vnfInstanceId);

        return new ResponseEntity<>(scopes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> backupVnfInstanceById(String vnfInstanceId, String accept,
                                                      String contentType, String idempotencyKey,
                                                      CreateBackupsRequest createBackupsRequest) {
        Supplier<ResponseEntity<Void>> createBackupVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Backup VNF Instance", vnfInstanceId, requestUsername);

            List<Task> tasks = prepareRecovery(CREATE_BACKUP, VnfInstance.builder().vnfInstanceId(vnfInstanceId).build());
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            backupsService.createBackup(createBackupsRequest, vnfInstanceId);

            databaseInteractionService.deleteTasks(tasks);

            LOGGER.info(VNF_OPERATION_IS_FINISHED_TEXT, "Backup VNF Instance", vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(createBackupVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> deleteBackupForVnfInstanceByInstanceIdAndBackupNameAndScope(String vnfInstanceId,
                                                                                            String backupName,
                                                                                            String scope,
                                                                                            String idempotencyKey) {

        Supplier<ResponseEntity<Void>> deleteBackupSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(BACKUP_OPERATION_PERFORMED_TEXT, backupName, vnfInstanceId, requestUsername);

            List<Task> tasks = prepareRecovery(DELETE_BACKUP, VnfInstance.builder().vnfInstanceId(vnfInstanceId).build());
            databaseInteractionService.saveTasksInNewTransaction(tasks);

            backupsService.deleteBackup(vnfInstanceId, scope, backupName);

            databaseInteractionService.deleteTasks(tasks);

            LOGGER.info(BACKUP_OPERATION_IS_FINISHED_TEXT, backupName, vnfInstanceId);

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deleteBackupSupplier, idempotencyKey);
    }

    public ResponseEntity<List<BackupsResponseDto>> getAllBackupsForVnfInstanceById(final String vnfInstanceId, final String accept) {
        List<BackupsResponseDto> backups = new ArrayList<>(backupsService.getAllBackups(vnfInstanceId));
        return new ResponseEntity<>(backups, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> terminateVnfInstanceById(final String vnfInstanceId,
                                                         final String accept,
                                                         final String contentType,
                                                         final String idempotencyKey,
                                                         final TerminateVnfRequest terminateVnfRequest) {

        Supplier<ResponseEntity<Void>> terminateVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.TERMINATE.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, LifecycleOperationType.TERMINATE, vnfInstanceId, requestUsername);

            if (terminateVnfRequest.getTerminationType() == TerminateVnfRequest.TerminationTypeEnum.GRACEFUL) {
                LOGGER.info("Accept GRACEFUL termination and handle as FORCEFUL termination for vnfInstanceId: {}", vnfInstanceId);
                terminateVnfRequest.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
            }

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.TERMINATE, vnfInstanceId, terminateVnfRequest, requestUsername, new HashMap<>());
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(terminateVnfSupplier, idempotencyKey);
    }

    /**
     * @deprecated use changeVnfPkgForVnfInstanceById instead
     */
    @SuppressWarnings("squid:S1133")
    @Override
    @Deprecated(since = "Use changeVnfPkgForVnfInstanceById instead")
    public ResponseEntity<Void> changeVnfInstancePackageInfoById(final String vnfInstanceId,
                                                                 final String accept,
                                                                 final String contentType,
                                                                 final String idempotencyKey,
                                                                 final ChangePackageInfoVnfRequest changePackageInfoVnfRequest) {

        Supplier<ResponseEntity<Void>> changeVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.CHANGE_VNFPKG.toString());

            ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
            changeCurrentVnfPkgRequest.setVnfdId(changePackageInfoVnfRequest.getVnfdId());
            changeCurrentVnfPkgRequest.setAdditionalParams(changePackageInfoVnfRequest.getAdditionalParams());

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.CHANGE_VNFPKG,
                                    vnfInstanceId,
                                    changeCurrentVnfPkgRequest,
                                    requestUsername,
                                    new HashMap<>());

            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(changeVnfSupplier, idempotencyKey);
    }

    /**
     * @deprecated use changeVnfPkgForVnfInstanceById instead
     */
    @SuppressWarnings("squid:S1133")
    @Override
    @Deprecated(since = "Use changeVnfPkgForVnfInstanceById instead")
    public ResponseEntity<Void> changeVnfInstancePackageInfoById(final String vnfInstanceId,
                                                                 final String accept,
                                                                 final String contentType,
                                                                 final String idempotencyKey,
                                                                 final MultipartFile values,
                                                                 final ChangePackageInfoVnfRequest changePackageInfoVnfRequest) {

        Supplier<ResponseEntity<Void>> changeVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.CHANGE_VNFPKG.toString());

            ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest = new ChangeCurrentVnfPkgRequest();
            changeCurrentVnfPkgRequest.setVnfdId(changePackageInfoVnfRequest.getVnfdId());
            changeCurrentVnfPkgRequest.setAdditionalParams(changePackageInfoVnfRequest.getAdditionalParams());

            Map<String, Object> valuesYamlMap = validateYamlFileAndConvertToMap(values);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfInstanceId, changeCurrentVnfPkgRequest, requestUsername, valuesYamlMap);
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(changeVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> changeVnfPkgForVnfInstanceById(final String vnfInstanceId,
                                                               final String accept,
                                                               final String contentType,
                                                               final String idempotencyKey,
                                                               final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest) {

        Supplier<ResponseEntity<Void>> changeVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.CHANGE_VNFPKG.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Change VNF Package", vnfInstanceId, requestUsername);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.CHANGE_VNFPKG,
                                    vnfInstanceId,
                                    changeCurrentVnfPkgRequest,
                                    requestUsername,
                                    new HashMap<>());
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(changeVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> changeVnfPkgForVnfInstanceById(final String vnfInstanceId,
                                                               final String accept,
                                                               final String contentType,
                                                               final String idempotencyKey,
                                                               final MultipartFile values,
                                                               final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest) {
        Supplier<ResponseEntity<Void>> changeVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.CHANGE_VNFPKG.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Change VNF Package", vnfInstanceId, requestUsername);

            Map<String, Object> valuesYamlMap = validateYamlFileAndConvertToMap(values);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.CHANGE_VNFPKG, vnfInstanceId, changeCurrentVnfPkgRequest, requestUsername, valuesYamlMap);
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(changeVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> healVnfInstanceById(final String vnfInstanceId,
                                                    final String accept,
                                                    final String contentType,
                                                    final String idempotencyKey,
                                                    final HealVnfRequest healVnfRequest) {

        Supplier<ResponseEntity<Void>> healVnfSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.HEAL.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Heal VNF Instance", vnfInstanceId, requestUsername);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                    .executeRequest(LifecycleOperationType.HEAL, vnfInstanceId, healVnfRequest, requestUsername, new HashMap<>());
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(healVnfSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> patchVnfInstanceById(final String accept,
                                                     final String vnfInstanceId,
                                                     final String contentType,
                                                     final VnfInfoModificationRequest vnfInfoModificationRequest) {

        String requestUsername = usernameCalculationService.calculateUsername();
        MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
        MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.MODIFY_INFO.toString());
        LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Modify VNF Instance", vnfInstanceId, requestUsername);

        String lifeCycleOperationOccurrenceId = lifeCycleManagementService
                .executeRequest(LifecycleOperationType.MODIFY_INFO, vnfInstanceId, vnfInfoModificationRequest, requestUsername, new HashMap<>());
        HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

        return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> cleanUpResourcesForVnfInstanceById(final String contentType,
                                                                   final String idempotencyKey,
                                                                   final String vnfInstanceId,
                                                                   final String accept,
                                                                   CleanupVnfRequest cleanupVnfRequest) {

        Supplier<ResponseEntity<Void>> cleanupResourcesSupplier = () -> {

            String requestUsername = usernameCalculationService.calculateUsername();
            MDC.put(VNF_INSTANCE_KEY, vnfInstanceId);
            MDC.put(LIFECYCLE_OPERATION_TYPE_KEY, LifecycleOperationType.TERMINATE.toString());
            LOGGER.info(VNF_OPERATION_PERFORMED_TEXT, "Cleanup", vnfInstanceId, requestUsername);

            VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);

            String lifeCycleOperationOccurrenceId = lifeCycleManagementService.cleanup(vnfInstance, cleanupVnfRequest, requestUsername);
            HttpHeaders headers = getHttpHeaders(lifeCycleOperationOccurrenceId);

            return new ResponseEntity<>(null, headers, HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(cleanupResourcesSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Object> getValuesForVnfInstanceById(final String vnfInstanceId, final String accept) {

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Path yamlFile = instanceService.getCombinedAdditionalValuesWithoutEVNFMParams(vnfInstance);
        if (yamlFile == null) {
            throw new NotFoundException("No values found for Vnf instance with id " + vnfInstanceId);
        }
        try {
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(yamlFile));
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(MediaType.TEXT_PLAIN.toString()))
                    .body(resource);
        } catch (IOException ioe) {
            throw new InternalRuntimeException("Error during creating values file", ioe);
        } finally {
            deleteFile(yamlFile);
        }
    }

    private LocalDateTime getPerformAtTime() {
        return LocalDateTime.now().plus((long) connectionToEnmRetry * (connectionToEnmTimeout + connectionToEnmDelay), ChronoUnit.MILLIS);
    }
}
