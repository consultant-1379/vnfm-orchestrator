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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANO_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper.EMPTY_BRO_URL;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.crypto.CryptoUtils;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstanceNamespaceDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceDeletionInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.request.InstantiateRequestHandler;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        LifeCycleManagementHelper.class,
        InstantiateRequestHandler.class,
        CryptoUtils.class
})
@MockBean(classes = {
        ClusterConfigService.class,
        WorkflowRoutingService.class,
        CryptoService.class,
        ObjectMapper.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        InstantiationLevelService.class,
        ExtensionsService.class,
        ReplicaDetailsService.class,
        Day0ConfigurationService.class,
        PackageService.class,
        HelmChartHelper.class,
        InstantiateVnfRequestValidatingService.class,
        ValuesFileService.class,
        InstanceService.class,
        OssNodeService.class,
        ValuesFileComposer.class,
        UsernameCalculationService.class,
        GrantingService.class,
        GrantingResourceDefinitionCalculation.class,
        MappingFileService.class,
        LcmOpSearchService.class,
        VnfInstanceService.class,
        HelmClientVersionValidator.class,
        LcmOpErrorManagementService.class,
        DeployableModulesService.class,
        VnfdParametersHelper.class
})
public class LifeCycleManagementHelperTest {

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private InstantiateRequestHandler instantiateRequestHandler;

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OssNodeService ossNodeService;

    @MockBean
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    @MockBean
    private EnmMetricsExposers enmMetricsExposers;

    @Value("${workflow.command.execute.defaultTimeOut}")
    private long defaultTimeout;

    @Test
    public void manoControlledScalingIsNullWhenVNFDoesNotSupportScaling() {
        VnfInstance instance = getVnfInstance();
        instance.setPolicies(null);
        instantiateRequestHandler.updateInstanceModel(instance, new HashMap<>());
        assertThat(instance.getManoControlledScaling()).isNull();
    }

    @Test
    public void manoControlledScalingDefaultValueWhenVNFSupportsScaling() {
        VnfInstance instance = getVnfInstance();
        instantiateRequestHandler.updateInstanceModel(instance, new HashMap<>());
        assertThat(instance.getManoControlledScaling()).isFalse();
    }

    @Test
    public void manoControlledScalingTurnedOffWhenVNFSupportsScaling() {
        VnfInstance instance = getVnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(MANO_CONTROLLED_SCALING, false);
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        assertThat(instance.getManoControlledScaling()).isFalse();
    }

    @Test
    public void manoControlledScalingTurnedOnWhenVNFSupportsScaling() {
        VnfInstance instance = getVnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(MANO_CONTROLLED_SCALING, true);
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        assertThat(instance.getManoControlledScaling()).isTrue();
    }

    @Test
    public void manoControlledScalingValueHasNoEffectOnVNFWhichDoesNotSupportScaling() {
        VnfInstance instance = getVnfInstance();
        instance.setPolicies(null);
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(MANO_CONTROLLED_SCALING, false);
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        assertThat(instance.getManoControlledScaling()).isNull();
        additionalParams.put(MANO_CONTROLLED_SCALING, true);
        instantiateRequestHandler.updateInstanceModel(instance, additionalParams);
        assertThat(instance.getManoControlledScaling()).isNull();
    }

    @Test
    public void manoControllerScalingInvalidParameterValue() {
        VnfInstance instance = getVnfInstance();
        Map<String, Object> additionalParams = new HashMap<>();

        additionalParams.put(MANO_CONTROLLED_SCALING, "text");
        assertThatThrownBy(() -> instantiateRequestHandler.updateInstanceModel(instance, additionalParams)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("is not a boolean");

        additionalParams.put(MANO_CONTROLLED_SCALING, "4");
        assertThatThrownBy(() -> instantiateRequestHandler.updateInstanceModel(instance, additionalParams)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("is not a boolean");

        additionalParams.put(MANO_CONTROLLED_SCALING, "");
        assertThatThrownBy(() -> instantiateRequestHandler.updateInstanceModel(instance, additionalParams)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("is not a boolean");
    }

    @Test
    public void testOperationError4xxWithValidJson() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        String errorMessage = "{\"errorDetails\":[{\"message\":\"Namespace \\\"---my-namespace\\\" is invalid: metadata.name: Invalid value: "
                + "\\\"---my-namespace\\\": a DNS-1123 label must consist of lower case alphanumeric characters or '-', and must start and end with"
                + " an alphanumeric character (e.g. 'my-name',  or '123-abc', regex used for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?')\"}]}";
        lifeCycleManagementHelper.setOperationErrorFor4xx(lifecycleOperation, errorMessage);
        assertThat(lifecycleOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,"
                + "\"detail\":\"[{\\\"message\\\":\\\"Namespace \\\\\\\"---my-namespace\\\\\\\""
                + " is invalid: metadata.name: Invalid value: \\\\\\\"---my-namespace\\\\\\\": "
                + "a DNS-1123 label must consist of lower case alphanumeric characters or '-', "
                + "and must start and end with an alphanumeric character (e.g. 'my-name',  or "
                + "'123-abc', regex used for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?')"
                + "\\\"}]\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void testOperationError4xxWithInvalidJson() {
        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        String errorMessage = "{\"releaseName\":\"my-release-name\",\"errorMessage\":\"The Namespace \"release-test^-vnf-tom-rest-rest\" is invalid: metadata.name: Invalid value: \"release-test^-vnf-tom-rest-rest\": a DNS-1123 label must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character (e.g. 'my-name',  or '123-abc', regex used for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?')";
        lifeCycleManagementHelper.setOperationErrorFor4xx(lifecycleOperation, errorMessage);
        assertThat(lifecycleOperation.getError()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,"
                + "\"detail\":\"{\\\"releaseName\\\":\\\"my-release-name\\\","
                + "\\\"errorMessage\\\":\\\"The Namespace "
                + "\\\"release-test^-vnf-tom-rest-rest\\\" is invalid: metadata.name: Invalid "
                + "value: \\\"release-test^-vnf-tom-rest-rest\\\": a DNS-1123 label must "
                + "consist of lower case alphanumeric characters or '-', and must start and end"
                + " with an alphanumeric character (e.g. 'my-name',  or '123-abc', regex used "
                + "for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?')\",\"instance\":\"about:blank\"}");
    }

    @Test
    public void testNamespaceValidationForInstantiateOnKubernetesNamespaceKubeSystem() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("kube-system", "my-cluster");
        assertThatThrownBy(() -> lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "kube-validation", "instantiate"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot instantiate in any of the Kubernetes initialized namespaces");
    }

    @Test
    public void testNamespaceValidationForInstantiateOnKubernetesNamespaceKubePublicOnDifferentCluster() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("kube-public", "my-cluster");
        assertThatThrownBy(() -> lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "kube-validation", "instantiate"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot instantiate in any of the Kubernetes initialized namespaces");
    }

    @Test
    public void testNamespaceValidationForInstantiateOnDefaultNamespace() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("default", "my-cluster");
        assertThatThrownBy(() -> lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "kube-validation", "instantiate"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cannot instantiate in any of the Kubernetes initialized namespaces");
    }

    @Test
    public void testNamespaceGenerationOnNullNamespace() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody(null, "my-cluster");
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "release-cnf-1", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        // Release name must be set to Namespace
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE)).isEqualTo("release-cnf-1");
        assertThat(additionalParams.get("ossTopology.disableLdapUser")).isEqualTo("true");
        assertThat(additionalParams.get("ossTopology.second")).isEqualTo("false");
        assertThat(additionalParams.get("third")).isEqualTo("false");
    }


    @Test
    public void testNamespaceGenerationWithReleaseNameMatching() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody(null, "cluster-validate");
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "namespace-validate", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        // Release name with a random string appended will be the namespace
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE).toString()).contains("namespace-validate");
        assertThat(additionalParams.get("ossTopology.disableLdapUser")).isEqualTo("true");
        assertThat(additionalParams.get("ossTopology.second")).isEqualTo("false");
        assertThat(additionalParams.get("third")).isEqualTo("false");
    }

    @Test
    public void testNamespaceGenerationWithEvnfmNamespaceMatching() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody(null, "my-cluster");
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "evnfm-ns", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        // Release name with a random string appended will be the namespace
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE).toString()).contains("evnfm-ns");
        assertThat(additionalParams.get("ossTopology.disableLdapUser")).isEqualTo("true");
        assertThat(additionalParams.get("ossTopology.second")).isEqualTo("false");
        assertThat(additionalParams.get("third")).isEqualTo("false");
    }

    @Test
    public void testNamespaceGenerationOnNullNamespaceAndKubeReleaseNameFail() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody(null, "my-cluster");
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "kube-system", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        // Release name must be set to Namespace plus 5 symbols
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE).toString()).startsWith("kube-system");
        assertThat(additionalParams.get(NAMESPACE).toString().length()).isEqualTo("kube-system".length() + 6);
    }

    @Test
    public void testNamespaceGenerationOnNullNamespaceAndCrdNamespaceReleaseNameFail() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody(null, "my-cluster");

        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(any())).thenReturn("eric-crd-ns");

        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "eric-crd-ns", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        // Release name must be set to Namespace plus 5 symbols
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE).toString()).startsWith("eric-crd-ns");
        assertThat(additionalParams.get(NAMESPACE).toString().length()).isEqualTo("eric-crd-ns".length() + 6);
    }

    @Test
    public void testNamespaceGenerationWithNullAdditionalParams() {
        InstantiateVnfRequest instantiateVnfRequest = new InstantiateVnfRequest();
        instantiateVnfRequest.setClusterName("cluster-validate");
        lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "release-validate", "instantiate");
        Map additionalParams = (Map) instantiateVnfRequest.getAdditionalParams();
        assertThat(additionalParams.get(NAMESPACE).toString()).matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
        assertThat(additionalParams.get(NAMESPACE).toString()).isEqualTo("release-validate");
    }

    @Test
    public void testValidBroEndpointUrlVNFD() throws URISyntaxException {
        VnfInstance instance = getVnfInstance();
        Path valuesFile = TestUtils.getResource("valueFiles/valid-bro-endpoint.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        assertThatNoException().isThrownBy(() -> lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, instance));
        assertThat(instance.getBroEndpointUrl()).isEqualTo("http://valid-bro-url:8080");
    }

    @Test
    public void testInvalidBroEndpointUrlVNFD() throws URISyntaxException {
        VnfInstance instance = getVnfInstance();
        Path valuesFile = TestUtils.getResource("valueFiles/invalid-bro-endpoint.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        assertThatThrownBy(() ->
                lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, instance))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The Url : invalid-bro-url is invalid due to no protocol: invalid-bro-url. Please provide a valid URL.");
        assertThat(instance.getBroEndpointUrl()).isNull();
    }

    @Test
    public void testEmptyBroEndpointUrlVNFD() throws URISyntaxException {
        VnfInstance instance = getVnfInstance();
        Path valuesFile = TestUtils.getResource("valueFiles/empty-bro-endpoint.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        assertThatThrownBy(() ->
                lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, instance))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Bro URL cannot be empty. Please provide a valid BRO URL.");
        assertThat(instance.getBroEndpointUrl()).isNull();
    }

    @Test
    public void testNullBroEndpointUrlVNFD() throws URISyntaxException {
        VnfInstance instance = getVnfInstance();
        Path valuesFile = TestUtils.getResource("valueFiles/null-value-bro-endpoint.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        assertThatThrownBy(() ->
                lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, instance))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(EMPTY_BRO_URL);
        assertThat(instance.getBroEndpointUrl()).isNull();
    }

    @Test
    public void testNoBroEndpointUrlVNFD() throws URISyntaxException {
        VnfInstance instance = getVnfInstance();
        Path valuesFile = TestUtils.getResource("valueFiles/no-bro-endpoint.yaml");
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
        assertThatNoException().isThrownBy(() -> lifeCycleManagementHelper.addBroUrlIfPresentToInstance(valuesYamlMap, instance));
        assertThat(instance.getBroEndpointUrl()).isNull();
    }

    @Test
    public void testSetTimeout() {
        LifecycleOperation operation = new LifecycleOperation();
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(300 + 120);
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, "300");
        assertThat(operation.getExpiredApplicationTime().toString()).isNotEmpty();
        assertThat(totalTimeout.getDayOfMonth()).isEqualTo(operation.getExpiredApplicationTime().getDayOfMonth());
        assertThat(totalTimeout.getDayOfWeek()).isEqualTo(operation.getExpiredApplicationTime().getDayOfWeek());
        assertThat(totalTimeout.getDayOfYear()).isEqualTo(operation.getExpiredApplicationTime().getDayOfYear());
        assertThat(totalTimeout.getHour()).isEqualTo(operation.getExpiredApplicationTime().getHour());
        assertThat(totalTimeout.getMinute()).isEqualTo(operation.getExpiredApplicationTime().getMinute());
    }

    @Test
    public void testSetTimeoutWithInvalidTime() {
        LifecycleOperation operation = new LifecycleOperation();
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(defaultTimeout + 120);

        String applicationTimeout = lifeCycleManagementHelper.getApplicationTimeout(Map.of(APPLICATION_TIME_OUT, "test"));
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, applicationTimeout);

        assertThat(operation.getExpiredApplicationTime().toString()).isNotEmpty();
        assertThat(totalTimeout.getDayOfMonth()).isEqualTo(operation.getExpiredApplicationTime().getDayOfMonth());
        assertThat(totalTimeout.getDayOfWeek()).isEqualTo(operation.getExpiredApplicationTime().getDayOfWeek());
        assertThat(totalTimeout.getDayOfYear()).isEqualTo(operation.getExpiredApplicationTime().getDayOfYear());
        assertThat(totalTimeout.getHour()).isEqualTo(operation.getExpiredApplicationTime().getHour());
        assertThat(totalTimeout.getMinute()).isEqualTo(operation.getExpiredApplicationTime().getMinute());

    }

    @Test
    public void testSetTimeoutWithNullValueInTime() {
        LifecycleOperation operation = new LifecycleOperation();
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(defaultTimeout + 120);

        String applicationTimeout = lifeCycleManagementHelper.getApplicationTimeout(Collections.emptyMap());
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, applicationTimeout);

        assertThat(operation.getExpiredApplicationTime().toString()).isNotEmpty();
        assertThat(totalTimeout.getDayOfMonth()).isEqualTo(operation.getExpiredApplicationTime().getDayOfMonth());
        assertThat(totalTimeout.getDayOfWeek()).isEqualTo(operation.getExpiredApplicationTime().getDayOfWeek());
        assertThat(totalTimeout.getDayOfYear()).isEqualTo(operation.getExpiredApplicationTime().getDayOfYear());
        assertThat(totalTimeout.getHour()).isEqualTo(operation.getExpiredApplicationTime().getHour());
        assertThat(totalTimeout.getMinute()).isEqualTo(operation.getExpiredApplicationTime().getMinute());
    }

    @Test
    public void testSetTimeoutWithEmptyValueInTime() {
        LifecycleOperation operation = new LifecycleOperation();
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(defaultTimeout + 120);

        String applicationTimeout = lifeCycleManagementHelper.getApplicationTimeout(Map.of(APPLICATION_TIME_OUT, ""));
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, applicationTimeout);

        assertThat(operation.getExpiredApplicationTime().toString()).isNotEmpty();
        assertThat(totalTimeout.getDayOfMonth()).isEqualTo(operation.getExpiredApplicationTime().getDayOfMonth());
        assertThat(totalTimeout.getDayOfWeek()).isEqualTo(operation.getExpiredApplicationTime().getDayOfWeek());
        assertThat(totalTimeout.getDayOfYear()).isEqualTo(operation.getExpiredApplicationTime().getDayOfYear());
        assertThat(totalTimeout.getHour()).isEqualTo(operation.getExpiredApplicationTime().getHour());
        assertThat(totalTimeout.getMinute()).isEqualTo(operation.getExpiredApplicationTime().getMinute());
    }

    @Test
    public void testSetTimeoutWithTimeOverflow() {
        LifecycleOperation operation = new LifecycleOperation();
        LocalDateTime totalTimeout = LocalDateTime
                .now()
                .plusSeconds(defaultTimeout + 120);

        String applicationTimeout = lifeCycleManagementHelper.getApplicationTimeout(Map.of(APPLICATION_TIME_OUT, "2000000000"));
        lifeCycleManagementHelper.setExpiredTimeoutAndPersist(operation, applicationTimeout);

        assertThat(operation.getExpiredApplicationTime().toString()).isNotEmpty();
        assertThat(totalTimeout.getDayOfMonth()).isEqualTo(operation.getExpiredApplicationTime().getDayOfMonth());
        assertThat(totalTimeout.getDayOfWeek()).isEqualTo(operation.getExpiredApplicationTime().getDayOfWeek());
        assertThat(totalTimeout.getDayOfYear()).isEqualTo(operation.getExpiredApplicationTime().getDayOfYear());
        assertThat(totalTimeout.getHour()).isEqualTo(operation.getExpiredApplicationTime().getHour());
        assertThat(totalTimeout.getMinute()).isEqualTo(operation.getExpiredApplicationTime().getMinute());
    }

    @Test
    public void testCNFsUsingSameCRDNamespace() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("multi-cluster-crd-ns", "crdcluster");

        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(anyString())).thenReturn("multi-cluster-crd-ns");
        assertThatThrownBy(() -> lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "validate-crd-namespace", "instantiate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("multi-cluster-crd-ns is reserved for CRDs. Cannot instantiate CNFs in CRD namespace");
    }

    @Test
    public void testCNFsUsingValidNamespace() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("multi-cluster-cnf-ns", "crdcluster");

        assertThatNoException().isThrownBy(() -> lifeCycleManagementHelper.verifyNamespaceCanBeUsed(instantiateVnfRequest, "validate-crd-namespace", "instantiate"));
    }


    @Test
    public void testPersistNamespaceDetailsFails() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId("dummyId");
        instance.setNamespace("dummyNamespace");
        instance.setClusterName("my-cluster");

        VnfInstanceNamespaceDetails namespaceDetails = new VnfInstanceNamespaceDetails();
        namespaceDetails.setNamespace(instance.getNamespace());
        namespaceDetails.setClusterServer("https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1");
        namespaceDetails.setDeletionInProgress(true);

        when(databaseInteractionService.getNamespaceDetailsPresent(any(), any()))
                .thenReturn(List.of(namespaceDetails));

        assertThatThrownBy(() -> lifeCycleManagementHelper
                .persistNamespaceDetails(instance))
                .isInstanceOf(NamespaceDeletionInProgressException.class)
                .hasMessage(String.format(NAMESPACE_MARKED_FOR_DELETION_ERROR_MESSAGE, instance.getNamespace(),
                        instance.getClusterName()));
    }

    private InstantiateVnfRequest createInstantiateVnfRequestBody(String namespace, String clusterName) {
        InstantiateVnfRequest request = new InstantiateVnfRequest();
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put(NAMESPACE, namespace);
        additionalParams.put("ossTopology.disableLdapUser", "true");
        additionalParams.put("ossTopology.second", "false");
        additionalParams.put("third", "false");
        request.setAdditionalParams(additionalParams);
        request.setClusterName(clusterName);
        return request;
    }

    private VnfInstance getVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setPolicies("");
        return vnfInstance;
    }
}
