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
package com.ericsson.vnfm.orchestrator.e2e.upgrade;

import static com.ericsson.vnfm.orchestrator.TestUtils.*;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.*;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJsonToGenericType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestConstants.EXISTING_CLUSTER_CONFIG_NAME;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkDefaultExtensionsAreSet;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.checkLevelsReplicaDetailsNull;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapContainsKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyMapDoesNotContainKey;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValueInMap;
import static com.ericsson.vnfm.orchestrator.e2e.util.VerificationHelper.verifyValuesFilePassedToWfs;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.UPGRADE_FAILED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.HELM_NO_HOOKS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;
import com.ericsson.vnfm.orchestrator.model.entity.*;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.EvnfmUpgrade;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Instantiate;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.operations.Terminate;
import com.ericsson.vnfm.orchestrator.repositories.ChangePackageOperationDetailsRepository;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.UsageStateRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;


public class UpgradePositiveTest extends AbstractEndToEndTest {

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @SpyBean
    private PackageService packageService;

    @Autowired
    private ChangePackageOperationDetailsRepository changePackageOperationDetailsRepository;

    @SpyBean
    private Instantiate instantiateService;

    @SpyBean
    private EvnfmUpgrade upgradeService;

    @SpyBean
    private Terminate terminateService;

    @Test
    public void successfulInstantiateChangePackageInfoNoScaling() throws Exception {
        addTopologyTemplateToDescriptor("no-scaling");

        //Create Identifier
        final String releaseName = "end-to-end-no-scaling";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(instance.getMetadata()).isEqualTo("{\"tenantName\":\"ecm\"}");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "no-scaling-namespace-2");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        List<String> instantiatedScaleIds = checkScaleEntityRecords(lifeCycleOperationId, 2);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getResourceDetails()).isNotNull();
        assertThat(vnfInstance.getPolicies()).isNotNull();
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_INSTANTIATE);
        verifyMapContainsKey(vnfInstance.getCombinedValuesFile(), "listType");

        //Upgrade - Completed with scaling, without values from previous values
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, true);

        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);

        List<String> upgradedScaleIds = checkScaleEntityRecords(lifeCycleOperationId, 2);
        assertThat(CollectionUtils.containsAny(upgradedScaleIds, instantiatedScaleIds)).isFalse();
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getOperationOccurrenceId()).isEqualTo(lifeCycleOperationId);
        assertThat(upgradedVnfInstance.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(upgradedVnfInstance.getTempInstance()).isNull();
        assertThat(upgradedVnfInstance.getHelmCharts()).extracting(HelmChart::getReleaseName)
                .containsOnly("end-to-end-no-scaling-1", "end-to-end-no-scaling-2");
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_UPGRADE);
        verifyMapDoesNotContainKey(upgradedVnfInstance.getCombinedValuesFile(), "listType");
        verifyValuesFilePassedToWfs(restTemplate, 2, UPGRADE_URL_ENDING, "listType", false);

        //Upgrade - Completed no scaling
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "no-scaling", false);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 3, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        checkScaleEntityRecords(lifeCycleOperationId, 0);
        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getResourceDetails()).isNull();
        assertThat(upgradedVnfInstance.getPolicies()).isNull();
        assertThat(upgradedVnfInstance.getScaleInfoEntity()).isEmpty();
        verifyMapContainsKey(upgradedVnfInstance.getCombinedValuesFile(), "Payload_InitialDelta");
        verifyValuesFilePassedToWfs(restTemplate, 4, UPGRADE_URL_ENDING, "Payload_InitialDelta", true);

    }

    @Test
    public void successfulInstantiateChangePackageInfoAllowedYamlFile() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end-instantiate-upgarde-with-yaml";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithYamlFile(vnfInstanceResponse,
                                                                                    "downsize-namespace-multi-with-values-1",
                                                                                    EXISTING_CLUSTER_CONFIG_NAME,
                                                                                    "valueFiles/instantiate.yaml");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        instance = vnfInstanceRepository.findByVnfInstanceId(instance.getVnfInstanceId());
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "eric-pm-server",
                         "{server={persistentVolume={storageClass=erikube-rbd}}}"); //evnfm yaml value set during upgrade
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "extras",
                         "{instantiate=during instantiation}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm additionalParam value set during upgrade

        //Upgrade
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithYamlFile(vnfInstanceResponse,
                                                                           E2E_CHANGE_PACKAGE_INFO_VNFD_ID,
                                                                           false,
                                                                           "valueFiles/upgrade.yaml");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);

        instance = operationAfterUpgrade.getVnfInstance();
        assertThat(instance.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "eric-pm-server",
                         "{server={persistentVolume={storageClass=network-block}}}"); //evnfm yaml value set during upgrade
        verifyValueInMap(instance.getCombinedValuesFile(),
                         "extras",
                         "{instantiate=during instantiation}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(instance.getCombinedValuesFile(), "upgradeParam", "testing value"); //non-evnfm additionalParam value set during upgrade
    }

    @Test
    public void successfulChangePackageInfoExcludeFailedParamsAllowedMultiChartParamsAndYamlFile() throws Exception {
        final String releaseName = "exclude-failed-params-multi-with-values-file";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithYamlFile(vnfInstanceResponse,
                                                                                    "failed-params-namespace-multi-with-values",
                                                                                    EXISTING_CLUSTER_CONFIG_NAME,
                                                                                    "valueFiles/large-values-a.yaml");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                                          HelmReleaseState.COMPLETED,
                                                                                          lifeCycleOperationId,
                                                                                          HelmReleaseOperationType.INSTANTIATE,
                                                                                          "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        instance = vnfInstanceRepository.findByVnfInstanceId(instance.getVnfInstanceId());
        String combinedValuesFile = instance.getCombinedValuesFile();
        verifyValueInMap(instance.getCombinedAdditionalParams(), HELM_NO_HOOKS, "true"); //evnfm value set during instantiate
        verifyValueInMap(combinedValuesFile, "listType", "[string1, string2]"); //non-evnfm additionalParam value set during instantiate
        verifyValueInMap(combinedValuesFile,
                         "eric-evnfm-testing",
                         "{instantiate=not included in upgrade}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(combinedValuesFile, "upgradeParam", null); //non-evnfm additionalParam value set during upgrade
        verifyValueInMap(combinedValuesFile, "eric-data-distributed-coordinator-ed", null); //non-evnfm yaml value set during upgrade

        //Fake upgrade
        final String yamlFileName = "valueFiles/large-values-b.yaml";
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithYamlFile(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false, yamlFileName);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);

        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        completedHelmMessage = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                              HelmReleaseState.COMPLETED,
                                                              lifeCycleOperationId,
                                                              HelmReleaseOperationType.CHANGE_VNFPKG,
                                                              "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completedHelmMessage, vnfInstanceResponse.getId(), false, COMPLETED);

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        String valuesYaml = TestUtils.readDataFromFile(yamlFileName);
        instance = operationAfterUpgrade.getVnfInstance();
        assertThat(operationAfterUpgrade.getOperationParams()).contains(UPGRADE_FAILED_VNFD_KEY);
        assertThat(instance.getCombinedAdditionalParams()).doesNotContain(UPGRADE_FAILED_VNFD_KEY);
        assertThat(valuesYaml).doesNotContain(UPGRADE_FAILED_VNFD_KEY);
        assertThat(instance.getHelmClientVersion()).isEqualTo("3.10");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulInstantiateInvalidChangePkgWithMultipleHelmChartPackagesSettingLevelAndExtensionsNonLinearScaling() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-level-extensions-non-linear";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName,
                                                                                        E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check levels are not set
        checkLevelsReplicaDetailsNull(instance);

        // Check that default extensions set correctly
        checkDefaultExtensionsAreSet(instance);

        //Create extensions
        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfScaling = new HashMap<>();
        vnfScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        vnfScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfScaling.put(PAYLOAD_3, MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfScaling);

        //Instantiate with level and that refers to an aspect with more than one step delta defined
        MvcResult result = requestHelper.getMvcResultInstantiateRequestWithLevelsExtensions(vnfInstanceResponse,
                                                                                            "instantiate-multi-levels-non-linear",
                                                                                            INST_LEVEL_3,
                                                                                            extensions,
                                                                                            false);
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

        //Check that instantiationLevel is correctly set on the instance
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_3);
        final Map<String, Object> vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(extensions.get(VNF_CONTROLLED_SCALING)).isEqualTo(vnfControlledScaling);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();

        HelmChart packageTwo = getHelmChartByName(helmCharts, SAMPLE_HELM_2);
        final Map<String, ReplicaDetails> pkg2ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageTwo);
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
        final Map<String, ReplicaDetails> pkg1ReplicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
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

        // Check that upgrade to a package without instantiation_level_3 throws error
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithExtensions(vnfInstanceResponse,
                                                                             E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU,
                                                                             extensions);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("InstantiationLevelId: instantiation_level_3 not present in VNFD.");

        //Check that no scale details of the instance have changed.
        instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        List<HelmChart> helmChartsAfterRejectedUpgrade = vnfInstance.getHelmCharts();
        assertThat(helmChartsAfterRejectedUpgrade).isEqualTo(helmCharts);
        List<ScaleInfoEntity> scaleInfoEntityListAfterRejectedUpgrade = instance.getScaleInfoEntity();
        assertThat(scaleInfoEntityListAfterRejectedUpgrade).usingElementComparatorIgnoringFields("vnfInstance").isEqualTo(scaleInfoEntities);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void successfulInstantiateChangePkgFromNoLevelsToLevels() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end-no-levels-to-levels";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Check extensions and levels not set
        checkLevelsExtensionsReplicaDetailsNull(instance);

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "no-levels-to-levels");
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
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.INSTANTIATE,
                                                   InstantiationState.INSTANTIATED);

        //Upgrade to a package with levels
        result = requestHelper.getMvcResultChangeVnfpkgRequestWithComplexTypes(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU);

        lifeCycleOperationId = getLifeCycleOperationId(result);
        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);

        //Assertions on state of the operation and instance
        verificationHelper.verifyOperationAndModel(vnfInstanceResponse,
                                                   lifeCycleOperationId,
                                                   LifecycleOperationType.CHANGE_VNFPKG,
                                                   InstantiationState.INSTANTIATED);
        assertThat(instance.getResourceDetails()).isBlank();
        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(lifeCycleOperationId);

        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());

        //Check extensions set on the instance correctly
        Map<String, Object> vnfControlledScaling =
                (Map<String, Object>) convertStringToJSONObj(vnfInstance.getVnfInfoModifiableAttributesExtensions()).get(VNF_CONTROLLED_SCALING);
        assertThat(vnfControlledScaling.get(PAYLOAD)).isEqualTo(MANUAL_CONTROLLED); //CL PL
        assertThat(vnfControlledScaling.get(PAYLOAD_2)).isEqualTo(CISM_CONTROLLED); //JL
        assertThat(vnfControlledScaling.get(PAYLOAD_3)).isEqualTo(MANUAL_CONTROLLED); //TL

        //Check level is set on the instance correctly
        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo(INST_LEVEL_1);

        //Check ReplicaDetails of specific charts
        List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        HelmChart packageOne = getHelmChartByName(helmCharts, SAMPLE_HELM_1);

        final Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(packageOne);
        assertThat(replicaDetails).hasSize(1);

        //Check Payload ReplicaDetails
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

        //Check Payload_3 ReplicaDetails
        ReplicaDetails tlScaledVm = replicaDetails2.get(TL_SCALED_VM);
        assertThat(tlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(tlScaledVm.getAutoScalingEnabledValue()).isFalse();
        assertThat(tlScaledVm.getMaxReplicasCount()).isNull();
        assertThat(tlScaledVm.getMinReplicasCount()).isNull();

        //Check Payload_2 ReplicaDetails
        ReplicaDetails jlScaledVm = replicaDetails2.get(JL_SCALED_VM);
        assertThat(jlScaledVm.getCurrentReplicaCount()).isEqualTo(1);
        assertThat(jlScaledVm.getAutoScalingEnabledValue()).isTrue();
        assertThat(jlScaledVm.getMaxReplicasCount()).isEqualTo(1);
        assertThat(jlScaledVm.getMinReplicasCount()).isEqualTo(1);
    }

    @Test
    public void successfulInstantiateChangePackageInfoNoScalingWhenUpdateUsageState() throws Exception {
        addTopologyTemplateToDescriptor("no-scaling");

        //Create Identifier
        final String releaseName = "end-to-end-no-scaling-2";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);
        assertThat(instance.getMetadata()).isEqualTo("{\"tenantName\":\"ecm\"}");

        //Instantiate
        MvcResult result = requestHelper.getMvcResultInstantiateRequestAndVerifyAccepted(vnfInstanceResponse, "no-scaling-namespace-1");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                                               HelmReleaseState.COMPLETED,
                                                                               lifeCycleOperationId,
                                                                               HelmReleaseOperationType.INSTANTIATE,
                                                                               "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        List<String> instantiatedScaleIds = checkScaleEntityRecords(lifeCycleOperationId, 2);
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        assertThat(vnfInstance.getResourceDetails()).isNotNull();
        assertThat(vnfInstance.getPolicies()).isNotNull();
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_INSTANTIATE);

        //Upgrade - Completed with scaling
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, E2E_CHANGE_PACKAGE_INFO_VNFD_ID, false);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);

        List<String> upgradedScaleIds = checkScaleEntityRecords(lifeCycleOperationId, 2);
        assertThat(CollectionUtils.containsAny(upgradedScaleIds, instantiatedScaleIds)).isFalse();
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(upgradedVnfInstance.getOperationOccurrenceId()).isEqualTo(lifeCycleOperationId);
        verificationHelper.checkBroEndpointUrl(vnfInstanceResponse, CNF_BRO_URL_UPGRADE);

        //Upgrade - Completed no scaling
        result = requestHelper.getMvcResultChangeVnfpkgRequestAndVerifyAccepted(vnfInstanceResponse, "no-scaling", false);

        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        lifeCycleOperationId = getLifeCycleOperationId(result);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                                                   HelmReleaseState.COMPLETED,
                                                   lifeCycleOperationId,
                                                   HelmReleaseOperationType.CHANGE_VNFPKG,
                                                   "7");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        checkScaleEntityRecords(lifeCycleOperationId, 0);
        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        upgradedVnfInstance = operationAfterUpgrade.getVnfInstance();
        assertThat(upgradedVnfInstance.getResourceDetails()).isNull();
        assertThat(upgradedVnfInstance.getPolicies()).isNull();
        assertThat(upgradedVnfInstance.getScaleInfoEntity()).isEmpty();
        verify(onboardingClient, times(3)).put(any(), argThat(request -> request instanceof UsageStateRequest));
    }

    @Test
    public void successfulInstantiateChangePackageUpgradePatternAllowedYamlFile() throws Exception {

        //Create Identifier
        final String releaseName = "end-to-end-instantiate-upgarde-pattern-with-yaml";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);
        //Assertions on state of instance
        VnfInstance instantiateVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instantiateVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.NOT_INSTANTIATED);

        //Instantiate
        MvcResult instantiateResult = requestHelper.getMvcResultInstantiateRequestWithYamlFile(vnfInstanceResponse,
                "downsize-namespace-multi-with-values-1",
                EXISTING_CLUSTER_CONFIG_NAME,
                "valueFiles/instantiate.yaml");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, INSTANTIATE_URL_ENDING);
        String lifeCycleOperationAfterInstantiateId = getLifeCycleOperationId(instantiateResult);

        //Fake completion message
        HelmReleaseLifecycleMessage completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                lifeCycleOperationAfterInstantiateId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), false, COMPLETED);

        instantiateVnfInstance = vnfInstanceRepository.findByVnfInstanceId(instantiateVnfInstance.getVnfInstanceId());
        verifyValueInMap(instantiateVnfInstance.getCombinedValuesFile(),
                "eric-pm-server",
                "{server={persistentVolume={storageClass=erikube-rbd}}}"); //evnfm yaml value set during upgrade
        verifyValueInMap(instantiateVnfInstance.getCombinedValuesFile(),
                "extras",
                "{instantiate=during instantiation}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(instantiateVnfInstance.getCombinedValuesFile(), "upgradeParam", null); //non-evnfm additionalParam value set during upgrade

        //Upgrade
        MvcResult upgradeResult = requestHelper.getMvcResultChangeVnfpkgRequestWithYamlFile(vnfInstanceResponse,
                E2E_CHANGE_PACKAGE_UPDATE_PATTERN_INFO_VNFD_ID,
                false,
                "valueFiles/upgrade.yaml");
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 2, INSTANTIATE_URL_ENDING);
        verificationHelper.verifyNoEvnfmParamsPassedToWfs(restTemplate, 1, UPGRADE_URL_ENDING);
        String lifeCycleOperationAfterUpgradeId = getLifeCycleOperationId(upgradeResult);

        //Fake completion messages
        completed = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                lifeCycleOperationAfterUpgradeId,
                HelmReleaseOperationType.CHANGE_VNFPKG,
                "2");
        messageHelper.sendCompleteMessageForAllCnfCharts(completed, vnfInstanceResponse.getId(), true, COMPLETED);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationAfterUpgradeId, COMPLETED));

        LifecycleOperation operationAfterUpgrade = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationAfterUpgradeId);
        String operationAfterUpgradeOccId = operationAfterUpgrade.getOperationOccurrenceId();
        VnfInstance upgradeVnfInstance = operationAfterUpgrade.getVnfInstance();

        assertThat(operationAfterUpgrade.getUpgradePattern()).isNotEmpty();
        assertThat(operationAfterUpgrade.getUpgradePattern()).isEqualTo(
                "[{\"end-to-end-instantiate-upgarde-pattern-with-yaml-1\":\"upgrade\"}," +
                        "{\"end-to-end-instantiate-upgarde-pattern-with-yaml-2\":\"delete\"}," +
                        "{\"end-to-end-instantiate-upgarde-pattern-with-yaml-2\":\"delete_pvc\"}," +
                        "{\"end-to-end-instantiate-upgarde-pattern-with-yaml-2\":\"install\"}]");

        assertThat(operationAfterUpgrade.getOperationState()).isEqualTo(COMPLETED);
        Map<String, Object> valuesFile = parseJsonToGenericType(operationAfterUpgrade.getCombinedValuesFile(), new TypeReference<>() {
        });
        assertThat(valuesFile)
                .contains(Assertions.entry("override-key", "install"),
                        Assertions.entry("instantiate-key", "install"));

        //Assertion that it is a UPGRADE operation
        final Optional<ChangePackageOperationDetails> changePackageOperationDetails = changePackageOperationDetailsRepository.
                findById(operationAfterUpgradeOccId);
        if (changePackageOperationDetails.isPresent()) {
            assertThat(changePackageOperationDetails.get()
                    .getChangePackageOperationSubtype()
                    .equals(ChangePackageOperationSubtype.UPGRADE)).isTrue();
        } else {
            fail("missing changePackageOperationDetails");
        }

        // Check operation and history records have been updated as is successful upgrade
        verificationHelper.checkOperationValuesAndHistoryRecordsSet(operationAfterUpgradeOccId);

        final String[] upgradePattern = operationAfterUpgrade.getUpgradePattern().split(",");
        assertThat(upgradePattern).hasSize(4);
        assertThat(upgradePattern[0]).contains("upgrade");
        assertThat(upgradePattern[1]).contains("delete");
        assertThat(upgradePattern[2]).contains("delete_pvc");
        assertThat(upgradePattern[3]).contains("install");

        assertThat(upgradeVnfInstance.getHelmClientVersion()).isEqualTo("3.10");
        assertThat(upgradeVnfInstance.getInstantiationState()).isEqualTo(InstantiationState.INSTANTIATED);

        verifyValueInMap(upgradeVnfInstance.getCombinedValuesFile(),
                "eric-pm-server",
                "{server={persistentVolume={storageClass=network-block}, ingress={enabled=false}}, enabled=true}"); //evnfm yaml value set during upgrade
        verifyValueInMap(upgradeVnfInstance.getCombinedValuesFile(),
                "extras",
                "{instantiate=during instantiation}"); //non-evnfm yaml value set during instantiate
        verifyValueInMap(upgradeVnfInstance.getCombinedValuesFile(), "upgradeParam", "testing value"); //non-evnfm additionalParam value set during upgrade

        // First Command: Chart 1 - Upgrade
        final HelmReleaseLifecycleMessage firstChartStage2 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                lifeCycleOperationAfterUpgradeId,
                HelmReleaseOperationType.CHANGE_VNFPKG, "2");

        messageHelper.sendMessageForChart(firstChartStage2,
                firstHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                false,
                HelmReleaseState.COMPLETED);

        // Second Command: Chart 2 - Terminate/Delete
        final HelmReleaseLifecycleMessage secondChartStage1 = getHelmReleaseLifecycleMessage(secondHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                operationAfterUpgradeOccId,
                HelmReleaseOperationType.TERMINATE,
                "1");

        messageHelper.sendMessageForChart(secondChartStage1,
                secondHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                false,
                HelmReleaseState.COMPLETED);

        // Third Command: Chart 2 - Delete Pvc
        final WorkflowServiceEventMessage secondChartStage2 = getWfsEventMessage(secondHelmReleaseNameFor(releaseName),
                WorkflowServiceEventStatus.COMPLETED,
                operationAfterUpgradeOccId,
                WorkflowServiceEventType.DELETE_PVC);

        messageHelper.sendMessageForChart(secondChartStage2,
                secondHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                false,
                HelmReleaseState.COMPLETED);

        // Fourth Command: Chart 2 - Install
        final HelmReleaseLifecycleMessage secondChartStage3 = getHelmReleaseLifecycleMessage(firstHelmReleaseNameFor(releaseName),
                HelmReleaseState.COMPLETED,
                operationAfterUpgradeOccId,
                HelmReleaseOperationType.INSTANTIATE,
                "1");
        messageHelper.sendMessageForChart(firstChartStage2,
                firstHelmReleaseNameFor(releaseName),
                vnfInstanceResponse.getId(),
                false,
                HelmReleaseState.COMPLETED);

        verify(upgradeService, Mockito.times(1))
                .execute(any());

        verify(terminateService, Mockito.times(1))
                .execute(any(), any(), anyBoolean());
    }

    private void addTopologyTemplateToDescriptor(final String vnfdId) {
        final PackageResponse packageResponse = packageService.getPackageInfo(vnfdId);
        final JSONObject vnfd = packageService.getVnfd(vnfdId);
        vnfd.put("topology_template", createTopologyTemplate());
        packageResponse.setDescriptorModel(vnfd.toString());

        when(packageService.getPackageInfoWithDescriptorModel(vnfdId)).thenReturn(packageResponse);
    }

    private void checkLevelsExtensionsReplicaDetailsNull(final VnfInstance instance) {
        //Check extensions and levels not set
        assertThat(instance.getInstantiationLevel()).isNull();
        assertThat(instance.getVnfInfoModifiableAttributesExtensions()).isNull();
        //Check charts do not contain replicaDetails
        List<HelmChart> charts = instance.getHelmCharts();
        assertThat(charts).extracting("replicaDetails").containsOnly(null, null);
    }

    private List<String> checkScaleEntityRecords(String lifeCycleOperationId, final int expectedSize) {
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        VnfInstance vnfInstance = operation.getVnfInstance();
        List<ScaleInfoEntity> preScaleInfoEntities = scaleInfoRepository.findAllByVnfInstance(vnfInstance);
        assertThat(vnfInstance.getScaleInfoEntity()).hasSize(expectedSize);
        return preScaleInfoEntities.stream().map(ScaleInfoEntity::getScaleInfoId).collect(Collectors.toList());
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
