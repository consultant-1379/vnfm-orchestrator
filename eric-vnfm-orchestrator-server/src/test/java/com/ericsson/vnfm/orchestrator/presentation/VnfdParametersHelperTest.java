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
package com.ericsson.vnfm.orchestrator.presentation;

import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdParametersHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.DefaultReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsBuilder;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ScaleMappingReplicaDetailsCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleServiceImpl;
import com.ericsson.vnfm.orchestrator.utils.VnfdUtils;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashMap;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.parseVnfdParameter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        VnfdParametersHelper.class,
        YamlFileMerger.class,
        ValuesFileService.class,
        ObjectMapper.class,
        ValuesFileComposer.class,
        ReplicaDetailsServiceImpl.class,
        DefaultReplicaDetailsCalculationService.class,
        ScaleMappingReplicaDetailsCalculationService.class,
        ReplicaCountCalculationServiceImpl.class,
        ReplicaDetailsBuilder.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        AdditionalAttributesHelper.class
})
@MockBean(classes = {
        MappingFileServiceImpl.class,
        ScaleServiceImpl.class,
        VnfInstanceServiceImpl.class
})
public class VnfdParametersHelperTest {

    @Autowired
    private VnfdParametersHelper vnfdParametersHelper;

    @Autowired
    private ValuesFileService valuesFileService;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setup() {
        when(packageService.getPackageInfoWithDescriptorModel(any())).thenReturn(createDummyPackageResponse());
    }

    @Test
    public void testAdditionalParamsHaveHighestPriority() {
        Map<String, Object> additionalParams = createMutableMap("test.property", "additionalParamValue");
        Map<String, Object> valuesYaml = createMutableMap("test", createMutableMap("property", "additionalParamValue"));
        Map<String, PropertiesModel> defaultProperties = createMutableMap("test.property", new PropertiesModel("defaultValue"));

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getOperationNameFromVnfd(any(), any()))
                    .thenReturn("DUMMY_OPERATION_NAME");
            vnfdUtils.when(() -> VnfdUtils.getPropertiesModelMap(any(), any()))
                    .thenReturn(defaultProperties);

            vnfdParametersHelper.mergeDefaultParameters("TEST_VNFD_ID", INSTANTIATE, additionalParams, valuesYaml);
        }
        Assertions.assertEquals("additionalParamValue", additionalParams.get("test.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("test"));
        Assertions.assertEquals("additionalParamValue", ((Map<?, ?>) valuesYaml.get("test")).get("property"));
    }

    @Test
    public void testValuesYamlHaveHigherPriorityThanDefault() {
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> valuesYaml = createMutableMap("test", createMutableMap("property", "valuesYAMLValue"));
        Map<String, PropertiesModel> defaultProperties = createMutableMap("test.property", new PropertiesModel("defaultValue"));

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getOperationNameFromVnfd(any(), any()))
                    .thenReturn("DUMMY_OPERATION_NAME");
            vnfdUtils.when(() -> VnfdUtils.getPropertiesModelMap(any(), any()))
                    .thenReturn(defaultProperties);

            vnfdParametersHelper.mergeDefaultParameters("TEST_VNFD_ID", INSTANTIATE, additionalParams, valuesYaml);
        }
        Assertions.assertNull(additionalParams.get("test.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("test"));
        Assertions.assertEquals("valuesYAMLValue", ((Map<?, ?>) valuesYaml.get("test")).get("property"));
    }

    @Test
    public void testDefaultParamsMergedIntoAdditionalAndValuesYaml() {
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> valuesYaml = new HashMap<>();
        Map<String, PropertiesModel> defaultProperties = createMutableMap("test.property", new PropertiesModel("defaultValue"));

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getOperationNameFromVnfd(any(), any()))
                    .thenReturn("DUMMY_OPERATION_NAME");
            vnfdUtils.when(() -> VnfdUtils.getPropertiesModelMap(any(), any()))
                    .thenReturn(defaultProperties);
            vnfdUtils.when(() -> parseVnfdParameter(anyString()))
                    .thenCallRealMethod();

            vnfdParametersHelper.mergeDefaultParameters("TEST_VNFD_ID", INSTANTIATE, additionalParams, valuesYaml);
        }
        Assertions.assertEquals("defaultValue", additionalParams.get("test.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("test"));
        Assertions.assertEquals("defaultValue", ((Map<?, ?>) valuesYaml.get("test")).get("property"));
    }

    @Test
    public void testDefaultParamsNotMergedWhenExceptionOccurred() {
        Map<String, Object> additionalParams = createMutableMap("test.property", "additionalParamValue");
        Map<String, Object> valuesYaml = createMutableMap("test", createMutableMap("property", "additionalParamValue"));

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getOperationNameFromVnfd(any(), any()))
                    .thenReturn("DUMMY_OPERATION_NAME");
            vnfdUtils.when(() -> VnfdUtils.getPropertiesModelMap(any(), any()))
                    .thenThrow(JsonProcessingException.class);
            vnfdUtils.when(() -> parseVnfdParameter(anyString()))
                    .thenCallRealMethod();

            vnfdParametersHelper.mergeDefaultParameters("TEST_VNFD_ID", INSTANTIATE, additionalParams, valuesYaml);
        }

        Assertions.assertEquals("additionalParamValue", additionalParams.get("test.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("test"));
        Assertions.assertEquals("additionalParamValue", ((Map<?, ?>) valuesYaml.get("test")).get("property"));
    }

    @Test
    public void testDowngradeParamsAreMergedIfPresent() {
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> valuesYaml = new HashMap<>();
        Property downgradeProperty = new Property();
        downgradeProperty.setDefaultValue("defaultValue");
        Map<String, Property> defaultProperties = createMutableMap("downgrade.property", downgradeProperty);

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getVnfdDowngradeParams(any(), any(), any(), any(), any()))
                    .thenReturn(defaultProperties);
            vnfdUtils.when(() -> parseVnfdParameter(anyString()))
                    .thenCallRealMethod();

            vnfdParametersHelper.mergeDefaultDowngradeParameters(createDummyVnfInstance(),
                    new ChangeCurrentVnfPkgRequest("DUMMY_TARGET_VNFD_ID"), INSTANTIATE, additionalParams, valuesYaml);
        }
        Assertions.assertEquals("defaultValue", additionalParams.get("downgrade.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("downgrade"));
        Assertions.assertEquals("defaultValue", ((Map<?, ?>) valuesYaml.get("downgrade")).get("property"));
    }

    @Test
    public void testUpgradeParamsAreMergedWhenDowngradeParamsAbsent() {
        Map<String, Object> additionalParams = new HashMap<>();
        Map<String, Object> valuesYaml = new HashMap<>();
        Map<String, PropertiesModel> defaultProperties = createMutableMap("upgrade.property", new PropertiesModel("defaultValue"));

        try (MockedStatic<VnfdUtils> vnfdUtils = mockStatic(VnfdUtils.class)) {
            vnfdUtils.when(() -> VnfdUtils.getVnfdDowngradeParams(any(), any(), any(), any(), any()))
                    .thenReturn(null);
            vnfdUtils.when(() -> parseVnfdParameter(anyString()))
                    .thenCallRealMethod();
            vnfdUtils.when(() -> VnfdUtils.getOperationNameFromVnfd(any(), any()))
                    .thenReturn("DUMMY_OPERATION_NAME");
            vnfdUtils.when(() -> VnfdUtils.getPropertiesModelMap(any(), any()))
                    .thenReturn(defaultProperties);
            vnfdUtils.when(() -> parseVnfdParameter(anyString()))
                    .thenCallRealMethod();

            vnfdParametersHelper.mergeDefaultDowngradeParameters(createDummyVnfInstance(),
                    new ChangeCurrentVnfPkgRequest("DUMMY_TARGET_VNFD_ID"), INSTANTIATE, additionalParams, valuesYaml);
        }
        Assertions.assertEquals("defaultValue", additionalParams.get("upgrade.property"));
        Assertions.assertInstanceOf(Map.class, valuesYaml.get("upgrade"));
        Assertions.assertEquals("defaultValue", ((Map<?, ?>) valuesYaml.get("upgrade")).get("property"));
    }

    private static VnfInstance createDummyVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfDescriptorId("DUMMY_SOURCE_VNFD_ID");
        vnfInstance.setVnfSoftwareVersion("DUMMY_SOURCE_SOFTWARE_VERSION");
        return vnfInstance;
    }

    private static <K, T> Map<K, T> createMutableMap(K key, T value) {
        Map<K, T> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private PackageResponse createDummyPackageResponse() {
        PackageResponse packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel("{dummy: DUMMY_DESCRIPTOR}");
        return packageResponse;
    }
}
