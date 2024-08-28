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
package com.ericsson.vnfm.orchestrator;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.PaginationLinks;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.json.JSONObject;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public final class TestUtils {
    public static final String INSTANCE_ID = "instanceID";
    public static final String DUMMY_INSTANCE_ID = "dummy_instance_id";
    public static final String DUMMY_INSTANCE_NAME = "dummy_instance_name";
    public static final String DUMMY_INSTANCE_DESCRIPTION = "dummy description";
    public static final String DUMMY_DESCRIPTOR_ID = "dummy_descriptor_id";
    public static final String DUMMY_PROVIDER_NAME = "dummy_provider_name";
    public static final String DUMMY_PRODUCT_NAME = "dummy_product_name";
    public static final String DUMMY_NAMESPACE = "dummy_namespace";
    public static final String DUMMY_RELEASE_NAME = "dummy-release";
    public static final String DUMMY_VNF_SOFTWARE_VERSION = "dummy_vnf_software_version";
    public static final String DUMMY_VNFD_VERSION = "dummy_vnfd_version";
    public static final String DUMMY_SCALE_VNF_INFO = "[{\"aspectId\":\"processing\",\"vnfdId\":null,\"scaleLevel\":3}]";
    public static final String DUMMY_SCALE_VNF_INFO_ENTITY = "[{\"aspectId\":\"processing\",\"scaleLevel\":3}]";
    public static final String EXTENSIONS = "{\"vnfControlledScaling\":{\"Aspect1\":\"ManualControlled\"}}";
    public static final String DEFAULT_CLUSTER_NAME = "hall914.config";
    public static final String CLEANUP_CLUSTER_NAME = "cleanup-cluster";
    public static final String DUMMY_CLUSTER_NAME = "hart070";
    public static final String DUMMY_HELM_CLIENT_VERSION = "3.8";
    public static final String ONBOARDING_PATH = "/api/vnfpkgm/v1/vnf_packages";
    public static final String ONBOARDING_QUERY_VALUE = "(eq,vnfdId,%s)";
    public static final String ONBOARDING_LOCAL_PARAM_NAME = "onboardingConfig";
    public static final String LIFECYCLE_OPERATION_PARAMS_JSON = "lifecycleOperationWithDay0ConfigurationParams.json";
    public static final String LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON = "lifecycleOperationWithoutAdditionalParameter.json";
    public static final String ADDITIONAL_PARAMS_FIELD = "additionalParams";
    public static final String REPO_DOWNGRADE_SUPPORTED_VNF_ID = "downgrade-3b-40b7-ab48-dd15d88332a7";
    public static final String REPO_DOWNGRADE_SUPPORTED_VNF_PACKAGE_ID = "9392468011745350001-DOWNGRADE";
    public static final String REPO_DOWNGRADE_NOT_SUPPORTED_NO_POLICY_VNF_ID = "downgrade-3b-40b7-ab48-dd15d67103a7";
    public static final String REPO_DOWNGRADE_NOT_SUPPORTED_VNFD_ID = "no-downgrade-40b7-ab48-dd15d88332a7";
    public static final String REPO_DOWNGRADE_NO_PACKAGE_VNF_ID = "downgrade-no-package-ab48-dd15d88332a7";
    public static final String STUB_DOWNGRADE_SUPPORTED_SOURCE_VNFD_ID = "ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5";
    public static final String STUB_DOWNGRADE_SUPPORTED_TARGET_VNFD_ID = "b1bb0ce7-ebca-4fa7-95ed-4840d70a1177";
    public static final String STUB_PACKAGE_VERSION = "cxp9025898_4r81e08";
    public static final String STUB_PACKAGE_ID_BY_FILTER = "d3def1ce-4cf4-477c-aab3-21cb04e6a379";
    public static final String STUB_VNFD_ID = "d3def1ce-4cf4-477c-aab3-21cb04e6a378";
    public static final int STUB_VNFD_CHARTS_QUANTITY = 2;
    public static final String STUB_UPDATE_USAGE_STATE_VNF_ID = "a1def1ce-4cf4-477c-aab3-21cb04e6a379";
    public static final String BRO_ENDPOINT_URL = "bro_endpoint_url";
    public static final String CNF_BRO_URL_INSTANTIATE = "http://bro-service-url.test:8080";
    public static final String CNF_BRO_URL_UPGRADE = "http://bro-service-url-upgrade.test:8080";

    public static final String E2E_INSTANTIATE_PACKAGE_VNFD_ID = "35570c3e-3cc0-4ec4-83e9-5280ffa3aee0";
    public static final String E2E_CHANGE_PACKAGE_UPDATE_PATTERN_INFO_VNFD_ID = "4c096964-69e7-11ee-8c99-0242ac120002";
    public static final String E2E_CHANGE_PACKAGE_INFO_VNFD_ID = "3d02c5c9-7a9b-48da-8ceb-46fcc83f584c";
    public static final String E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FOR_ROLLBACK = "3d02c5c9-7a9b-48da-8ceb-46fcc83f694c";
    public static final String E2E_CHANGE_PACKAGE_INFO_VNFD_ID_FAILED = "4d02c5c9-7a9b-48da-8ceb-46fcc83f585c";
    public static final String E2E_INSTANTIATE_PACKAGE_VNFD_ID_FOR_ROLLBACK = "35570c3e-3cc0-4ec4-83e9-5280ffa3aff1";
    public static final String E2E_INSTANTIATE_PACKAGE_WITH_LEVELS_NO_VDU_VNFD_ID = "levels-no-vdu";
    public static final String E2E_INSTANTIATE_PACKAGE_NO_SCALING_MAPPING_VNFD_ID = "levels-no-vdu-no-scaling-mapping";
    public static final String E2E_CHANGE_PACKAGE_INFO_WITH_LEVELS_NO_VDU = "upgrade-no-vdu-levels";

    public static final String TL_SCALED_VM = "TL_scaled_vm";
    public static final String PL_SCALED_VM = "PL__scaled_vm";
    public static final String CL_SCALED_VM = "CL_scaled_vm";
    public static final String JL_SCALED_VM = "JL_scaled_vm";
    public static final String PAYLOAD = "Payload";
    public static final String PAYLOAD_2 = "Payload_2";
    public static final String PAYLOAD_3 = "Payload_3";
    public static final String SAMPLE_HELM_1 = "sample-helm1";
    public static final String SAMPLE_HELM_2 = "sample-helm2";
    public static final String INST_LEVEL_1 = "instantiation_level_1";
    public static final String INST_LEVEL_2 = "instantiation_level_2";
    public static final String INST_LEVEL_3 = "instantiation_level_3";
    public static final String MANUAL_CONTROLLED = "ManualControlled";
    public static final String CISM_CONTROLLED = "CISMControlled";
    public static final String VNF_CONTROLLED_SCALING = "vnfControlledScaling";

    public static final String NEXTPAGE_OPAQUE_MARKER = "nextpage_opaque_marker";
    public static final String NEXTPAGE_OPAQUE_MARKER_QUERY = NEXTPAGE_OPAQUE_MARKER + "=%s";
    public static final String PAGE = "page";
    public static final String PAGE_QUERY = PAGE + "=%s";
    public static final String NUMBER = "number";
    public static final String SIZE = "size";
    public static final String TOTAL_PAGES = "totalPages";
    public static final String TOTAL_ELEMENTS = "totalElements";
    public static final String FIRST = "first";
    public static final String PREVIOUS = "previous";
    public static final String SELF = "self";
    public static final String NEXT = "next";
    public static final String LAST = "last";
    public static final String QUERY_PARAMETER_EXCEPTION = "Invalid Pagination Query Parameter Exception";
    public static final String ERROR_BODY_SORT_COLUMN = "Invalid column value for sorting:: %s. Acceptable values are :: [";
    public static final String ERROR_BODY_SORT_ORDER = "Invalid sorting values :: %s. Acceptable values are :: 'desc' or 'asc' (case insensitive)";

    public static String INSTANTIATE_URL_ENDING = "/instantiate";
    public static String UPGRADE_URL_ENDING = "/upgrade";
    private static final String CRD_PACKAGE_KEY = "crd_package";
    private static final String HELM_PACKAGE_KEY = "helm_package";

    private TestUtils() {
    }

    public static Path getResource(String fileToLocate) throws URISyntaxException {
        return Paths.get(Resources.getResource(fileToLocate).toURI());
    }

    public static Path getValuesFileCopy(String fileToLocate) throws Exception {
        String content = YamlUtility.getValuesString(getResource(fileToLocate)).orElse(null);
        return YamlUtility.writeStringToValuesFile(content);
    }

    /**
     * Returns a path to a file using {@link Class#getResource(String)}.
     * This method allows to find resources relative to the {@link Class}'s package.
     */
    public static Path getResource(final Class<?> testClass, final String fileName) {
        final var resource = testClass.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException(format("Could not find file %s", fileName));
        }

        return Paths.get(resource.getPath());
    }

    public static String readDataFromFile(String fileName) throws IOException, URISyntaxException {
        try (Stream<String> lines = Files.lines(getResource(fileName))) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    public static String readDataFromFile(Path filePath) throws IOException {
        try (Stream<String> lines = Files.lines(filePath)) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    /**
     * Returns a file using {@link Class#getResource(String)}.
     * This method allows to load resources relative to the {@link Class}'s package.
     */
    public static String readDataFromFile(final Class<?> testClass, String fileName) {
        try {
            return readDataFromFile(getResource(testClass, fileName));
        } catch (final IOException e) {
            throw new IllegalArgumentException(format("Could not read file %s", fileName), e);
        }
    }

    public static String parseJsonFile(String filename) {
        String jsonResponseBody = "";
        try {
            jsonResponseBody = Resources.toString(
                    Resources.getResource(filename), Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResponseBody;
    }

    public static LifecycleOperation createDummyLifecycleOperation(String fromJsonFile) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        if (fromJsonFile != null) {
            lifecycleOperation.setOperationParams(TestUtils.parseJsonFile(fromJsonFile));
        }
        lifecycleOperation.setVnfInstance(createDummyInstance(Collections.emptyList(), InstantiationState.INSTANTIATED));
        lifecycleOperation.getVnfInstance().setAllOperations(Collections.singletonList(lifecycleOperation));
        lifecycleOperation.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        lifecycleOperation.setOperationState(LifecycleOperationState.COMPLETED);
        return lifecycleOperation;
    }

    public static LifecycleOperation createLifecycleOperation(VnfInstance vnfInstance, LifecycleOperationType lifecycleOperationType,
                                                              LifecycleOperationState operationState) {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setVnfInstance(vnfInstance);
        lifecycleOperation.getVnfInstance().setAllOperations(Collections.singletonList(lifecycleOperation));
        lifecycleOperation.setLifecycleOperationType(lifecycleOperationType);
        lifecycleOperation.setOperationState(operationState);
        return lifecycleOperation;
    }

    public static VnfInstance createDummyInstance(final List<ScaleInfoEntity> instantiateVnfInfo,
                                                  final InstantiationState instantiationState) {
        return createDummyInstance(DUMMY_INSTANCE_ID, DUMMY_INSTANCE_NAME,
                DUMMY_INSTANCE_DESCRIPTION, DUMMY_DESCRIPTOR_ID, DUMMY_PROVIDER_NAME, DUMMY_PRODUCT_NAME,
                DUMMY_VNF_SOFTWARE_VERSION, DUMMY_VNFD_VERSION, DUMMY_DESCRIPTOR_ID, instantiateVnfInfo,
                instantiationState, DUMMY_HELM_CLIENT_VERSION);
    }

    public static VnfInstance createDummyInstanceWithHelmChart(String... releaseNames) {
        VnfInstance vnfInstance = createDummyInstance(null, InstantiationState.NOT_INSTANTIATED);
        vnfInstance.setClusterName(DUMMY_CLUSTER_NAME);
        List<HelmChart> helmCharts = new ArrayList<>();
        for (String releaseName : releaseNames) {
            HelmChart helmChart = new HelmChart();
            helmChart.setReleaseName(releaseName);
            helmCharts.add(helmChart);
        }
        vnfInstance.setHelmCharts(helmCharts);
        return vnfInstance;
    }

    public static VnfInstance createDummyInstance(String vnfInstanceId, String vnfInstanceName, String vnfInstanceDescription,
                                                  String vnfDescriptorId, String vnfProviderName, String vnfProductName, String vnfSoftwareVersion,
                                                  String vnfdVersion, String vnfPackageId, List<ScaleInfoEntity> scaleInfoEntityList,
                                                  final InstantiationState instantiationState, String helmClientVersion) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(vnfInstanceId);
        vnfInstance.setVnfInstanceName(vnfInstanceName);
        vnfInstance.setVnfInstanceDescription(vnfInstanceDescription);
        vnfInstance.setVnfDescriptorId(vnfDescriptorId);
        vnfInstance.setVnfProductName(vnfProductName);
        vnfInstance.setVnfSoftwareVersion(vnfSoftwareVersion);
        vnfInstance.setVnfProviderName(vnfProviderName);
        vnfInstance.setVnfdVersion(vnfdVersion);
        vnfInstance.setVnfPackageId(vnfPackageId);
        vnfInstance.setScaleInfoEntity(scaleInfoEntityList);
        vnfInstance.setInstantiationState(instantiationState);
        vnfInstance.setOssTopology(new JSONObject(new HashMap<>()).toString());
        Map<String, Object> values = new HashMap<>();
        values.put("Aspect1", "ManualControlled");
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("vnfControlledScaling", values);
        vnfInstance.setMetadata("{ \"tenantName\": \"ecm\" }");
        vnfInstance.setVnfInfoModifiableAttributesExtensions(Utility.convertObjToJsonString(extensions));
        vnfInstance.setHelmClientVersion(helmClientVersion);
        return vnfInstance;
    }

    public static void createHelmChart(final VnfInstance instance) {
        String helmChartUrl = "https://testing/testing1.tgz";
        HelmChart helmChart = getHelmChart(helmChartUrl, instance.getVnfInstanceName(), instance, 1, null);
        helmChart.setState("COMPLETED");

        Map<String, ReplicaDetails> replicaDetailsList = new HashMap<>();
        replicaDetailsList.put("CL_scaled_vm",
                ReplicaDetails.builder()
                        .withScalingParameterName("CL_scaled_vm")
                        .withMaxReplicasParameterName("maxName")
                        .withMinReplicasParameterName("minName")
                        .withAutoScalingEnabledValue(false)
                        .withCurrentReplicaCount(2)
                        .withMinReplicasCount(2)
                        .build());
        replicaDetailsList.put("PL__scaled_vm", ReplicaDetails.builder()
                .withScalingParameterName("PL__scaled_vm")
                .withMaxReplicasParameterName("maxName")
                .withMinReplicasParameterName("minName")
                .withAutoScalingEnabledValue(false)
                .withCurrentReplicaCount(2)
                .build());
        helmChart.setReplicaDetails(Utility.convertObjToJsonString(replicaDetailsList));
        List<HelmChart> charts = new ArrayList<>();
        charts.add(helmChart);
        instance.setHelmCharts(charts);
    }

    public static VnfInstance getVnfInstance() {
        final VnfInstance instance = createDummyInstance(null, "dummy-instance-name",
                null, "dummy-descriptor-id", "dummy-provider-name",
                "dummy-product-name", "dummy-software-version",
                "dummy-vnfd-version", "dummy-package-id", null, null,
                DUMMY_HELM_CLIENT_VERSION);
        HelmChart chart = getHelmChart("test", DUMMY_RELEASE_NAME, instance, 1, HelmChartType.CNF);
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(chart);
        instance.setHelmCharts(helmCharts);
        instance.setTerminatedHelmCharts(new ArrayList<>());
        instance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        return instance;
    }

    public static HelmChart getHelmChart(String chartUrl, String releaseName, VnfInstance instance, int priority, HelmChartType chartType) {
        HelmChart chart = new HelmChart();
        chart.setHelmChartUrl(chartUrl);
        chart.setHelmChartName(releaseName);
        chart.setReleaseName(releaseName);
        chart.setHelmChartArtifactKey(releaseName);
        chart.setVnfInstance(instance);
        chart.setPriority(priority);
        chart.setHelmChartType(chartType);
        return chart;
    }

    public static ScaleVnfRequest createScaleRequest(String aspectId, int numberOfSteps,
                                                     ScaleVnfRequest.TypeEnum scale) {
        ScaleVnfRequest request = new ScaleVnfRequest();
        request.setAspectId(aspectId);
        request.setNumberOfSteps(numberOfSteps);
        request.setType(scale);
        return request;
    }

    public static ScaleInfoEntity createScaleInfoEntity(final VnfInstance vnfInstance, final String aspectId, final int scaleLevel) {
        ScaleInfoEntity tempScaleInfo = new ScaleInfoEntity();
        tempScaleInfo.setAspectId(aspectId);
        tempScaleInfo.setScaleLevel(scaleLevel);
        tempScaleInfo.setVnfInstance(vnfInstance);
        return tempScaleInfo;
    }

    public static InstantiateVnfRequest createInstantiateRequest(String clusterName,
                                                                 Map<String, Object> additionalParam) {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        request.setClusterName(clusterName);
        request.setAdditionalParams(additionalParam);
        return request;
    }

    public static ChangeCurrentVnfPkgRequest createUpgradeRequest(String vnfdId,
                                                                  Map<String, Object> additionalParam) {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        request.setVnfdId(vnfdId);
        request.setAdditionalParams(additionalParam);
        return request;
    }

    public static TerminateVnfRequest createTerminateRequest(Map<String, Object> additionalParam) {
        TerminateVnfRequest request = new TerminateVnfRequest();
        request.setTerminationType(TerminateVnfRequest.TerminationTypeEnum.FORCEFUL);
        request.setAdditionalParams(additionalParam);
        return request;
    }

    public static WorkflowRoutingResponse createResponse(String instanceId, String errorMessage,
                                                         HttpStatus httpStatus) {
        final WorkflowRoutingResponse workflowRoutingResponse = new WorkflowRoutingResponse();
        Map<String, String> links = new HashMap<>();
        links.put("dummy", "dummy-link");
        workflowRoutingResponse.setHttpStatus(httpStatus);
        workflowRoutingResponse.setLinks(links);
        workflowRoutingResponse.setInstanceId(instanceId);
        workflowRoutingResponse.setErrorMessage(errorMessage);
        return workflowRoutingResponse;
    }

    public static Map<String, Object> getComplexTypeAdditionalParams() {
        List<String> listTypeValue = Arrays.asList("string1", "string2");
        Map<String, Object> mapTypeValue = new HashMap<>(2);
        mapTypeValue.put("key1", "value1");
        mapTypeValue.put("key2", "value2");
        Map<String, Object> mapOfListsValue = new HashMap<>(2);
        mapOfListsValue.put("key1", listTypeValue);
        mapOfListsValue.put("key2", listTypeValue);
        Map<String, Object> mapOfMapsValue = new HashMap<>(2);
        mapOfMapsValue.put("key1", mapTypeValue);
        mapOfMapsValue.put("key2", mapTypeValue);

        Map<String, Object> additionalParams = new HashMap<>(6);
        additionalParams.put("listType", listTypeValue);
        additionalParams.put("mapType", mapTypeValue);
        additionalParams.put("listTypeOfList", Arrays.asList(listTypeValue, listTypeValue));
        additionalParams.put("listTypeOfMap", Arrays.asList(mapTypeValue, mapTypeValue));
        additionalParams.put("mapTypeOfList", mapOfListsValue);
        additionalParams.put("mapTypeOfMap", mapOfMapsValue);
        return additionalParams;
    }

    public static Path createDuplicateResource(final String resourcePath, File temporaryFolder) throws URISyntaxException, IOException {
       return createDuplicateResource(getResource(resourcePath), temporaryFolder);
    }

    public static Path createDuplicateResource(Class<?> testClass, String resourcePath, File temporaryFolder) throws IOException {
        return createDuplicateResource(getResource(testClass, resourcePath), temporaryFolder);
    }

    private static Path createDuplicateResource (Path path, File temporaryFolder) throws IOException {
        File actualFile = path.toFile();
        final File tempFile = temporaryFolder.createTempFile("junit", "txt");
        FileUtils.copyFile(actualFile, tempFile);
        return tempFile.toPath();
    }

    public static String createPoliciesWithSpecificInstantiationLevel(String defaultLevel) {
        return "{\"allScalingAspects\":{\"ScalingAspects1\":{\"type\":\"tosca.policies.nfv.ScalingAspects\","
                + "\"properties\":{\"aspects\":{\"Aspect1\":{\"name\":\"Aspect1\",\n"
                + "\"description\":\"Scale level 0-10 maps to 1-41 for test-cnf-vnfc3 VNFC instances and also maps to 5-45 for test-cnf-vnfc2 VNFC "
                + "instances (4 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],"
                + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                + "\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},"
                + "\"targets\":[\"eric-pm-bulk-reporter\"],\"allInitialDelta\":{\"eric-pm-bulk-reporter\":{\"type\":\"tosca.policies.nfv"
                + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":2}},"
                + "\"targets\":[\"eric-pm-bulk-reporter\"]}}}}}}}}},\"allInitialDelta\":{\"eric-pm-bulk-reporter\":{\"type\":\"tosca.policies.nfv"
                + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":2}},\"targets\":[\"eric-pm-bulk-reporter\"]}},"
                + "\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\","
                + "\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},"
                + "\"targets\":[\"eric-pm-bulk-reporter\"],\"allInitialDelta\":{\"eric-pm-bulk-reporter\":{\"type\":\"tosca.policies.nfv"
                + ".VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":2}},\"targets\":[\"eric-pm-bulk-reporter\"]}}}},"
                + "\"allVnfPackageChangePolicy\":{},\"allVduInstantiationLevels\":{\"fsb1_InstantiationLevels\":{\"type\":\"tosca.policies.nfv"
                + ".VduInstantiationLevels\",\"properties\":{\"instantiationLevels\":{\"instantiation_level_1\":{\"number_of_instances\":1}},"
                + "\"levels\":{\"instantiation_level_1\":{\"number_of_instances\":1}}},\"targets\":[\"eric-pm-bulk-reporter\"]}},"
                + "\"allInstantiationLevels\":{\"InstantiationLevels\":{\"type\":\"tosca.policies.nfv.InstantiationLevels\","
                + "\"properties\":{\"levels\":{\"instantiation_level_2\":{\"scale_info\":{\"Aspect1\":{\"scale_level\":3}},"
                + "\"description\":\"eric-pm-bulk-reporter\"},\"instantiation_level_1\":{\"scale_info\":{\"Aspect1\":{\"scale_level\":1}},"
                + "\"description\":\"eric-pm-bulk-reporter\"}},\"default_level\":\"" + defaultLevel + "\"}}}}";
    }

    public static void checkLinkHeader(MockHttpServletResponse response, int self, int totalPages) {
        int last = totalPages == 0 ? 1 : totalPages;
        assertThat(response.getHeaderNames()).contains("Link");
        String linkHeader = response.getHeader("Link");
        assertThat(linkHeader).isNotNull();

        LinkRelation previousRel = LinkRelation.of(PREVIOUS);
        LinkRelation nextRel = LinkRelation.of(NEXT);
        LinkRelation selfRel = LinkRelation.of(SELF);
        LinkRelation lastRel = LinkRelation.of(LAST);
        LinkRelation firstRel = LinkRelation.of(FIRST);

        String firstMarker = format(NEXTPAGE_OPAQUE_MARKER_QUERY, 1);
        String previousMarker = format(NEXTPAGE_OPAQUE_MARKER_QUERY, self - 1);
        String selfMarker = format(NEXTPAGE_OPAQUE_MARKER_QUERY, self);
        String nextMarker = format(NEXTPAGE_OPAQUE_MARKER_QUERY, self + 1);
        String lastMarker = format(NEXTPAGE_OPAQUE_MARKER_QUERY, last);

        Links links = Links.parse(linkHeader);
        assertThat(links).isNotNull();
        assertThat(links.stream())
                .anyMatch(link -> link.getHref().contains(firstMarker) && link.getRel().isSameAs(firstRel))
                .anyMatch(link -> link.getHref().contains(selfMarker) && link.getRel().isSameAs(selfRel))
                .anyMatch(link -> link.getHref().contains(lastMarker) && link.getRel().isSameAs(lastRel));

        if (last == 1) {
            assertThat(links).hasSize(3);
            assertThat(links.getLink(nextRel)).isEmpty();
            assertThat(links.getLink(previousRel)).isEmpty();
        } else if (self == last) {
            assertThat(links).hasSize(4);
            assertThat(links.getLink(nextRel)).isEmpty();
            assertThat(links.getLink(previousRel))
                    .isNotEmpty().get()
                    .matches(link -> link.getHref().contains(previousMarker))
                    .matches(link -> link.getRel().isSameAs(previousRel));
        } else if (self == 1) {
            assertThat(links).hasSize(4);
            assertThat(links.getLink(previousRel)).isEmpty();
            assertThat(links.getLink(nextRel))
                    .isNotEmpty().get()
                    .matches(link -> link.getHref().contains(nextMarker))
                    .matches(link -> link.getRel().isSameAs(nextRel));
        } else {
            assertThat(links).hasSize(5);
            assertThat(links.getLink(nextRel))
                    .isNotEmpty().get()
                    .matches(link -> link.getHref().contains(nextMarker))
                    .matches(link -> link.getRel().isSameAs(nextRel));
            assertThat(links.getLink(previousRel))
                    .isNotEmpty().get()
                    .matches(link -> link.getHref().contains(previousMarker))
                    .matches(link -> link.getRel().isSameAs(previousRel));
        }
    }

    public static void checkLinkBody(PaginationLinks paginationLinks, int self, int totalPages) {
        int last = totalPages == 0 ? 1 : totalPages;
        assertThat(paginationLinks).isNotNull();
        String firstPage = format(PAGE_QUERY, 1);
        String previousPage = format(PAGE_QUERY, self - 1);
        String selfPage = format(PAGE_QUERY, self);
        String nextPage = format(PAGE_QUERY, self + 1);
        String lastPage = format(PAGE_QUERY, last);

        assertThat(paginationLinks.getFirst().getHref()).isNotBlank().contains(firstPage);
        assertThat(paginationLinks.getLast().getHref()).isNotBlank().contains(lastPage);
        assertThat(paginationLinks.getSelf().getHref()).isNotBlank().contains(selfPage);

        if (last == 1) {
            assertThat(paginationLinks.getNext()).isNull();
            assertThat(paginationLinks.getPrev()).isNull();
        } else if (self == last) {
            assertThat(paginationLinks.getNext()).isNull();
            assertThat(paginationLinks.getPrev().getHref()).isNotNull().contains(previousPage);
        } else if (self == 1) {
            assertThat(paginationLinks.getPrev()).isNull();
            assertThat(paginationLinks.getNext().getHref()).isNotNull().contains(nextPage);
        } else {
            assertThat(paginationLinks.getPrev().getHref()).isNotNull().contains(previousPage);
            assertThat(paginationLinks.getNext().getHref()).isNotNull().contains(nextPage);
        }
    }

    public static Integer checkPaginationHeader(MockHttpServletResponse response, int number, int size) {
        assertThat(response.getHeaderNames()).contains("PaginationInfo");
        String paginationHeader = response.getHeader("PaginationInfo");
        assertThat(paginationHeader).isNotNull();

        Map<String, Integer> headerMap = Arrays.stream(paginationHeader.split(","))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(headers -> headers[0], headers -> Integer.parseInt(headers[1])));
        assertThat(headerMap).isNotNull();
        assertThat(headerMap).hasSize(4);
        assertThat(headerMap)
                .containsEntry(NUMBER, number)
                .containsEntry(SIZE, size);
        assertThat(headerMap.get(TOTAL_PAGES)).isGreaterThanOrEqualTo(number);
        assertThat(headerMap.get(TOTAL_ELEMENTS)).isLessThanOrEqualTo(size * (headerMap.get(TOTAL_PAGES)));
        return headerMap.get(TOTAL_PAGES);
    }

    public static Integer checkPaginationBody(PaginationInfo paginationInfo, int number, int size) {
        assertThat(paginationInfo).isNotNull();
        assertThat(paginationInfo.getNumber()).isEqualTo(number);
        assertThat(paginationInfo.getSize()).isEqualTo(size);
        assertThat(paginationInfo.getTotalPages()).isGreaterThanOrEqualTo(number);
        assertThat(paginationInfo.getTotalElements()).isLessThanOrEqualTo(size * (paginationInfo.getTotalPages()));
        return paginationInfo.getTotalPages();
    }

    public static HelmChart getHelmChartByName(final List<HelmChart> helmCharts, final String helmChartName) {
        Optional<HelmChart> optionalPackage = helmCharts.stream()
                .filter(helmChart -> helmChart.getHelmChartName().contains(helmChartName))
                .findFirst();
        assertThat(optionalPackage).isPresent();
        return optionalPackage.get();
    }

    public static Map<String, Object> loadYamlToMap(final Path valuesFile) throws IOException {
        InputStream inputStream = Files.newInputStream(valuesFile);
        return new Yaml(new SafeConstructor(new LoaderOptions())).load(inputStream);
    }

    public static String firstHelmReleaseNameFor(String releaseName) {
        return helmReleaseNameFor(releaseName, 1);
    }

    public static String secondHelmReleaseNameFor(String releaseName) {
        return helmReleaseNameFor(releaseName, 2);
    }

    public static String thirdHelmReleaseNameFor(String releaseName) {
        return helmReleaseNameFor(releaseName, 3);
    }

    public static VnfInstance createVnfInstance(boolean withExtensions) {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setClusterName("hall914.config");
        vnfInstance.setPolicies(TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_2));
        vnfInstance.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        if (withExtensions) {
            var extensions = createExtensions();
            var modifyAspects = checkAndCastObjectToMap(extensions.get(VNF_CONTROLLED_SCALING));
            modifyAspects.put("Aspect1", "CISMControlled");
            modifyAspects.put("Payload", "asdf");
            vnfInstance.setVnfInfoModifiableAttributesExtensions(convertObjToJsonString(extensions));
        }
        return vnfInstance;
    }

    public static Map<String, Object> createExtensions() {
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put(TestUtils.PAYLOAD, CISM_CONTROLLED);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);
        return extensions;
    }

    public static List<ScaleInfo> createTargetScaleLevelInfo(Map<String, Integer> scaleLevelInfo) {
        return scaleLevelInfo.entrySet().stream().map(entry -> {
            ScaleInfo scaleInfo = new ScaleInfo();

            scaleInfo.setAspectId(entry.getKey());
            scaleInfo.setScaleLevel(entry.getValue());

            return scaleInfo;
        }).collect(Collectors.toList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assertOneValueFilterExpression(String key, OperandOneValue operand, Object value,
                                                      FilterExpressionOneValue result) {
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getOperation()).isEqualTo(operand);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assertMultiValueFilterExpression(String key, OperandMultiValue operand, List values,
                                                        FilterExpressionMultiValue result) {
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getOperation()).isEqualTo(operand);
        assertThat(result.getValues()).hasSize(values.size())
                .hasSameElementsAs(values);
    }

    private static String helmReleaseNameFor(final String releaseName, final int index) {
        return format("%s-%d", releaseName, index);
    }

    public static List<OperationDetail> createSupportedOperations(LCMOperationsEnum... lcmOperationsEnum) {
        return Arrays.stream(lcmOperationsEnum)
                .map(operationsEnum -> OperationDetail.ofSupportedOperation(operationsEnum.getOperation()))
                .collect(Collectors.toList());
    }

    public static List<OperationDetail> createNotSupportedOperations(LCMOperationsEnum... lcmOperationsEnum) {
        return Arrays.stream(lcmOperationsEnum)
                .map(operationsEnum -> OperationDetail.ofNotSupportedOperation(operationsEnum.getOperation()))
                .collect(Collectors.toList());
    }

    public static List<OperationDetail> createNotSupportedOperationsWithError(Map<LCMOperationsEnum, String> mapOfOperationsAndErrors) {
        List<OperationDetail> listOfOperationDetails = new ArrayList<>();
        for (Map.Entry<LCMOperationsEnum, String> entry : mapOfOperationsAndErrors.entrySet()) {
            listOfOperationDetails.add(
                    OperationDetail.ofNotSupportedOperationWithError(entry.getKey().getOperation(), entry.getValue()));
        }
        return listOfOperationDetails;
    }

    public static Map<String, Object> createExtensionsWithDeployableModules() {
        Map<String, Object> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "enabled");
        deployableModules.put("deployable_module_3", "disabled");
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(DEPLOYABLE_MODULES, deployableModules);
        return extensions;
    }

    public static List<HelmChart> prepareHelmCHarts() {
        List<HelmChart> helmCharts = new ArrayList<>();
        helmCharts.add(createCrdHelmChart(1));
        helmCharts.add(createCrdHelmChart(2));
        helmCharts.add(createCrdHelmChart(3));
        helmCharts.add(createCrdHelmChart(4));
        helmCharts.add(createCnfHelmChart(1));
        helmCharts.add(createCnfHelmChart(2));
        helmCharts.add(createCnfHelmChart(3));
        helmCharts.add(createCnfHelmChart(4));

        return helmCharts;
    }

    public static void setDefaultPriority(List<HelmChart> helmCharts) {
        int defaultPriority = 1;
        for (HelmChart helmChart : helmCharts) {
            helmChart.setPriority(defaultPriority);
            defaultPriority++;
        }
    }

    public static HelmChart createCrdHelmChart(Integer id) {
        return createHelmChart(CRD_PACKAGE_KEY + id, HelmChartType.CRD, null);
    }

    public static HelmChart createCnfHelmChart(Integer id) {
        return createHelmChart(HELM_PACKAGE_KEY + id, HelmChartType.CNF, null);
    }

    public static HelmChart createCnfHelmChart(Integer id, LifecycleOperationState state) {
        return createHelmChart(HELM_PACKAGE_KEY + id, HelmChartType.CNF, state);
    }

    public static HelmChart createHelmChart(String id, HelmChartType type, LifecycleOperationState state) {
        HelmChart chart = new HelmChart();
        chart.setId(id);
        chart.setReleaseName(type + "_release_name");
        chart.setHelmChartType(type);
        chart.setHelmChartArtifactKey(id);
        chart.setState(state == null ? null : state.toString());

        return chart;
    }
}
