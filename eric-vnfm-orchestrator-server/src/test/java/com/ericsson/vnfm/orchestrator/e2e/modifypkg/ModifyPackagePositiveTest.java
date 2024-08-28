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
package com.ericsson.vnfm.orchestrator.e2e.modifypkg;

import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static com.ericsson.vnfm.orchestrator.TestUtils.CL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.JL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.PL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.TL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkDefaultExtensionsAreSet;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkLevelsReplicaDetailsNull;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;


public class ModifyPackagePositiveTest extends AbstractEndToEndTest {

    @Autowired
    private PackageService packageService;

    @Autowired
    private ChangedInfoRepository changedInfoRepository;

    @Test
    @SuppressWarnings("unchecked")
    public void successfulInstantiateMultipleModifyVnfWithMultipleHelmChart() throws Exception {
        //Create Identifier
        final String modifiedReleaseName = "end-to-end-default-level-extensions-modify";
        final String releaseName = "end-to-end-first-release-name";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        // Check that default extensions set correctly
        checkDefaultExtensionsAreSet(instance);

        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("tenant", "ECM");

        String vnfDescription = "modified description";
        String packageId = E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;

        //  Modify all attributes before instantiation
        MvcResult result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                                     extensions,
                                                                                                     packageId,
                                                                                                     modifiedReleaseName,
                                                                                                     metadata,
                                                                                                     vnfDescription);
        String lifeCycleOperationId = getLifeCycleOperationId(result);
        LifecycleOperation modifyOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        PackageResponse packageResponse = packageService.getPackageInfo(E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getVnfPackageId()).isEqualTo(packageId);
        assertThat(instance.getVnfDescriptorId()).isEqualTo(packageResponse.getVnfdId());
        assertThat(instance.getVnfInstanceDescription()).isEqualTo(vnfDescription);
        assertThat(instance.getMetadata()).isEqualTo(convertObjToJsonString(metadata));
        assertThat(instance.getVnfInstanceName()).isEqualTo(modifiedReleaseName);
        assertThat(instance.getTempInstance()).isNull();
        Map<String, Object> vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(instance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling).containsAllEntriesOf((Map<? extends String, ?>) extensions.get(VNF_CONTROLLED_SCALING));

        // Verify changed info was set correctly
        ChangedInfo changedInfoBeforeInstantiation = changedInfoRepository.findById(modifyOperation.getOperationOccurrenceId()).orElse(null);
        assertThat(changedInfoBeforeInstantiation).isNotNull();
        assertThat(modifyOperation.getVnfInfoModifiableAttributesExtensions()).isEqualTo(instance.getVnfInfoModifiableAttributesExtensions());
        assertThat(changedInfoBeforeInstantiation.getVnfPkgId()).isEqualTo(packageId);
        assertThat(changedInfoBeforeInstantiation.getVnfInstanceDescription()).isEqualTo(vnfDescription);
        assertThat(changedInfoBeforeInstantiation.getMetadata()).isEqualTo(convertObjToJsonString(metadata));
        assertThat(changedInfoBeforeInstantiation.getVnfInstanceName()).isEqualTo(modifiedReleaseName);
        assertThat(changedInfoBeforeInstantiation.getVnfDescriptorId()).isEqualTo(packageResponse.getVnfdId());
        assertThat(changedInfoBeforeInstantiation.getVnfProductName()).isEqualTo(packageResponse.getVnfProductName());
        assertThat(changedInfoBeforeInstantiation.getVnfdVersion()).isEqualTo(packageResponse.getVnfdVersion());
        assertThat(changedInfoBeforeInstantiation.getVnfProviderName()).isEqualTo(packageResponse.getVnfProvider());
        assertThat(changedInfoBeforeInstantiation.getVnfSoftwareVersion()).isEqualTo(packageResponse.getVnfSoftwareVersion());

        //Instantiate
        result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                  "end-to-end-default-level-namespace",
                                                                                  INST_LEVEL_3,
                                                                                  null,
                                                                                  false);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(modifiedReleaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, LifecycleOperationState.COMPLETED);

        // Check operation and history records after instantiate
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                             lifeCycleOperationId,
                                                                             LifecycleOperationType.INSTANTIATE,
                                                                             InstantiationState.INSTANTIATED);

        //Check that instantiationLevel is correctly set on the instance
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_3);
        vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions).containsEntry(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        Map<String, ReplicaDetails> pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        //Check non-linear scale correct
        //Payload_3 = JL only.
        ReplicaDetails jlScaledVM = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVM.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(jlScaledVM.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVM.getMaxReplicasCount()).isNull();
        assertThat(jlScaledVM.getMinReplicasCount()).isNull();

        // Check others are all set using initial deltas and Manual/CISM as defined
        ReplicaDetails tlScaledVM = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVM.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVM.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVM.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVM.getMinReplicasCount()).isNull();

        ReplicaDetails plScaledVM = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVM.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVM.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVM.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVM.getMinReplicasCount()).isEqualTo(1);

        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);
        Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(1);

        ReplicaDetails clScaledVM = pkg1ReplicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVM.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVM.getAutoScalingEnabledValue()).isTrue();
        assertThat(clScaledVM.getMaxReplicasCount()).isEqualTo(3);
        assertThat(clScaledVM.getMinReplicasCount()).isEqualTo(3);

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(2);
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        String descriptionAfterInstantiation = "description After Instantiation";

        // Modify description after instantiation
        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           descriptionAfterInstantiation);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        modifyOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(modifyOperation.getOperationState()).isEqualTo(COMPLETED);

        ChangedInfo changedInfoAfterInstantiation = changedInfoRepository.findById(modifyOperation.getOperationOccurrenceId()).orElse(null);
        assertThat(changedInfoAfterInstantiation).isNotNull();
        assertThat(instance.getVnfInstanceDescription()).isEqualTo(changedInfoAfterInstantiation.getVnfInstanceDescription());

        // Modify extensions after instantiation
        extensions = new HashMap<>();
        vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           extensions,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, LifecycleOperationState.COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.MODIFY_INFO,
                                                   InstantiationState.INSTANTIATED);

        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        //Check extensions set on the instance correctly
        vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling).containsAllEntriesOf((Map<String, Object>) extensions.get(VNF_CONTROLLED_SCALING));

        //Check level has been maintained
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_3);

        //Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(1);

        //Check Payload_3 replicaDetails
        ReplicaDetails tlScaledVMAfterUpgrade = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVMAfterUpgrade.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVMAfterUpgrade.getMinReplicasCount()).isEqualTo(1);

        //Check Payload replicaDetails.
        ReplicaDetails plScaledVMAfterUpgrade = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVMAfterUpgrade.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVMAfterUpgrade.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails clScaledVMAfterUpgrade = pkg1ReplicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVMAfterUpgrade.getMaxReplicasCount()).isEqualTo(3);
        assertThat(clScaledVMAfterUpgrade.getMinReplicasCount()).isEqualTo(3);

        //Check that Payload_2 replicaDetails
        ReplicaDetails jlScaledVMAfterUpgrade = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(jlScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isTrue();
        assertThat(jlScaledVMAfterUpgrade.getMaxReplicasCount()).isEqualTo(4);
        assertThat(jlScaledVMAfterUpgrade.getMinReplicasCount()).isEqualTo(1);

        // Modify extensions after instantiation with only one affected chart
        extensions = new HashMap<>();
        vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           extensions,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "3");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, LifecycleOperationState.COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.MODIFY_INFO,
                                                   InstantiationState.INSTANTIATED);

        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        //Check extensions set on the instance correctly
        vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling).containsAllEntriesOf((Map<String, Object>) extensions.get(VNF_CONTROLLED_SCALING));

        //Check level has been maintained
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_3);

        //Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        //Check revision has increased for affected charts
        assertThat(packageTwo.getRevisionNumber()).isEqualTo("3");

        pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(1);
        //Check revision has not increased
        assertThat(packageOne.getRevisionNumber()).isEqualTo("2");

        //Check Payload_3 replicaDetails has not changed
        assertThat(jlScaledVMAfterUpgrade).isEqualTo(pkg2ReplicaDetails.get(JL_SCALED_VM));

        //Check Payload replicaDetails has not changed
        assertThat(plScaledVMAfterUpgrade).isEqualTo(pkg2ReplicaDetails.get(PL_SCALED_VM));
        assertThat(clScaledVMAfterUpgrade).isEqualTo(pkg1ReplicaDetails.get(CL_SCALED_VM));

        //Check that Payload_2 replicaDetails has changed
        ReplicaDetails tlScaledVMAfterModify = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVMAfterModify.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVMAfterModify.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVMAfterModify.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVMAfterModify.getMinReplicasCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulInstantiateModifyVnfWithMultipleHelmChart() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-no-level-extensions-modify";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "scale-non-scalable-chart");

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            "end-to-end-default-no-level-namespace",
                                                                                            null,
                                                                                            extensions,
                                                                                            false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        // Check operation and history records after instantiate
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        //Assertions on state of instance after successful instantiate
        VnfInstance vnfInstance = verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                                             lifeCycleOperationId,
                                                                             LifecycleOperationType.INSTANTIATE,
                                                                             InstantiationState.INSTANTIATED);

        Map<String, Object> vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling).containsAllEntriesOf((Map<String, Object>) extensions.get(VNF_CONTROLLED_SCALING));

        //Check that instantiationLevel is not set
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(null);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(0);

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(2);

        //Payload associated with PL and TL
        ReplicaDetails plScaledVm = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails tlScaledVm = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        //Check all replicaDetail params have the correct value
        assertThat(tlScaledVm.getMaxReplicasParameterName()).isEqualTo("TL_scaled_vm.autoscaling.maxReplicas");
        assertThat(tlScaledVm.getMinReplicasParameterName()).isEqualTo("TL_scaled_vm.autoscaling.minReplicas");
        assertThat(tlScaledVm.getScalingParameterName()).isEqualTo("TL_scaled_vm.replicaCount");
        assertThat(tlScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("TL_scaled_vm.autoscaling.engine.enabled");
        assertThat(plScaledVm.getMaxReplicasParameterName()).isEqualTo("PL__scaled_vm.autoscaling.maxReplicas");
        assertThat(plScaledVm.getMinReplicasParameterName()).isEqualTo("PL__scaled_vm.autoscaling.minReplicas");
        assertThat(plScaledVm.getScalingParameterName()).isEqualTo("PL__scaled_vm.replicaCount");
        assertThat(plScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("PL__scaled_vm.autoscaling.engine.enabled");

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 defined");
            }
        }

        // Modify extensions after instantiation with only one affected chart
        extensions = new HashMap<>();
        vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        result = requestHelper.getMvcResultModifyRequestWithLevelsExtensionsVerifyAccepted(vnfInstanceResponse,
                                                                                           extensions,
                                                                                           null,
                                                                                           null,
                                                                                           null,
                                                                                           null);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, LifecycleOperationState.COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.MODIFY_INFO,
                                                   InstantiationState.INSTANTIATED);

        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        //Check extensions set on the instance correctly
        vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling).containsAllEntriesOf((Map<String, Object>) extensions.get(VNF_CONTROLLED_SCALING));

        //Check that instantiationLevel is not set
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(null);

        //Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(0);

        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(2);

        //Payload associated with PL and TL
        plScaledVm = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(1);

        tlScaledVm = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);
    }
}
