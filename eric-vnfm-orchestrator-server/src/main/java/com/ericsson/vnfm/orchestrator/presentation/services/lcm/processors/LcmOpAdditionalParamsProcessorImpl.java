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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_MERGING_PREVIOUS_VALUES;
import static com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService.convertYamlStringAndSetToValuesYamlMap;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.ChangeVnfPackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.utils.OperationsUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.LifecycleRequestHandler;
import com.ericsson.vnfm.orchestrator.utils.AdditionalParamsUtils;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;
import com.ericsson.vnfm.orchestrator.utils.FileMerger;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LcmOpAdditionalParamsProcessorImpl implements LcmOpAdditionalParamsProcessor {

    @Autowired
    private ChangeVnfPackageService changeVnfPackageService;

    @Autowired
    private OperationsUtils operationsUtils;

    @Autowired
    private AdditionalParamsUtils additionalParamsUtils;

    @Autowired
    @Qualifier("yamlFileMerger")
    private FileMerger fileMerger;

    @Override
    public void process(final Map<String, Object> valuesYamlMap, final String targetVnfdId, final LifecycleOperation operation) {
        boolean isDowngrade = changeVnfPackageService.isDowngrade(targetVnfdId, operation);
        if (isDowngrade) {
            LOGGER.info("Processing additional params for Downgrade operation");
            List<LifecycleOperation> operations = getSeriesOfSelfUpgradeLifecycleOperationsForDowngrade(targetVnfdId, operation);
            mergeValuesForOperations(valuesYamlMap, operations);
        } else if (changeVnfPackageService.isSelfUpgrade(targetVnfdId, operation) && !isSkipMergingPreviousValues(operation)) {
            LOGGER.info("Processing additional params for Self Upgrade operation");
            List<LifecycleOperation> operations = getSeriesOfSelfUpgradeLifecycleOperations(targetVnfdId, operation);
            mergeValuesForOperations(valuesYamlMap, operations);
        }
    }

    @Override
    public Map<String, Object> processRaw(final VnfInstance vnfInstance) {
        Map<String, Object> additionalParamsFromLastOperation = operationsUtils.getAdditionalParamsFromLastOperation(vnfInstance, false);
        String additionalParamsYamlContent = additionalParamsUtils.mergeAdditionalParams(additionalParamsFromLastOperation);
        return convertYamlStringIntoMap(additionalParamsYamlContent);
    }

    private List<LifecycleOperation> getSeriesOfSelfUpgradeLifecycleOperationsForDowngrade(final String targetVnfdId, LifecycleOperation operation) {
        final LifecycleOperation upgradeOperation = changeVnfPackageService
                .getSuitableTargetDowngradeOperationFromOperation(targetVnfdId, operation);
        return getSeriesOfSelfUpgradeLifecycleOperations(targetVnfdId, upgradeOperation);
    }

    private List<LifecycleOperation> getSeriesOfSelfUpgradeLifecycleOperations(String targetVnfdId, LifecycleOperation startingOperation) {
        List<LifecycleOperation> selfUpgradesSeries = operationsUtils.getSelfUpgradesSeriesWhichBeginningFromOperation(
                targetVnfdId, startingOperation);
        if (selfUpgradesSeries.isEmpty()) {
            return selfUpgradesSeries;
        }
        int lastElementIndex = selfUpgradesSeries.size() - 1;
        final LifecycleOperation firstOperationInSeries = selfUpgradesSeries.get(lastElementIndex);
        if (changeVnfPackageService.isDowngrade(targetVnfdId, firstOperationInSeries)) {
            selfUpgradesSeries.remove(lastElementIndex);
            final List<LifecycleOperation> selfUpgradeSeriesForDowngrade =
                    getSeriesOfSelfUpgradeLifecycleOperationsForDowngrade(targetVnfdId, firstOperationInSeries);
            selfUpgradesSeries.addAll(selfUpgradeSeriesForDowngrade);
        }
        return selfUpgradesSeries;
    }

    private void mergeValuesForOperations(final Map<String, Object> valuesYamlMap,
                                          final List<LifecycleOperation> selfUpgradesSeries) {
        final List<String> additionalParamsToMerge = selfUpgradesSeries.stream()
                .map(operation -> operationsUtils.getAdditionalParamsFromOperation(operation.getVnfInstance(), operation))
                .map(YamlUtility::convertMapToYamlFormat)
                .collect(Collectors.toList());

        Collections.reverse(additionalParamsToMerge);
        String mergedAdditionalParamsYaml = fileMerger.merge(additionalParamsToMerge.toArray(new String[0]));
        convertYamlStringAndSetToValuesYamlMap(valuesYamlMap, mergedAdditionalParamsYaml);
    }

    private boolean isSkipMergingPreviousValues(final LifecycleOperation operation) {
        final Map<String, Object> additionalParamsFromOperation = operationsUtils.getAdditionalParamsFromOperation(operation.getVnfInstance(),
                                                                                                                   operation);
        return LifecycleRequestHandler.parameterPresent(additionalParamsFromOperation, SKIP_MERGING_PREVIOUS_VALUES)
                && BooleanUtils.getBooleanValue(additionalParamsFromOperation.get(SKIP_MERGING_PREVIOUS_VALUES));
    }
}
