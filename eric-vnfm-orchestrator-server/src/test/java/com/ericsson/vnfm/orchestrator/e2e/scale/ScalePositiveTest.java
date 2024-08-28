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
package com.ericsson.vnfm.orchestrator.e2e.scale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.CL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.CNF_BRO_URL_INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_NO_SCALING_MAPPING_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANTIATE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.INST_LEVEL_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.JL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_3;
import static com.ericsson.vnfm.orchestrator.TestUtils.PL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_1;
import static com.ericsson.vnfm.orchestrator.TestUtils.SAMPLE_HELM_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.TL_SCALED_VM;
import static com.ericsson.vnfm.orchestrator.TestUtils.UPGRADE_URL_ENDING;
import static com.ericsson.vnfm.orchestrator.TestUtils.firstHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.TestUtils.secondHelmReleaseNameFor;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkDefaultExtensionsAreSet;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkLevelsReplicaDetailsNull;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValueInMap;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.type.TypeReference;

public class ScalePositiveTest extends AbstractEndToEndTest {

    @SpyBean
    private PackageService packageService;

    @Test
    @SuppressWarnings("unchecked")
    public void successfulInstantiateScaleChangePkgWithMultipleHelmChartPackagesSettingLevelAndExtensions() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-level-extensions";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //// Check that default extensions set correctly
        checkDefaultExtensionsAreSet(instance);

        //Create extensions
        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        //Instantiate with non-default level and extensions
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            "instantiate-multi-levels",
                                                                                            INST_LEVEL_2,
                                                                                            extensions, false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        verificationHelper.checkComplexTypesProperlyMapped(result, vnfInstanceResponse);
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_INSTANTIATE);

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

        //Verify that instance contains correct values
        verifyMapContainsKey(vnfInstance.getCombinedValuesFile(), "listType");

        //Check that instantiationLevel is correctly set on the instance
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_2);
        Map<String, Object> vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions.get(VNF_CONTROLLED_SCALING)).isEqualTo(vnfControlledScaling);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        //Payload 2 only associated with TL_SCALED_VM and Manual
        ReplicaDetails tlScaledVM = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVM.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(tlScaledVM.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVM.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVM.getMinReplicasCount()).isNull();

        // Check others are all set using initial deltas and Manual/CISM as defined
        ReplicaDetails plScaledVM = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVM.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVM.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVM.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVM.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails jlScaledVM = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVM.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVM.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVM.getMaxReplicasCount()).isNull();
        assertThat(jlScaledVM.getMinReplicasCount()).isNull();

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
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(3);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Scale Out
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 defined");
            }
        }

        //Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        //Check the replicaDetails of each chart have been set correctly
        //Payload 2 only associated with TL_SCALED_VM and Manual

        ReplicaDetails tlScaledVMAfterScaleOut = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVMAfterScaleOut.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(tlScaledVMAfterScaleOut.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVMAfterScaleOut.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVMAfterScaleOut.getMinReplicasCount()).isNull();

        ReplicaDetails plScaledVMAfterScaleOut = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVMAfterScaleOut).isEqualTo(plScaledVM);
        ReplicaDetails jlScaledVMAfterScaleOut = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVMAfterScaleOut).isEqualTo(jlScaledVM);

        //Scale In
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                   HelmReleaseState.COMPLETED,
                                                                   lifeCycleOperationId,
                                                                   HelmReleaseOperationType.SCALE,
                                                                   "3");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        // Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        // Check the replicaDetails of charts have been reset correctly
        ReplicaDetails tlScaledVMAfterScaleIn = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVMAfterScaleIn).isEqualTo(tlScaledVM);
        ReplicaDetails plScaledVMAfterScaleIn = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVMAfterScaleIn).isEqualTo(plScaledVM);
        ReplicaDetails jlScaledVMAfterScaleIn = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVMAfterScaleIn).isEqualTo(jlScaledVM);

        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(3);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Change Pkg Request and set Payload_2 to Manual, Payload_3 to CISM

        extensions = new HashMap<>();
        vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD_2, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, CISM_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        result = requestHelper.getMvcResultChangeVnfpkgRequestWithExtensionsAndVerifyAccepted(vnfInstanceResponse,
                                                                                              E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU,
                                                                                              extensions);
        lifeCycleOperationId = getLifeCycleOperationId(result);
        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "4");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isBlank();
        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        //Check that instance contains correct values
        verifyMapDoesNotContainKey(vnfInstance.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 2, UPGRADE_URL_ENDING, "listType", false);

        //Check extensions set on the instance correctly
        vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions.get(VNF_CONTROLLED_SCALING)).isEqualTo(vnfControlledScaling);

        //Check level has been maintained
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_2);

        //Check scale info as expected
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(3);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

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
        assertThat(plScaledVMAfterUpgrade.getMaxReplicasCount()).isNull();
        assertThat(plScaledVMAfterUpgrade.getMinReplicasCount()).isNull();

        ReplicaDetails clScaledVMAfterUpgrade = pkg1ReplicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVMAfterUpgrade.getMaxReplicasCount()).isNull();
        assertThat(clScaledVMAfterUpgrade.getMinReplicasCount()).isNull();

        //Check that Payload_2 replicaDetails
        ReplicaDetails jlScaledVMAfterUpgrade = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVMAfterUpgrade.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(jlScaledVMAfterUpgrade.getAutoScalingEnabledValue()).isTrue();
        assertThat(jlScaledVMAfterUpgrade.getMaxReplicasCount()).isEqualTo(4);
        assertThat(jlScaledVMAfterUpgrade.getMinReplicasCount()).isEqualTo(1);

        //Scale Out
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleAfterUpgradeHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleAfterUpgradeHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that instance contains correct values
        verifyMapDoesNotContainKey(vnfInstance.getCombinedValuesFile(), "listType");

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 defined");
            }
        }

        //Check ReplicaDetails of specific charts
        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(pkg2ReplicaDetails).hasSize(3);

        //Check the replicaDetails of each chart have been set correctly
        //Payload 2 only associated with TL_SCALED_VM and Manual

        ReplicaDetails jlScaledVMAfterUpgradeAndScaleOut = pkg2ReplicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVMAfterUpgradeAndScaleOut.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(jlScaledVMAfterUpgradeAndScaleOut.getAutoScalingEnabledValue()).isTrue();
        assertThat(jlScaledVMAfterUpgradeAndScaleOut.getMaxReplicasCount()).isEqualTo(5);
        assertThat(jlScaledVMAfterUpgradeAndScaleOut.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails plScaledVMAfterUpgradeAndScaleOut = pkg2ReplicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVMAfterUpgradeAndScaleOut).isEqualTo(plScaledVMAfterUpgrade);
        ReplicaDetails tlScaledVMAfterUpgradeAndScaleOut = pkg2ReplicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVMAfterUpgradeAndScaleOut).isEqualTo(tlScaledVMAfterUpgrade);
    }

    @Test
    public void successfulInstantiateScaleWithMultipleHelmChartPackagesUsingDefaultLevelAndExtensions() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-default-level-extensions";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //// Check that default extensions set correctly
        checkDefaultExtensionsAreSet(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "end-to-end-default-level-namespace-1");
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

        //Check that instantiationLevel is correctly set on the instance
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_1);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(1);

        //Payload associated with CL and PL
        ReplicaDetails clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(19);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isNull();
        assertThat(clScaledVm.getMinReplicasCount()).isNull();

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(3);

        ReplicaDetails plScaledVm = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(17);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isNull();
        assertThat(plScaledVm.getMinReplicasCount()).isNull();

        // Check others are all set using initial deltas and Manual/CISM as defined
        ReplicaDetails tlScaledVm = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails jlScaledVm = replicaDetails2.get(JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(jlScaledVm.getMinReplicasCount()).isNull();

        //Check all replicaDetail params have the correct value
        assertThat(clScaledVm.getMaxReplicasParameterName()).isEqualTo("CL_scaled_vm.autoscaling.maxReplicas");
        assertThat(clScaledVm.getMinReplicasParameterName()).isEqualTo("CL_scaled_vm.autoscaling.minReplicas");
        assertThat(clScaledVm.getScalingParameterName()).isEqualTo("CL_scaled_vm.replicaCount");
        assertThat(clScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("CL_scaled_vm.autoscaling.engine.enabled");
        assertThat(tlScaledVm.getMaxReplicasParameterName()).isEqualTo("TL_scaled_vm.autoscaling.maxReplicas");
        assertThat(tlScaledVm.getMinReplicasParameterName()).isEqualTo("TL_scaled_vm.autoscaling.minReplicas");
        assertThat(tlScaledVm.getScalingParameterName()).isEqualTo("TL_scaled_vm.replicaCount");
        assertThat(tlScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("TL_scaled_vm.autoscaling.engine.enabled");
        assertThat(plScaledVm.getMaxReplicasParameterName()).isEqualTo("PL__scaled_vm.autoscaling.maxReplicas");
        assertThat(plScaledVm.getMinReplicasParameterName()).isEqualTo("PL__scaled_vm.autoscaling.minReplicas");
        assertThat(plScaledVm.getScalingParameterName()).isEqualTo("PL__scaled_vm.replicaCount");
        assertThat(plScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("PL__scaled_vm.autoscaling.engine.enabled");
        assertThat(jlScaledVm.getMaxReplicasParameterName()).isEqualTo("JL_scaled_vm.autoscaling.maxReplicas");
        assertThat(jlScaledVm.getMinReplicasParameterName()).isEqualTo("JL_scaled_vm.autoscaling.minReplicas");
        assertThat(jlScaledVm.getScalingParameterName()).isEqualTo("JL_scaled_vm.replicaCount");
        assertThat(jlScaledVm.getAutoScalingEnabledParameterName()).isEqualTo("JL_scaled_vm.autoscaling.engine.enabled");

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Scale out an aspect that is CISM controlled (Payload 2)
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        //Check that chart that is CISM controlled is scaled correctly
        ReplicaDetails tlScaledVmAfterScale = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(2);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(2);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(1);

        // Check that other charts aren't impacted
        ReplicaDetails plScaledVmAfterScale = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScale).isEqualTo(plScaledVm);
        ReplicaDetails jlScaledVmAfterScale = replicaDetails2.get(JL_SCALED_VM);
        assertThat(jlScaledVmAfterScale).isEqualTo(jlScaledVm);

        //Scale In
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                   HelmReleaseState.COMPLETED,
                                                                   lifeCycleOperationId,
                                                                   HelmReleaseOperationType.SCALE,
                                                                   "3");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        //Check that chart that is CISM controlled is scaled in correctly
        ReplicaDetails tlScaledVmAfterScaleIn = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScaleIn).isEqualTo(tlScaledVm);

        //Check that scale level is only decreased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }
    }

    @Test
    public void successfulInstantiateScaleWhenScaleLevelZeroAndOneChartWithOnlyNonScalablePods() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-non-scalable-chart";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "scale-non-scalable-chart");

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "end-to-end-non-scalable-pods-chart");
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

        //Scale out an aspect that is CISM controlled (Payload 2)
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(0);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else {
                fail("There should be no aspect other than Payload and Payload_2 defined");
            }
        }

        helmCharts = vnfInstance.getHelmCharts();

        packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);
        replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(0);

        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        //Check that chart that is CISM controlled is scaled correctly
        ReplicaDetails tlScaledVmAfterScale = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(5);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(1);

        //Check that chart that is CISM controlled is scaled correctly
        ReplicaDetails plScaledVmAfterScale = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(plScaledVmAfterScale.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(5);
        assertThat(plScaledVmAfterScale.getMinReplicasCount()).isEqualTo(1);
    }

    @Test
    public void successfulInstantiateScaleWithMultipleHelmChartPackagesWithExtensionsWithoutScalingMappingManoControlledScaling() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-default-no-scaling-mapping";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_NO_SCALING_MAPPING_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        checkDefaultExtensionsAreSet(instance);
        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            "default-level-no-scaling-mapping-namespace",
                                                                                            null,
                                                                                            null,
                                                                                            true);
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

        //Check that instantiationLevel is correctly set on the instance
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_1);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(4);

        //Payload associated with CL
        ReplicaDetails clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(19);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(19);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(19);

        //Payload associated with JL
        ReplicaDetails jlScaledVm = replicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(1);

        //Payload associated with TL
        ReplicaDetails tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        //Payload associated with PL
        ReplicaDetails plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(17);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(17);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(17);

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(4);

        clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(19);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(19);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(19);

        jlScaledVm = replicaDetails.get(JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(1);

        tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(17);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(17);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(17);

        // Check all replicaDetail params have the correct value
        assertThat(clScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta.maxReplicas");
        assertThat(clScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta.minReplicas");
        assertThat(clScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta.replicaCount");
        assertThat(clScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(tlScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta_1.maxReplicas");
        assertThat(tlScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta_1.minReplicas");
        assertThat(tlScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta_1.replicaCount");
        assertThat(tlScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(plScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta_2.maxReplicas");
        assertThat(plScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta_2.minReplicas");
        assertThat(plScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta_2.replicaCount");
        assertThat(plScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(jlScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta_3.maxReplicas");
        assertThat(jlScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta_3.minReplicas");
        assertThat(jlScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta_3.replicaCount");
        assertThat(jlScaledVm.getAutoScalingEnabledParameterName()).isNull();

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Scale out an aspect Payload 2
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        //Check that chart that is scaled correctly
        ReplicaDetails tlScaledVmAfterScale = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(2);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(2);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(1);

        // Check that other charts aren't impacted
        ReplicaDetails plScaledVmAfterScale = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScale).isEqualTo(plScaledVm);
        ReplicaDetails jlScaledVmAfterScale = replicaDetails2.get(JL_SCALED_VM);
        assertThat(jlScaledVmAfterScale).isEqualTo(jlScaledVm);
        ReplicaDetails clScaledVmAfterScale = replicaDetails2.get(CL_SCALED_VM);
        assertThat(clScaledVmAfterScale).isEqualTo(clScaledVm);

        //Scale In
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                   HelmReleaseState.COMPLETED,
                                                                   lifeCycleOperationId,
                                                                   HelmReleaseOperationType.SCALE,
                                                                   "3");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        //Check that chart that is scaled in correctly
        ReplicaDetails tlScaledVmAfterScaleIn = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScaleIn).isEqualTo(tlScaledVm);

        //Check that scale level is only decreased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(4);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_3.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }
    }

    @Test
    public void successfulInstantiateScaleWithNoExtensionsWithoutScalingMapping() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-no-extensions-no-scaling-mapping";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "levels-no-extensions-no-scaling-mapping");

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            "default-no-extensions-no-scaling-mapping-namespace",
                                                                                            null,
                                                                                            null,
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

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(3);

        ReplicaDetails clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(clScaledVm.getMaxReplicasCount()).isNull();
        assertThat(clScaledVm.getMinReplicasCount()).isNull();

        ReplicaDetails plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isNull();
        assertThat(plScaledVm.getMinReplicasCount()).isNull();

        ReplicaDetails tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVm.getMinReplicasCount()).isNull();

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(3);

        clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(clScaledVm.getMaxReplicasCount()).isNull();
        assertThat(clScaledVm.getMinReplicasCount()).isNull();

        plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isNull();
        assertThat(plScaledVm.getMinReplicasCount()).isNull();

        tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVm.getMinReplicasCount()).isNull();

        //Check all replicaDetail params have the correct value
        assertThat(clScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta1.maxReplicas");
        assertThat(clScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta1.minReplicas");
        assertThat(clScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta1.replicaCount");
        assertThat(clScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(tlScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta1.maxReplicas");
        assertThat(tlScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta1.minReplicas");
        assertThat(tlScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta1.replicaCount");
        assertThat(tlScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(plScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta2.maxReplicas");
        assertThat(plScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta2.minReplicas");
        assertThat(plScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta2.replicaCount");
        assertThat(plScaledVm.getAutoScalingEnabledParameterName()).isNull();

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Scale out
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        ReplicaDetails tlScaledVmAfterScale = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(6);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(6);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(6);

        ReplicaDetails plScaledVmAfterScale = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(plScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(4);
        assertThat(plScaledVmAfterScale.getMinReplicasCount()).isEqualTo(4);

        // Check that other charts aren't impacted
        ReplicaDetails clScaledVmAfterScale = replicaDetails2.get(CL_SCALED_VM);
        assertThat(clScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());
        assertThat(clScaledVmAfterScale.getMinReplicasCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());
        assertThat(clScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());

        //Scale In
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                   HelmReleaseState.COMPLETED,
                                                                   lifeCycleOperationId,
                                                                   HelmReleaseOperationType.SCALE,
                                                                   "3");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        ReplicaDetails tlScaledVmAfterScaleIn = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVmAfterScaleIn.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterScaleIn.getMaxReplicasCount()).isEqualTo(3);
        assertThat(tlScaledVmAfterScaleIn.getMinReplicasCount()).isEqualTo(3);

        ReplicaDetails plScaledVmAfterScaleIn = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVmAfterScaleIn.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterScaleIn.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVmAfterScaleIn.getMinReplicasCount()).isEqualTo(1);

        // Check CL is not impacted
        ReplicaDetails clScaledVmAfterScaleIn = replicaDetails2.get(CL_SCALED_VM);
        assertThat(clScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());

        //Check that scale level is only decreased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }
    }

    @Test
    public void successfulInstantiateScaleWithNoExtensionsWithoutScalingMappingManoControlledScaling() throws Exception {

        //Create Identifier
        final String releaseName = "no-extensions-no-scaling-mapping-mano-controlled";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, "levels-no-extensions-no-scaling-mapping");

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(
                vnfInstanceResponse,
                "default-no-extensions-no-scaling-mapping-mano-controlled-ns",
                null,
                null,
                true);
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

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(3);

        ReplicaDetails clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(3);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(3);

        ReplicaDetails plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(1);

        ReplicaDetails tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(3);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(3);

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);

        Map<String, ReplicaDetails> replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
        assertThat(replicaDetails2).hasSize(3);

        clScaledVm = replicaDetails.get(CL_SCALED_VM);
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(3);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(3);

        plScaledVm = replicaDetails.get(PL_SCALED_VM);
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(1);

        tlScaledVm = replicaDetails.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(3);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(3);

        //Check all replicaDetail params have the correct value
        assertThat(clScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta1.maxReplicas");
        assertThat(clScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta1.minReplicas");
        assertThat(clScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta1.replicaCount");
        assertThat(clScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(tlScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta1.maxReplicas");
        assertThat(tlScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta1.minReplicas");
        assertThat(tlScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta1.replicaCount");
        assertThat(tlScaledVm.getAutoScalingEnabledParameterName()).isNull();
        assertThat(plScaledVm.getMaxReplicasParameterName()).isEqualTo("Payload_InitialDelta2.maxReplicas");
        assertThat(plScaledVm.getMinReplicasParameterName()).isEqualTo("Payload_InitialDelta2.minReplicas");
        assertThat(plScaledVm.getScalingParameterName()).isEqualTo("Payload_InitialDelta2.replicaCount");
        assertThat(plScaledVm.getAutoScalingEnabledParameterName()).isNull();

        //Check Scale Info Correct after Instantiate
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        //Scale out
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        //Check that scale level is only increased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        ReplicaDetails tlScaledVmAfterScale = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(6);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(6);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(6);

        ReplicaDetails plScaledVmAfterScale = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(4);
        assertThat(plScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(4);
        assertThat(plScaledVmAfterScale.getMinReplicasCount()).isEqualTo(4);

        // Check that other charts aren't impacted
        ReplicaDetails clScaledVmAfterScale = replicaDetails2.get(CL_SCALED_VM);
        assertThat(clScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());
        assertThat(clScaledVmAfterScale.getMinReplicasCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());
        assertThat(clScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());

        //Scale In
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.IN, PAYLOAD_2);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                   HelmReleaseState.COMPLETED,
                                                                   lifeCycleOperationId,
                                                                   HelmReleaseOperationType.SCALE,
                                                                   "3");
        messageHelper.sendMessageForChart(completedScaleHelmMessage,
                                          secondHelmReleaseNameFor(releaseName),
                                          vnfInstanceResponse.getId(),
                                          false,
                                          HelmReleaseState.COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        helmCharts = vnfInstance.getHelmCharts();
        packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        replicaDetails2 = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);

        ReplicaDetails tlScaledVmAfterScaleIn = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(tlScaledVmAfterScaleIn.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterScaleIn.getMaxReplicasCount()).isEqualTo(3);
        assertThat(tlScaledVmAfterScaleIn.getMinReplicasCount()).isEqualTo(3);

        ReplicaDetails plScaledVmAfterScaleIn = replicaDetails2.get(PL_SCALED_VM);
        assertThat(plScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVmAfterScaleIn.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterScaleIn.getMaxReplicasCount()).isEqualTo(1);
        assertThat(plScaledVmAfterScaleIn.getMinReplicasCount()).isEqualTo(1);

        // Check that other charts aren't impacted
        ReplicaDetails clScaledVmAfterScaleIn = replicaDetails2.get(CL_SCALED_VM);
        assertThat(clScaledVmAfterScaleIn.getCurrentReplicaCount()).isEqualTo(clScaledVm.getCurrentReplicaCount());

        //Check that scale level is only decreased for Payload_2
        scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There should be no aspect other than Payload and Payload_2 and Payload_3 defined");
            }
        }
    }

    @Test
    public void successfulChangePackageInfoMultiChartWithScaleDone() throws Exception {
        final String releaseName = "end-to-end-multi-with-scale";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "multi-with-scale-1");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(3);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get("PL__scaled_vm");
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(plScaledVm.getMaxReplicasCount()).isNull();
        assertThat(plScaledVm.getMinReplicasCount()).isNull();

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get("CL_scaled_vm");
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(3);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(clScaledVm.getMaxReplicasCount()).isNull();
        assertThat(clScaledVm.getMinReplicasCount()).isNull();

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get("TL_scaled_vm");
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(tlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVm.getMinReplicasCount()).isNull();

        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }

        //Scale
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedScaleHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        // assertions on instance
        assertThat(vnfInstance.getTempInstance()).isNull();

        List<HelmChart> helmChartsAfterScale = vnfInstance.getHelmCharts();
        HelmChart packageOneAfterScale = getHelmChartByName(helmChartsAfterScale, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetailsAfterScale = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOneAfterScale);
        assertThat(pkg1ReplicaDetailsAfterScale).hasSize(3);

        ReplicaDetails plScaledVmAfterScale = pkg1ReplicaDetailsAfterScale.get("PL__scaled_vm");
        assertThat(plScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(plScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(5);
        assertThat(plScaledVmAfterScale.getMinReplicasCount()).isEqualTo(5);

        ReplicaDetails clScaledVmAfterScale = pkg1ReplicaDetailsAfterScale.get("CL_scaled_vm");
        assertThat(clScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(7);
        assertThat(clScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(7);
        assertThat(clScaledVmAfterScale.getMinReplicasCount()).isEqualTo(7);

        ReplicaDetails tlScaledVmAfterScale = pkg1ReplicaDetailsAfterScale.get("TL_scaled_vm");
        assertThat(tlScaledVmAfterScale.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVmAfterScale.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterScale.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVmAfterScale.getMinReplicasCount()).isEqualTo(1);

        List<ScaleInfoEntity> scaleInfoEntitiesAfterScale = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntitiesAfterScale).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterScale) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }

        //Upgrade
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(
                vnfInstanceResponse.getId(), E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false, true, false
        );
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "3");
        completedHelmMessage.setMessage("Upgrade after scale completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        List<HelmChart> helmChartsAfterUpgrade = vnfInstance.getHelmCharts();
        HelmChart packageOneAfterUpgrade = getHelmChartByName(helmChartsAfterUpgrade, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetailsAfterUpgrade =
                replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOneAfterUpgrade);
        assertThat(pkg1ReplicaDetailsAfterUpgrade).hasSize(3);

        ReplicaDetails plScaledVmAfterUpgrade = pkg1ReplicaDetailsAfterUpgrade.get("PL__scaled_vm");
        assertThat(plScaledVmAfterUpgrade.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(plScaledVmAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVmAfterUpgrade.getMaxReplicasCount()).isEqualTo(5);
        assertThat(plScaledVmAfterUpgrade.getMinReplicasCount()).isEqualTo(5);

        ReplicaDetails clScaledVmAfterUpgrade = pkg1ReplicaDetailsAfterUpgrade.get("CL_scaled_vm");
        assertThat(clScaledVmAfterUpgrade.getCurrentReplicaCount()).isEqualTo(7);
        assertThat(clScaledVmAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVmAfterUpgrade.getMaxReplicasCount()).isEqualTo(7);
        assertThat(clScaledVmAfterUpgrade.getMinReplicasCount()).isEqualTo(7);

        ReplicaDetails tlScaledVmAfterUpgrade = pkg1ReplicaDetailsAfterUpgrade.get("TL_scaled_vm");
        assertThat(tlScaledVmAfterUpgrade.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVmAfterUpgrade.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVmAfterUpgrade.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVmAfterUpgrade.getMinReplicasCount()).isEqualTo(1);

        List<ScaleInfoEntity> scaleInfoEntitiesAfterUpgrade = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntitiesAfterUpgrade).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterUpgrade) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }
    }

    @Test
    public void successfulChangePackageInfoWithNoScalePoliciesAndMultiChartWithScaleDone() throws Exception {
        addTopologyTemplateToDescriptor("no-scaling");

        final String releaseName = "end-to-end-multi-with-no-policy";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "multi-with-scale-2");
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade
        assertThat(vnfInstance.getResourceDetails()).isNotBlank();
        Map<String, Integer> resourcesDetail = mapper.readValue(vnfInstance.getResourceDetails(),
                                                                new TypeReference<HashMap<String, Integer>>() {
                                                                });
        assertThat(resourcesDetail.get(TL_SCALED_VM)).isEqualTo(1);
        assertThat(resourcesDetail.get(PL_SCALED_VM)).isEqualTo(1);
        assertThat(resourcesDetail.get(CL_SCALED_VM)).isEqualTo(3);
        List<ScaleInfoEntity> scaleInfoEntities = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntities).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntities) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }

        //Scale
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedScaleHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getResourceDetails()).isNotBlank();
        assertThat(vnfInstance.getTempInstance()).isNull();

        Map<String, Integer> resourcesDetailAfterScale = mapper.readValue(vnfInstance.getResourceDetails(),
                                                                          new TypeReference<HashMap<String, Integer>>() {
                                                                          });
        assertThat(resourcesDetailAfterScale.get(TL_SCALED_VM)).isEqualTo(1);
        assertThat(resourcesDetailAfterScale.get(PL_SCALED_VM)).isEqualTo(5);
        assertThat(resourcesDetailAfterScale.get(CL_SCALED_VM)).isEqualTo(7);
        List<ScaleInfoEntity> scaleInfoEntitiesAfterScale = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntitiesAfterScale).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterScale) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }

        //Upgrade
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "no-scaling", false, true, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "3");
        completedHelmMessage.setMessage("Upgrade after scale completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getResourceDetails()).isNull();
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(vnfInstance.getScaleInfoEntity()).isEmpty();
    }

    @Test
    public void successfulChangePackageInfoDownsizeAllowedMultiChart() throws Exception {
        String clusterName = "config";
        final String releaseName = "end-to-end-downsize-multi";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        vnfInstanceResponse.setClusterName(clusterName);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse,
                                                                                         "downsize-namespace-multi-1",
                                                                                         clusterName);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm value set during upgrade

        //Scale
        result = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, PAYLOAD);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               lifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedScaleHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        awaitHelper.awaitOperationReachingState(lifeCycleOperationId, COMPLETED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(vnfInstance.getResourceDetails()).isNotBlank();
        assertThat(vnfInstance.getTempInstance()).isNull();

        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate

        Map<String, Integer> resourcesDetailAfterScale = mapper.readValue(vnfInstance.getResourceDetails(),
                                                                          new TypeReference<HashMap<String, Integer>>() {
                                                                          });
        assertThat(resourcesDetailAfterScale.get(TL_SCALED_VM)).isEqualTo(1);
        assertThat(resourcesDetailAfterScale.get(PL_SCALED_VM)).isEqualTo(5);
        assertThat(resourcesDetailAfterScale.get(CL_SCALED_VM)).isEqualTo(7);
        List<ScaleInfoEntity> scaleInfoEntitiesAfterScale = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntitiesAfterScale).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterScale) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }

        //Upgrade - with downsize
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(
                vnfInstanceResponse.getId(), E2E_CHANGE_PACKAGE_INFO_VNFD_ID, true,true, false
        );
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake downsize completion message
        WorkflowServiceEventMessage completedWfsMessage = getWfsEventMessage(lifeCycleOperationId,
                                                                             WorkflowServiceEventType.DOWNSIZE,
                                                                             WorkflowServiceEventStatus.COMPLETED,
                                                                             "Successfully scaled down all ReplicaSets and StatefulSets to 0.");
        messageHelper.sendDownsizeMessageForAllCharts(completedWfsMessage, vnfInstanceResponse.getId(), false);

        //Fake upgrade completion messages
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "2");
        completedHelmMessage.setMessage("Upgrade after downsize completed successfully.");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        vnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(vnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);

        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        verifyValueInMap(vnfInstance.getCombinedAdditionalParams(), HELM_NO_HOOKS, null); //evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "listType", "[string1, string2]"); //non-evnfm value set during instantiate
        verifyValueInMap(vnfInstance.getCombinedValuesFile(), "upgradeParam", "testing value"); //non-evnfm value set during upgrade

        assertThat(vnfInstance.getResourceDetails()).isNotBlank();
        assertThat(vnfInstance.getHelmClientVersion()).isEqualTo("3.10");

        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, "sample-helm1");

        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(pkg1ReplicaDetails).hasSize(3);

        ReplicaDetails plScaledVm = pkg1ReplicaDetails.get("PL__scaled_vm");
        assertThat(plScaledVm.getCurrentReplicaCount()).isEqualTo(5);
        assertThat(plScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(plScaledVm.getMaxReplicasCount()).isEqualTo(5);
        assertThat(plScaledVm.getMinReplicasCount()).isEqualTo(5);

        ReplicaDetails clScaledVm = pkg1ReplicaDetails.get("CL_scaled_vm");
        assertThat(clScaledVm.getCurrentReplicaCount()).isEqualTo(7);
        assertThat(clScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(clScaledVm.getMaxReplicasCount()).isEqualTo(7);
        assertThat(clScaledVm.getMinReplicasCount()).isEqualTo(7);

        ReplicaDetails tlScaledVm = pkg1ReplicaDetails.get("TL_scaled_vm");
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(tlScaledVm.getMinReplicasCount()).isEqualTo(1);

        List<ScaleInfoEntity> scaleInfoEntitiesAfterUpgrade = vnfInstance.getScaleInfoEntity();
        assertThat(scaleInfoEntitiesAfterUpgrade).hasSize(2);

        for (ScaleInfoEntity scaleInfo : scaleInfoEntitiesAfterUpgrade) {
            if (PAYLOAD.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isEqualTo(1);
            } else if (PAYLOAD_2.equals(scaleInfo.getAspectId())) {
                assertThat(scaleInfo.getScaleLevel()).isZero();
            } else {
                fail("There is no other aspect defined");
            }
        }
    }

    private void addTopologyTemplateToDescriptor(final String vnfdId) {
        final PackageResponse packageResponse = packageService.getPackageInfo(vnfdId);
        final JSONObject vnfd = packageService.getVnfd(vnfdId);
        vnfd.put("topology_template", createTopologyTemplate());
        packageResponse.setDescriptorModel(vnfd.toString());

        when(packageService.getPackageInfoWithDescriptorModel(vnfdId)).thenReturn(packageResponse);
    }

    private JSONObject createTopologyTemplate() {
        JSONObject topologyTemplate = new JSONObject();

        topologyTemplate.put("node_templates", new JSONObject());
        topologyTemplate.getJSONObject("node_templates").put("SampleVnf", createSampleVnf());

        return topologyTemplate;
    }

    private JSONObject createSampleVnf() {
        JSONObject sampleVnf = new JSONObject();
        sampleVnf.put("type", "Ericsson.SAMPLE-VNF.1_25_CXS101289_R81E08.cxp9025898_4r81e08");
        sampleVnf.put("interfaces", new JSONObject());
        sampleVnf.getJSONObject("interfaces").put("Vnflcm", new JSONObject());

        return sampleVnf;
    }
}
