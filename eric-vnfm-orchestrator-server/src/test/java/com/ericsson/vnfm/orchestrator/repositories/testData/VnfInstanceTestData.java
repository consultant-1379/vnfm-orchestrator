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
package com.ericsson.vnfm.orchestrator.repositories.testData;

import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildExpectedLifecycleOperationsWithAllFields;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildLifecycleOperations;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.toTerminateHelmChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;

public class VnfInstanceTestData {
    public static Map<String, VnfInstance> buildVnfInstances() {
        final VnfInstance firstVnfInstance = buildFirstVnfInstance();
        final VnfInstance secondVnfInstance = buildSecondVnfInstance();
        final VnfInstance thirdVnfInstance = buildThirdVnfInstance();

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", thirdVnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForSorting() {
        VnfInstance firstVnfInstance = buildFirstVnfInstance();
        firstVnfInstance.setAllOperations(new ArrayList<>());
        final VnfInstance secondVnfInstance = buildSecondVnfInstance();
        secondVnfInstance.setAllOperations(new ArrayList<>());
        final VnfInstance thirdVnfInstance = buildThirdVnfInstance();
        thirdVnfInstance.setAllOperations(new ArrayList<>());

        Map<String, VnfInstance> vnfInstances = new LinkedHashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", thirdVnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForSpecification() {
        VnfInstance firstVnfInstance = buildFirstVnfInstanceWithAllFields();
        final VnfInstance secondVnfInstance = buildSecondVnfInstanceWithAllFields();
        final VnfInstance thirdVnfInstance = buildThirdVnfInstanceWithAllFields();

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", thirdVnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForSpecificationAndSort() {
        final VnfInstance firstVnfInstance = buildFirstVnfInstanceWithAllFields();
        resetVnfInstanceAssociations(firstVnfInstance);
        final VnfInstance secondVnfInstance = buildSecondVnfInstanceWithAllFields();
        resetVnfInstanceAssociations(secondVnfInstance);
        final VnfInstance thirdVnfInstance = buildThirdVnfInstanceWithAllFields();
        resetVnfInstanceAssociations(thirdVnfInstance);

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", thirdVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForPagination() {
        final VnfInstance firstVnfInstance = buildFirstVnfInstance();
        firstVnfInstance.setAllOperations(new ArrayList<>());
        final VnfInstance secondVnfInstance = buildSecondVnfInstance();
        secondVnfInstance.setAllOperations(new ArrayList<>());

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForPaginationAndSpecification() {
        final VnfInstance vnfInstance = buildThirdVnfInstanceWithAllFields();
        resetVnfInstanceAssociations(vnfInstance);

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", vnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesForPaginationAndSpecificationWithAssociations() {
        final VnfInstance vnfInstance = buildThirdVnfInstanceWithAllFields();
        final List<LifecycleOperation> lifecycleOperations = buildLifecycleOperations("354", "355");
        vnfInstance.setAllOperations(lifecycleOperations);
        vnfInstance.setScaleInfoEntity(new ArrayList<>());

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a252", vnfInstance);

        return vnfInstances;
    }

    public static Map<String, VnfInstance> buildVnfInstancesWithAllFields() {
        final VnfInstance firstVnfInstance = buildFirstVnfInstanceWithAllFields();
        final VnfInstance secondVnfInstance = buildSecondVnfInstanceWithAllFields();

        Map<String, VnfInstance> vnfInstances = new HashMap<>();
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a250", firstVnfInstance);
        vnfInstances.put("d3def1ce-4cf4-477c-aab3-21c454e6a251", secondVnfInstance);

        return vnfInstances;
    }

    public static VnfInstance buildVnfInstanceByIdWithAllFields(String vnfInstanceId) {
        final Map<String, VnfInstance> vnfInstances = buildVnfInstancesWithAllFields();
        return vnfInstances.get(vnfInstanceId);
    }

    private static VnfInstance buildFirstVnfInstanceWithAllFields() {
        final List<LifecycleOperation> lifecycleOperations = buildExpectedLifecycleOperationsWithAllFields("350", "351");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("450", "451");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("650",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a350",
                                                                                         "651",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a351" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("550", "551");

        VnfInstance vnfInstance = buildVnfInstance("250",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a350");
        initializeEncryptedFields(vnfInstance, "250");

        return vnfInstance;
    }

    private static VnfInstance buildSecondVnfInstanceWithAllFields() {
        final List<LifecycleOperation> lifecycleOperations = buildExpectedLifecycleOperationsWithAllFields("352", "353");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("452", "453");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("652",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a352",
                                                                                         "653",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a353" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("552", "553");

        VnfInstance vnfInstance = buildVnfInstance("251",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a352");
        initializeEncryptedFields(vnfInstance, "251");

        return vnfInstance;
    }

    private static VnfInstance buildThirdVnfInstanceWithAllFields() {
        final List<LifecycleOperation> lifecycleOperations = buildExpectedLifecycleOperationsWithAllFields("354", "355");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("454", "455");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("654",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a354",
                                                                                         "655",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a355" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("554", "555");

        VnfInstance vnfInstance = buildVnfInstance("252",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a354");
        initializeEncryptedFields(vnfInstance, "252");

        return vnfInstance;
    }

    private static void initializeEncryptedFields(VnfInstance vnfInstance, String idSuffix) {
        vnfInstance.setOssTopology("testOssTopology" + idSuffix);
        vnfInstance.setInstantiateOssTopology("{\"testInstantiateOssTopology" + idSuffix + "\":\"test\"}");
        vnfInstance.setAddNodeOssTopology("testAddNodeOssTopology" + idSuffix);
        vnfInstance.setCombinedValuesFile("testCombinedValuesFile" + idSuffix);
        vnfInstance.setTempInstance("testTempInstance" + idSuffix);
        vnfInstance.setSitebasicFile("testSitebasicFile" + idSuffix);
        vnfInstance.setOssNodeProtocolFile("testOssNodeProtocolFile" + idSuffix);
        vnfInstance.setSensitiveInfo("testSensitiveInfo" + idSuffix);
    }

    private static VnfInstance buildFirstVnfInstance() {
        final List<LifecycleOperation> lifecycleOperations = buildLifecycleOperations("350", "351");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("450", "451");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("650",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a350",
                                                                                         "651",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a351" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("550", "551");
        VnfInstance vnfInstance = buildVnfInstance("250",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a350");
        return vnfInstance;
    }

    private static VnfInstance buildSecondVnfInstance() {
        final List<LifecycleOperation> lifecycleOperations = buildLifecycleOperations("352", "353");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("452", "453");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("652",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a352",
                                                                                         "653",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a353" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("552", "553");
        VnfInstance vnfInstance = buildVnfInstance("251",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a352");
        return vnfInstance;
    }

    private static VnfInstance buildThirdVnfInstance() {
        final List<LifecycleOperation> lifecycleOperations = buildLifecycleOperations("354", "355");
        final List<OperationDetail> supportedOperations = buildSupportedOperations();
        final List<HelmChart> helmCharts = buildHelmCharts("454", "455");
        final List<TerminatedHelmChart> terminatedHelmCharts = buildTerminatedHelmCharts("654",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a354",
                                                                                         "655",
                                                                                         "d3def1ce-4cf4-477c-aab3-21c454e6a355" );
        final List<ScaleInfoEntity> scaleInfoEntities = buildScaleInfoEntity("554", "555");
        VnfInstance vnfInstance = buildVnfInstance("252",
                                                   supportedOperations,
                                                   lifecycleOperations,
                                                   helmCharts,
                                                   terminatedHelmCharts,
                                                   scaleInfoEntities);
        vnfInstance.setOperationOccurrenceId("d3def1ce-4cf4-477c-aab3-21c454e6a354");
        return vnfInstance;
    }

    private static VnfInstance buildVnfInstance(String idSuffix,
                                                List<OperationDetail> supportedOperations,
                                                List<LifecycleOperation> lifecycleOperations,
                                                List<HelmChart> helmCharts,
                                                List<TerminatedHelmChart> terminatedHelmCharts,
                                                List<ScaleInfoEntity> scaleInfoEntities) {

        return VnfInstance.builder()
                .vnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a" + idSuffix)
                .vnfInstanceName("testVnfInstanceName" + idSuffix)
                .vnfInstanceDescription("testVnfInstanceDescription" + idSuffix)
                .vnfDescriptorId("testVnfInstanceDescriptorId" + idSuffix)
                .vnfProviderName("testVnfProviderName" + idSuffix)
                .vnfProductName("testVnfProductName" + idSuffix)
                .vnfSoftwareVersion("testVnfSoftwareVersion" + idSuffix)
                .vnfdVersion("testVnfdVersion" + idSuffix)
                .vnfPackageId("testVnfPackageId" + idSuffix)
                .instantiationState(InstantiationState.NOT_INSTANTIATED)
                .clusterName("testClusterName" + idSuffix)
                .namespace("testNamespace" + idSuffix)
                .supportedOperations(supportedOperations)
                .addedToOss(true)
                .combinedAdditionalParams("testCombinedAdditionalParams" + idSuffix)
                .policies("testPolicies" + idSuffix)
                .resourceDetails("testResourceDetails" + idSuffix)
                .manoControlledScaling(true)
                .overrideGlobalRegistry(true)
                .metadata("{\"testMetadata" + idSuffix + "\":\"test\"}")
                .alarmSupervisionStatus("testAlarmSupervisionStatus" + idSuffix)
                .cleanUpResources(true)
                .isHealSupported(true)
                .broEndpointUrl("testBroEndpointUrl" + idSuffix)
                .vnfInfoModifiableAttributesExtensions("{\"testVnfInfoModifiableAttributesExtensions" + idSuffix + "\":\"test\"}")
                .instantiationLevel("testInstantiationLevel" + idSuffix)
                .crdNamespace("testCrdNamespace" + idSuffix)
                .isRel4(true)
                .helmClientVersion("testHelmClientVersion" + idSuffix)
                .allOperations(lifecycleOperations)
                .helmCharts(helmCharts)
                .terminatedHelmCharts(terminatedHelmCharts)
                .scaleInfoEntity(scaleInfoEntities)
                .build();
    }

    private static List<OperationDetail> buildSupportedOperations() {
        List<OperationDetail> supportedOperations = new ArrayList<>();
        supportedOperations.add(OperationDetail.ofSupportedOperation("instantiate"));
        supportedOperations.add(OperationDetail.ofSupportedOperation("terminate"));
        supportedOperations.add(OperationDetail.ofSupportedOperation("heal"));
        supportedOperations.add(OperationDetail.ofSupportedOperation("change_package"));
        supportedOperations.add(OperationDetail.ofSupportedOperation("scale"));
        return supportedOperations;
    }

    private static List<HelmChart> buildHelmCharts(String firstIdSuffix, String secondIdSuffix) {
        List<HelmChart> helmCharts = new ArrayList<>();

        HelmChart firstHelmChart = buildHelmChart(firstIdSuffix);

        HelmChart secondHelmChart = buildHelmChart(secondIdSuffix);
        secondHelmChart.setPriority(0);
        secondHelmChart.setRetryCount(2);

        helmCharts.add(firstHelmChart);
        helmCharts.add(secondHelmChart);

        return helmCharts;
    }

    private static List<TerminatedHelmChart> buildTerminatedHelmCharts(String firstIdSuffix,
                                                                       String firstOperationOccurrenceId,
                                                                       String secondIdSuffix,
                                                                       String secondOperationOccurrenceId) {

        List<TerminatedHelmChart> terminatedHelmCharts = new ArrayList<>();

        final TerminatedHelmChart firstTerminatedHelmChart = buildTerminatedHelmChart(firstIdSuffix, firstOperationOccurrenceId);

        final TerminatedHelmChart secondTerminatedHelmChart = buildTerminatedHelmChart(secondIdSuffix, secondOperationOccurrenceId);
        secondTerminatedHelmChart.setPriority(0);
        secondTerminatedHelmChart.setRetryCount(2);

        terminatedHelmCharts.add(firstTerminatedHelmChart);
        terminatedHelmCharts.add(secondTerminatedHelmChart);

        return terminatedHelmCharts;
    }

    private static HelmChart buildHelmChart(String idSuffix) {
        return HelmChart.builder()
                .id("d3def1ce-4cf4-477c-aab3-21c454e6a" + idSuffix)
                .helmChartName("testHelmChartName" + idSuffix)
                .helmChartVersion("testHelmChartVersion" + idSuffix)
                .helmChartType(HelmChartType.CNF)
                .helmChartArtifactKey("testHelmChartArtifactKey" + idSuffix)
                .helmChartUrl("testHelmChartUrl" + idSuffix)
                .priority(1)
                .releaseName("testReleaseName" + idSuffix)
                .revisionNumber("testRevisionNumber" + idSuffix)
                .state("testState" + idSuffix)
                .retryCount(3)
                .deletePvcState("testDeletePvcState" + idSuffix)
                .downsizeState("testDownsizeState" + idSuffix)
                .replicaDetails("testReplicaDetails" + idSuffix)
                .build();
    }

    private static TerminatedHelmChart buildTerminatedHelmChart(final String idSuffix, final String operationOccurrenceId) {
        LifecycleOperation opeartion = new LifecycleOperation();
        opeartion.setOperationOccurrenceId(operationOccurrenceId);

        final TerminatedHelmChart terminateHelmChart = toTerminateHelmChart(buildHelmChart(idSuffix), opeartion);
        terminateHelmChart.setId("d3def1ce-4cf4-477c-aab3-21c454e6a" + idSuffix);
        terminateHelmChart.setState("testState" + idSuffix);

        return terminateHelmChart;
    }

    private static List<ScaleInfoEntity> buildScaleInfoEntity(String firstIdSuffix, String secondIdSuffix) {
        List<ScaleInfoEntity> scaleInfoEntities = new ArrayList<>();
        ScaleInfoEntity firstScaleInfo = ScaleInfoEntity.builder()
                .scaleInfoId("d3def1ce-4cf4-477c-aab3-21c454e6a" + firstIdSuffix)
                .aspectId("Aspect1")
                .scaleLevel(0)
                .build();
        ScaleInfoEntity secondScaleInfo = ScaleInfoEntity.builder()
                .scaleInfoId("d3def1ce-4cf4-477c-aab3-21c454e6a" + secondIdSuffix)
                .aspectId("Aspect3")
                .scaleLevel(2)
                .build();

        scaleInfoEntities.add(firstScaleInfo);
        scaleInfoEntities.add(secondScaleInfo);

        return scaleInfoEntities;
    }

    private static void resetVnfInstanceAssociations(VnfInstance vnfInstance) {
        vnfInstance.setAllOperations(new ArrayList<>());
        vnfInstance.setHelmCharts(new ArrayList<>());
        vnfInstance.setScaleInfoEntity(new ArrayList<>());
    }
}