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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.CISM_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.TestUtils.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD;
import static com.ericsson.vnfm.orchestrator.TestUtils.PAYLOAD_2;
import static com.ericsson.vnfm.orchestrator.TestUtils.VNF_CONTROLLED_SCALING;
import static com.ericsson.vnfm.orchestrator.TestUtils.createExtensions;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.DEPLOYABLE_MODULES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.utils.InstanceUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ExtensionsServiceImpl.class,
        ExtensionsMapper.class,
        ObjectMapper.class
})
public class ExtensionsServiceImplTest {

    private static final String POLICIES_JSON = "policies.json";

    @Autowired
    private ExtensionsService extensionsService;

    @Autowired
    private ExtensionsMapper extensionsMapper;

    @Test
    public void testUpdateInstanceWithExtensionsInRequest() {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();

        //need to set extensions to instance
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\","
                                                                     + "\"Payload_2\":\"CISMControlled\"}}");

        createTempInstance(vnfInstance);

        //create map of request extensions
        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> vnfControlledScalingInRequest = new HashMap<>();
        vnfControlledScalingInRequest.put(PAYLOAD_2, MANUAL_CONTROLLED);
        requestExtensions.put(VNF_CONTROLLED_SCALING, vnfControlledScalingInRequest);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        extensionsService.updateInstanceWithExtensionsInRequest(requestExtensions, tempInstance);
        // expected is default provided in VNFD
        // no Modify extensions and no extensions in Instantiate request
        Map<String, Object> expectedExtensions = new HashMap<>();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put(PAYLOAD, MANUAL_CONTROLLED);
        vnfControlledScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        expectedExtensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        assertThat(Utility.convertObjToJsonString(expectedExtensions)).isEqualTo(tempInstance.getVnfInfoModifiableAttributesExtensions());
    }

    @Test
    public void testUpdateTempInstanceWithExtensionsFromRequestAndDefault() {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfInfoModifiableAttributesExtensions(null);

        createTempInstance(vnfInstance);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        //create map of request extensions
        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> vnfControlledScalingInRequest = new HashMap<>();
        vnfControlledScalingInRequest.put(PAYLOAD, CISM_CONTROLLED);
        vnfControlledScalingInRequest.put(PAYLOAD_2, MANUAL_CONTROLLED);
        requestExtensions.put(VNF_CONTROLLED_SCALING, vnfControlledScalingInRequest);

        extensionsService.updateInstanceWithExtensionsInRequest(requestExtensions, tempInstance);

        // expected is combination of Modification request and default in VNFD
        // Payload from Modify and Payload_2 from default
        Map<String, Object> expectedExtensions = new HashMap<>();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put(PAYLOAD, CISM_CONTROLLED);
        vnfControlledScaling.put(PAYLOAD_2, MANUAL_CONTROLLED);
        expectedExtensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);

        assertThat(Utility.convertObjToJsonString(expectedExtensions)).isEqualTo(tempInstance.getVnfInfoModifiableAttributesExtensions());
    }

    @Test
    public void testSetDefaultExtensionsReset() {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"vnfControlledScaling\":{\"Payload\":\"ManualControlled\","
                                                                     + "\"Payload_2\":\"CISMControlled\"}}");

        String descriptorWithoutExtensions = readDataFromFile(getClass(), "descriptorModelWithoutExtensions.json");
        extensionsService.setDefaultExtensions(vnfInstance, descriptorWithoutExtensions);

        assertThat(vnfInstance.getVnfInfoModifiableAttributesExtensions()).isNull();
    }

    @Test
    public void testSetDefaultExtensionsWithDeployableModules() throws Exception {
        Map<String, String> expectedDeployableModules = new LinkedHashMap<>();
        expectedDeployableModules.put("deployable_module_1", "enabled");
        expectedDeployableModules.put("deployable_module_2", "enabled");
        expectedDeployableModules.put("deployable_module_3", "disabled");

        final VnfInstance vnfInstance = TestUtils.getVnfInstance();
        String descriptorModel = readDataFromFile(getClass(), "deployableModules/descriptorModel.json");

        extensionsService.setDefaultExtensions(vnfInstance, descriptorModel);

        final String vnfInfoModifiableAttributesExtensions = vnfInstance.getVnfInfoModifiableAttributesExtensions();
        assertThat(vnfInfoModifiableAttributesExtensions).isNotNull();
        final Map<String, String> deployableModules = extensionsMapper.getDeployableModulesValues(vnfInfoModifiableAttributesExtensions);
        assertThat(deployableModules).hasSize(3).containsExactlyInAnyOrderEntriesOf(expectedDeployableModules);
    }

    @Test
    public void testUpdateInstanceWithExtensionsInRequestWithDeployableModules() {
        VnfInstance vnfInstance = TestUtils.getVnfInstance();
        vnfInstance.setVnfInfoModifiableAttributesExtensions("{\"deployableModules\":{\"deployable_module_3\":\"disabled\","
                                                                     + "\"deployable_module_2\":\"enabled\",\"deployable_module_1\":\"enabled\"}}");
        createTempInstance(vnfInstance);

        Map<String, String> deployableModules = new HashMap<>();
        deployableModules.put("deployable_module_1", "enabled");
        deployableModules.put("deployable_module_2", "disabled");
        deployableModules.put("deployable_module_3", "disabled");

        Map<String, Object> requestExtensions = new HashMap<>();
        Map<String, Object> deployableModulesInRequest = new HashMap<>();
        deployableModulesInRequest.put("deployable_module_2", "disabled");
        requestExtensions.put(DEPLOYABLE_MODULES, deployableModulesInRequest);

        VnfInstance tempInstance = parseJson(vnfInstance.getTempInstance(), VnfInstance.class);

        extensionsService.updateInstanceWithExtensionsInRequest(requestExtensions, tempInstance);

        final Map<String, String> actualExtensions =
                extensionsMapper.getDeployableModulesValues(tempInstance.getVnfInfoModifiableAttributesExtensions());

        assertThat(actualExtensions).hasSize(3).containsExactlyInAnyOrderEntriesOf(deployableModules);
    }

    @Test
    public void testValidateExtensions() {
        var extensions = createExtensions();
        var policies = readDataFromFile(getClass(), POLICIES_JSON);

        assertThatNoException().isThrownBy(() -> extensionsService.validateVnfControlledScalingExtension(extensions, policies));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidateExtensionsNotDefinedAspectFail() {
        var extensions = createExtensions();
        ((Map<String, String>) extensions.get(VNF_CONTROLLED_SCALING)).put("NotDefinedAspect", CISM_CONTROLLED);
        var policies = readDataFromFile(getClass(), POLICIES_JSON);

        assertThatThrownBy(() -> extensionsService.validateVnfControlledScalingExtension(extensions, policies))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ASPECTS_SPECIFIED_IN_THE_REQUEST_ARE_NOT_DEFINED_IN_THE_POLICY);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidateExtensionsWrongAspectTypeFail() {
        var extensions = createExtensions();
        ((Map<String, String>) extensions.get(VNF_CONTROLLED_SCALING)).put(PAYLOAD_2, "WrongType");
        var policies = readDataFromFile(getClass(), POLICIES_JSON);

        assertThatThrownBy(() -> extensionsService.validateVnfControlledScalingExtension(extensions, policies))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(VNF_CONTROLLED_SCALING_INVALID_ERROR_MESSAGE);
    }

    private void createTempInstance(final VnfInstance vnfInstance) {
        VnfInstance tempInstance = InstanceUtils.createTempInstance(vnfInstance);
        vnfInstance.setTempInstance(convertObjToJsonString(tempInstance));
    }

}