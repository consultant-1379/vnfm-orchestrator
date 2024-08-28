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

import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.updateOperationState;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logLifecycleOperation;
import static com.ericsson.vnfm.orchestrator.utils.RequestNameEnum.FAIL_LCM_OPP;
import static com.ericsson.vnfm.orchestrator.utils.TaskUtils.prepareRecovery;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.Task;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.RollbackOperation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.RollbackOperationFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfLcmOpOccMapper;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.HelmChartUtils;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VnfLcmOperationServiceImpl implements VnfLcmOperationService {
    private static final String MAPPING_ERROR = "Exception while mapping lifecycleOperations to vnfLcmOppOcc: ";

    @Autowired
    private VnfLcmOpOccMapper vnfLcmOpOccMapper;

    @Autowired
    private LifecycleOperationQuery lifecycleOperationQuery;

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private RollbackOperationFactory rollbackOperationFactory;

    @Autowired
    private ChangedInfoRepository changedInfoRepository;

    @Autowired
    private CcvpPatternTransformer ccvpPatternTransformer;

    @Override
    public VnfLcmOpOcc getLcmOperationByOccId(String id) {
        LifecycleOperation lifecycleOperations = databaseInteractionService.getLifecycleOperation(id);
        LOGGER.debug("Lifecycle operation for the occurrence id {} is {}", id, logLifecycleOperation(lifecycleOperations));
        return getVnfLcmOpOcc(lifecycleOperations);
    }

    @Override
    public Page<LifecycleOperation> getAllLcmOperationsPage(String filter, Pageable pageable) {
        Page<LifecycleOperation> lifecycleOperationPage;
        if (Strings.isNullOrEmpty(filter)) {
            lifecycleOperationPage = databaseInteractionService.getAllOperations(pageable);
        } else {
            lifecycleOperationPage = lifecycleOperationQuery.getPageWithFilter(filter, pageable);
        }
        final List<LifecycleOperation> operations = lifecycleOperationPage.getContent();
        databaseInteractionService.initializeOperationParamsFieldInLifecycleOperations(operations);
        return lifecycleOperationPage;
    }

    private VnfLcmOpOcc getVnfLcmOpOcc(LifecycleOperation lifecycleOperation) {
        VnfLcmOpOcc vnfLcmOpOcc = null;

        if (lifecycleOperation != null) {
            final ChangedInfo changedInfo = changedInfoRepository.findById(lifecycleOperation.getOperationOccurrenceId()).orElse(null);
            try {
                vnfLcmOpOcc = vnfLcmOpOccMapper.toInternalModel(lifecycleOperation, changedInfo);
            } catch (final Exception e) {
                throw new InternalRuntimeException(MAPPING_ERROR, e);
            }
        }

        return vnfLcmOpOcc;
    }

    @Override
    public List<VnfLcmOpOcc> mapToVnfLcmOpOcc(Page<LifecycleOperation> lifecycleOperationList) {
        List<LifecycleOperation> lifecycleOperations = lifecycleOperationList.getContent();
        if (!CollectionUtils.isEmpty(lifecycleOperations)) {
            final List<String> operationOccurrenceIds = lifecycleOperations.stream()
                    .map(LifecycleOperation::getOperationOccurrenceId)
                    .collect(Collectors.toList());
            final List<ChangedInfo> changedInfos = changedInfoRepository.findAllById(operationOccurrenceIds);
            final Map<String, ChangedInfo> changedInfoMap = changedInfos.stream().collect(Collectors.toMap(ChangedInfo::getId, Function.identity()));
            try {
                return vnfLcmOpOccMapper.toInternalModel(lifecycleOperations, changedInfoMap);
            } catch (final Exception e) {
                throw new InternalRuntimeException(MAPPING_ERROR, e);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void rollbackLifecycleOperationByOccId(final String id) {
        LifecycleOperation lifecycleOperation = updateLifecycleOperation(id, LifecycleOperationState.ROLLING_BACK);
        lifeCycleManagementHelper.setExpiredTimeout(lifecycleOperation, lifecycleOperation.getApplicationTimeout());
        VnfInstance actualInstance = lifecycleOperation.getVnfInstance();
        VnfInstance tempInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        lifecycleOperation.setHelmClientVersion(actualInstance.getHelmClientVersion());
        HelmChart failedHelmChart = OperationsUtils.retrieveFailedHelmChart(tempInstance, lifecycleOperation);
        HelmChartUtils.setCompletedChartsStateToProcessing(tempInstance, lifecycleOperation);
        actualInstance.setTempInstance(convertObjToJsonString(tempInstance));

        final List<MutablePair<String, String>> helmChartCommandList =
                ccvpPatternTransformer.saveRollbackFailurePatternInOperationForOperationRollback(
                        actualInstance,
                        tempInstance,
                        lifecycleOperation,
                        failedHelmChart);

        RollbackOperation rollbackOperation = rollbackOperationFactory.getServiceByPattern(helmChartCommandList);
        rollbackOperation.execute(lifecycleOperation);
    }

    @Override
    public VnfLcmOpOcc failLifecycleOperationByOccId(final String id) {
        LifecycleOperation lifecycleOperation = updateLifecycleOperation(id, LifecycleOperationState.FAILED);
        databaseInteractionService.persistLifecycleOperation(lifecycleOperation);
        VnfInstance instance = lifecycleOperation.getVnfInstance();
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);

        List<Task> tasks = prepareRecovery(FAIL_LCM_OPP, instance);
        databaseInteractionService.saveTasksInNewTransaction(tasks);

        instanceService.updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(instance.getVnfPackageId(),
                                                                                         tempInstance.getVnfPackageId(),
                                                                                         tempInstance.getVnfPackageId(),
                                                                                         tempInstance.getVnfInstanceId(), false);

        databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(instance.getVnfInstanceId(), UPDATE_PACKAGE_STATE);

        final ChangedInfo changedInfo = changedInfoRepository.findById(lifecycleOperation.getOperationOccurrenceId()).orElse(null);
        VnfLcmOpOcc vnfLcmOpOcc;
        try {
            vnfLcmOpOcc = vnfLcmOpOccMapper.toInternalModel(lifecycleOperation, changedInfo);
        } catch (final Exception e) {
            throw new InternalRuntimeException(MAPPING_ERROR, e);
        }
        return vnfLcmOpOcc;
    }

    private LifecycleOperation updateLifecycleOperation(final String id,
                                                                     final LifecycleOperationState state) {
        LifecycleOperation lifecycleOperation = databaseInteractionService.getLifecycleOperation(id);
        if (lifecycleOperation == null) {
            throw new NotFoundException(String.format("The vnfLcmOpOccId-%s does not exist", id));
        }
        if (!lifecycleOperation.getOperationState().equals(LifecycleOperationState.FAILED_TEMP)) {
            throw new IllegalStateException(
                    "Operation state has to be in FAILED_TEMP in order to rollback/fail the operation");
        }
        if (!id.equals(lifecycleOperation.getVnfInstance().getOperationOccurrenceId())) {
            throw new IllegalStateException(
                    String.format("The requested operation %s is not the latest operation, can not perform request", id));
        }
        updateOperationState(lifecycleOperation, state);

        return lifecycleOperation;
    }
}
