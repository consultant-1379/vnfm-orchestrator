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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static com.ericsson.vnfm.orchestrator.messaging.MessageUtility.isDowngradeOperation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandler;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.MessageUtility;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartHistoryRecord;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.HelmChartHistoryService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.HelmChartMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
@AllArgsConstructor
public class DetermineUpgradeOperationType extends MessageHandler<HelmReleaseLifecycleMessage> {

    private final MessageUtility utility;
    private final HelmChartHistoryService helmChartHistoryService;
    private final InstanceService instanceService;
    private final ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;
    private final HelmChartMapper helmChartMapper;
    private final PackageService packageService;
    private final VnfdHelper vnfdHelper;

    @Override
    public void handle(MessageHandlingContext<HelmReleaseLifecycleMessage> context) {
        LOGGER.info("Determine upgrade operation type");
        final LifecycleOperation operation = context.getOperation();
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository
                .findById(operation.getOperationOccurrenceId());
        if (isDowngradeOperation(changePackageOperationDetails)) {
            LOGGER.info("Starting package downgrade operation");
            String targetOperationId = changePackageOperationDetails.get().getTargetOperationOccurrenceId();
            startRollbackFlowForDowngrade(context.getVnfInstance(), operation, targetOperationId);
            passToSuccessor(getAlternativeSuccessor(), context);
        } else {
            passToSuccessor(getSuccessor(), context);
        }
    }

    private void startRollbackFlowForDowngrade(final VnfInstance instance, final LifecycleOperation operation,
                                               final String targetOperationOccurrenceId) {
        final List<HelmChartHistoryRecord> helmChartHistoryRecords = helmChartHistoryService
                .getHelmChartHistoryRecordsByOperationId(targetOperationOccurrenceId);
        final VnfInstance tempInstance = setUpTempInstanceWithTargetPackageInfo(instance, operation.getTargetVnfdId());
        Map<String, HelmChart> chartsByReleaseName = instance.getHelmCharts()
                .stream()
                .collect(Collectors.toMap(HelmChart::getReleaseName, Function.identity()));
        tempInstance.setHelmCharts(helmChartHistoryRecords.stream()
                .map(helmChartMapper::toInternalModel)
                .peek(hc -> setAdditionalChartParams(chartsByReleaseName, hc))
                .peek(hc -> hc.setVnfInstance(tempInstance))
                .sorted(Comparator.comparing(HelmChart::getPriority))
                .collect(Collectors.toList()));
        instance.setTempInstance(Utility.convertObjToJsonString(tempInstance));
        final HelmChart helmChart = tempInstance.getHelmCharts()
                .stream()
                .filter(chart -> chart.getHelmChartType() != HelmChartType.CRD)
                .findFirst()
                .get();
        LOGGER.info("Starting Rollback operation for chart : {} ", helmChart.getHelmChartName());
        utility.triggerRollbackOperation(operation, helmChart.getReleaseName(), helmChart.getRevisionNumber(),
                instance);
    }

    private static void setAdditionalChartParams(Map<String, HelmChart> chartsByReleaseName, HelmChart hc) {
        HelmChart chartByReleaseName = chartsByReleaseName.get(hc.getReleaseName());
        hc.setId(chartByReleaseName.getId());
        hc.setHelmChartType(chartByReleaseName.getHelmChartType());
        hc.setHelmChartVersion(chartByReleaseName.getHelmChartVersion());

    }

    private VnfInstance setUpTempInstanceWithTargetPackageInfo(VnfInstance instance, String targetVnfdId) {
        final PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(targetVnfdId);
        final Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(new JSONObject(packageInfo.getDescriptorModel()));
        VnfInstance tempInstance = instanceService.createTempInstanceForUpgradeOperation(instance, packageInfo, policies);
        VnfInstance tempInstanceInInstance = Utility.parseJson(instance.getTempInstance(), VnfInstance.class);
        if (!Strings.isNullOrEmpty(tempInstanceInInstance.getVnfInfoModifiableAttributesExtensions())) {
            tempInstance.setVnfInfoModifiableAttributesExtensions(tempInstanceInInstance
                                                                      .getVnfInfoModifiableAttributesExtensions());
        }
        if (!Strings.isNullOrEmpty(tempInstanceInInstance.getInstantiationLevel())) {
            tempInstance.setInstantiationLevel(tempInstanceInInstance.getInstantiationLevel());
        }
        for (HelmChart chartTemp : tempInstanceInInstance.getHelmCharts()) {
            tempInstance.getHelmCharts().stream()
                .filter(chartCurrent -> chartTemp.getReleaseName().equals(chartCurrent.getReleaseName()))
                .findFirst()
                .ifPresent(chartCurrent -> chartCurrent.setReplicaDetails(chartTemp.getReplicaDetails()));
        }
        return tempInstance;
    }
}
