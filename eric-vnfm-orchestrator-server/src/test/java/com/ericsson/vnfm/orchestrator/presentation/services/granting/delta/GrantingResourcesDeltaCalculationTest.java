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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.ScaleMappingContainerDetails;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduOsContainer;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduOsContainerDeployableUnit;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduVirtualBlockStorage;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinitionType;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourcesDeltaCalculationImpl;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        GrantingResourcesDeltaCalculationTest.CommonTest.class,
        GrantingResourcesDeltaCalculationTest.FalseExpressionsTest.class,
        GrantingResourcesDeltaCalculationTest.TrueExpressionsTest.class
})
public class GrantingResourcesDeltaCalculationTest {

    public static class CommonTest {
        private final GrantingResourcesDeltaCalculationImpl grantingResourcesDeltaCalculation = new GrantingResourcesDeltaCalculationImpl();

        @Test
        public void calculateAddResourcesEmptyNodes() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createEmptyTemplate(),
                    createTestDataReplicasCount(),
                    null);
            assertTrue(resourceDefinitions.isEmpty());
        }

        @Test
        public void calculateAddResourcesEmptyNewNode() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createEmptyTemplate(),
                    createTestDataReplicasCount(),
                    null);
            assertTrue(resourceDefinitions.isEmpty());
        }

        @Test
        public void calculateAddResourcesEmptyOriginNode() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestData(),
                    createTestDataReplicasCount(),
                    null);
            assertFalse(resourceDefinitions.isEmpty());
            assertEquals(2, resourceDefinitions.size());
        }

        @Test
        public void calculateAddResourcesDifferentNodes() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestDataWithNewNodes(),
                    createTestDataReplicasCount(),
                    null);
            assertFalse(resourceDefinitions.isEmpty());
            assertEquals(2, resourceDefinitions.size());
            assertTrue(containsResourceWithName(resourceDefinitions, "third_container"));
            assertTrue(containsResourceWithName(resourceDefinitions, "third_storage"));
        }

        @Test
        public void calculateAddResourcesDifferentNodesReversedData() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestData(),
                    createTestDataReplicasCount(),
                    null);
            assertFalse(resourceDefinitions.isEmpty());
            assertEquals(2, resourceDefinitions.size());
            assertTrue(containsResourceWithName(resourceDefinitions, "first_container"));
            assertTrue(containsResourceWithName(resourceDefinitions, "first_storage"));
        }

        @Test
        public void calculateAddResourcesWithDisabledResources() {
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestDataWithDisabledResources(),
                    createTestDataReplicasCount(),
                    null);
            assertFalse(resourceDefinitions.isEmpty());
            assertEquals(2, resourceDefinitions.size());

            assertTrue(containsResourceWithName(resourceDefinitions, "first_container"));
            assertTrue(containsResourceWithName(resourceDefinitions, "first_storage"));
            assertFalse(containsResourceWithName(resourceDefinitions, "disabled_container_1"));
            assertFalse(containsResourceWithName(resourceDefinitions, "disabled_container_2"));
            assertFalse(containsResourceWithName(resourceDefinitions, "disabled_storage_1"));
            assertFalse(containsResourceWithName(resourceDefinitions, "disabled_storage_2"));
        }

        @Test
        public void calculateResourcesDisabledByScalingMappingAndValuesFiles() throws URISyntaxException {
            Path valuesFile = TestUtils.getResource("valueFiles/test-values-for-granting.yaml");
            Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
            Map<String, ScaleMapping> scaleMapping = prepareScaleMapping();
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestDataForScalingMapping(),
                    createTestDataReplicasCountForScaleMapping(),
                    null);

            List<ResourceDefinition> completelyDisabledResources = grantingResourcesDeltaCalculation
                    .getCompletelyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);
            List<ResourceDefinition> partiallyDisabledResources = grantingResourcesDeltaCalculation
                    .getPartiallyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);

            assertEquals(2, completelyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                    .count());
            assertEquals(1, completelyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                    .count());
            assertEquals(2, partiallyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                    .count());
            assertEquals(0, partiallyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                    .count());
        }
    }

    public static class FalseExpressionsTest {

        private final GrantingResourcesDeltaCalculationImpl grantingResourcesDeltaCalculation = new GrantingResourcesDeltaCalculationImpl();

        public static Stream<String> getFalseExpressionsWhenOscduIdIsFalse() {
            return Stream.of("(test_cnf.enabled is NULL) || (test_cnf.enabled==true)",
                                "test_cnf.enabled==true",
                                "test_cnf.enabled is NULL");
        }

        @ParameterizedTest(name = "False expression: {0}")
        @MethodSource("getFalseExpressionsWhenOscduIdIsFalse")
        public void calculateResourcesDisabledByScalingMappingAndValuesFilesWithBooleanAllowedDeploymentWhenExpressionIsFalse(
                String deploymentAllowed) throws URISyntaxException {
            Path valuesFile = TestUtils.getResource("valueFiles/test-values-for-granting.yaml");
            Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
            Map<String, ScaleMapping> scaleMapping = prepareScaleMappingWithBooleanDeploymentAllowed(deploymentAllowed);
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestDataForScalingMapping(),
                    createTestDataReplicasCountForScaleMapping(),
                    null);

            List<ResourceDefinition> completelyDisabledResources = grantingResourcesDeltaCalculation
                    .getCompletelyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);
            List<ResourceDefinition> partiallyDisabledResources = grantingResourcesDeltaCalculation
                    .getPartiallyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);

            assertEquals(completelyDisabledResources.stream()
                                 .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                                 .count(), 2);
            assertEquals(completelyDisabledResources.stream()
                                 .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                                 .count(), 1);
            assertEquals(partiallyDisabledResources.stream()
                                 .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                                 .count(), 2);
            assertEquals(partiallyDisabledResources.stream()
                                 .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                                 .count(), 0);
        }
    }

    public static class TrueExpressionsTest {

        private final GrantingResourcesDeltaCalculationImpl grantingResourcesDeltaCalculation = new GrantingResourcesDeltaCalculationImpl();

        public static Stream<String> getTrueExpressionsWhenOscduIdIsFalse() {
            return Stream.of("(test_cnf.enabled is NULL) || (test_cnf.enabled==false)",
                                "test_cnf.enabled==false",
                                "(test_cnf.enabled==false ) & (test_cnf.enabled==false)");
        }

        @ParameterizedTest(name = "True expression: {0}")
        @MethodSource("getTrueExpressionsWhenOscduIdIsFalse")
        public void calculateResourcesDisabledByScalingMappingAndValuesFilesWithBooleanAllowedDeploymentWhenExpressionIsTrue(
                String deploymentAllowed) throws URISyntaxException {
            Path valuesFile = TestUtils.getResource("valueFiles/test-values-for-granting.yaml");
            Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(valuesFile);
            Map<String, ScaleMapping> scaleMapping = prepareScaleMappingWithBooleanDeploymentAllowed(deploymentAllowed);
            List<ResourceDefinition> resourceDefinitions = grantingResourcesDeltaCalculation.calculateResources(
                    createTestDataForScalingMapping(),
                    createTestDataReplicasCountForScaleMapping(),
                    null);

            List<ResourceDefinition> completelyDisabledResources = grantingResourcesDeltaCalculation
                    .getCompletelyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);
            List<ResourceDefinition> partiallyDisabledResources = grantingResourcesDeltaCalculation
                    .getPartiallyDisabledResources(resourceDefinitions, scaleMapping, valuesYamlMap);

            assertEquals(2, completelyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                    .count());
            assertEquals(1, completelyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                    .count());
            assertEquals(0, partiallyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.OSCONTAINER == resource.getType())
                    .count());
            assertEquals(0, partiallyDisabledResources.stream()
                    .filter(resource -> ResourceDefinitionType.STORAGE == resource.getType())
                    .count());
        }
    }

    private static NodeTemplate createEmptyTemplate() {
        var nodeTemplate = new NodeTemplate();
        nodeTemplate.setVduCompute(Collections.emptyList());
        nodeTemplate.setMciop(Collections.emptyList());
        nodeTemplate.setOsContainerDeployableUnit(Collections.emptyList());
        nodeTemplate.setVduOsContainer(Collections.emptyList());

        return nodeTemplate;
    }

    private static NodeTemplate createTemplate(List<VduOsContainerDeployableUnit> vduOsContainerDeployableUnits,
                                               List<VduOsContainer> vduOsContainers,
                                               List<VduVirtualBlockStorage> vduVirtualBlockStorages) {
        var nodeTemplate = new NodeTemplate();
        nodeTemplate.setOsContainerDeployableUnit(vduOsContainerDeployableUnits);
        nodeTemplate.setVduOsContainer(vduOsContainers);
        nodeTemplate.setVduVirtualBlockStorages(vduVirtualBlockStorages);

        return nodeTemplate;
    }

    private static Map<String, Integer> createTestDataReplicasCount() {
        return Map.of("first_vdu_os_container", 1);
    }

    private static Map<String, Integer> createTestDataReplicasCountForScaleMapping() {
        return Map.of("test-cnf-vnfc1", 2,
                      "test-cnf", 2);
    }

    private static Map<String, ScaleMapping> prepareScaleMapping() {
        Map<String, ScaleMapping> scaleMapping = ScaleMappingTestUtils.getScalingMapping(true);

        ScaleMappingContainerDetails vnfcContainer = new ScaleMappingContainerDetails();
        vnfcContainer.setDeploymentAllowed("test_cnf.enabled");
        scaleMapping.get("test-cnf-vnfc1").getContainers().put("test_cnf_container", vnfcContainer);

        return scaleMapping;
    }

    private static Map<String, ScaleMapping> prepareScaleMappingWithBooleanDeploymentAllowed(String deploymentAllowed) {
        Map<String, ScaleMapping> scaleMapping = ScaleMappingTestUtils.getScalingMapping(true);

        ScaleMappingContainerDetails vnfcContainer = new ScaleMappingContainerDetails();
        vnfcContainer.setDeploymentAllowed(deploymentAllowed);
        scaleMapping.get("test-cnf-vnfc1").getContainers().put("test_cnf_container", vnfcContainer);

        return scaleMapping;
    }

    private static NodeTemplate createTestDataWithDisabledResources() {
        NodeTemplate nodeTemplate = createTestData();

        var vduOsContainerDeployableUnit = new VduOsContainerDeployableUnit();
        Map<String, List<String>> requirements = new HashMap<>();
        requirements.put("container", List.of("disabled_container_1", "disabled_container_2"));
        requirements.put("virtual_storage", List.of("disabled_storage_1", "disabled_storage_2"));
        vduOsContainerDeployableUnit.setRequirements(requirements);
        vduOsContainerDeployableUnit.setVduComputeKey("disabled_vdu_os_container");

        var vduOsContainer1 = new VduOsContainer();
        vduOsContainer1.setNodeName("disabled_container_1");
        vduOsContainer1.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduOsContainer2 = new VduOsContainer();
        vduOsContainer2.setNodeName("disabled_container_2");
        vduOsContainer2.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduVirtualBlockStorage1 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage1.setName("disabled_storage_1");
        vduVirtualBlockStorage1.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        var vduVirtualBlockStorage2 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage2.setName("disabled_storage_2");
        vduVirtualBlockStorage2.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        List<VduOsContainerDeployableUnit> vduOsContainerDeployableUnits = new ArrayList<>(nodeTemplate.getOsContainerDeployableUnit());
        vduOsContainerDeployableUnits.addAll(List.of(vduOsContainerDeployableUnit));
        nodeTemplate.setOsContainerDeployableUnit(vduOsContainerDeployableUnits);

        List<VduOsContainer> vduOsContainers = new ArrayList<>(nodeTemplate.getVduOsContainer());
        vduOsContainers.addAll(List.of(vduOsContainer1, vduOsContainer2));
        nodeTemplate.setVduOsContainer(vduOsContainers);

        List<VduVirtualBlockStorage> vduVirtualBlockStorages = new ArrayList<>(nodeTemplate.getVduVirtualBlockStorages());
        vduVirtualBlockStorages.addAll(List.of(vduVirtualBlockStorage1, vduVirtualBlockStorage2));
        nodeTemplate.setVduVirtualBlockStorages(vduVirtualBlockStorages);

        return nodeTemplate;
    }

    private static NodeTemplate createTestData() {
        var vduOsContainerDeployableUnit = new VduOsContainerDeployableUnit();
        Map<String, List<String>> requirements = new HashMap<>();
        requirements.put("container", List.of("first_container", "second_container"));
        requirements.put("virtual_storage", List.of("first_storage", "second_storage"));
        vduOsContainerDeployableUnit.setRequirements(requirements);
        vduOsContainerDeployableUnit.setVduComputeKey("first_vdu_os_container");

        var vduOsContainer1 = new VduOsContainer();
        vduOsContainer1.setNodeName("first_container");
        vduOsContainer1.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduOsContainer2 = new VduOsContainer();
        vduOsContainer2.setNodeName("second_container");
        vduOsContainer2.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduVirtualBlockStorage1 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage1.setName("first_storage");
        vduVirtualBlockStorage1.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        var vduVirtualBlockStorage2 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage2.setName("second_storage");
        vduVirtualBlockStorage2.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        return createTemplate(List.of(vduOsContainerDeployableUnit),
                              List.of(vduOsContainer1, vduOsContainer2),
                              List.of(vduVirtualBlockStorage1, vduVirtualBlockStorage2));
    }

    private static NodeTemplate createTestDataWithNewNodes() {
        var vduOsContainerDeployableUnit = new VduOsContainerDeployableUnit();
        Map<String, List<String>> requirements = new HashMap<>();
        requirements.put("container", List.of("third_container", "second_container"));
        requirements.put("virtual_storage", List.of("third_storage", "second_storage"));
        vduOsContainerDeployableUnit.setRequirements(requirements);
        vduOsContainerDeployableUnit.setVduComputeKey("first_vdu_os_container");

        var vduOsContainer1 = new VduOsContainer();
        vduOsContainer1.setNodeName("second_container");
        vduOsContainer1.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduOsContainer2 = new VduOsContainer();
        vduOsContainer2.setNodeName("third_container");
        vduOsContainer2.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduVirtualBlockStorage1 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage1.setName("second_storage");
        vduVirtualBlockStorage1.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        var vduVirtualBlockStorage2 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage2.setName("third_storage");
        vduVirtualBlockStorage2.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        return createTemplate(List.of(vduOsContainerDeployableUnit),
                              List.of(vduOsContainer1, vduOsContainer2),
                              List.of(vduVirtualBlockStorage1, vduVirtualBlockStorage2));
    }

    private static NodeTemplate createTestDataForScalingMapping() {
        var vduOsContainerDeployableUnit1 = new VduOsContainerDeployableUnit();
        vduOsContainerDeployableUnit1.setVduComputeKey("test-cnf-vnfc1");
        Map<String, List<String>> requirements = new HashMap<>();
        requirements.put("container", List.of("vnfc1_container", "test_cnf_container"));
        requirements.put("virtual_storage", List.of("vnfc1_storage"));
        vduOsContainerDeployableUnit1.setRequirements(requirements);

        var vduOsContainer1 = new VduOsContainer();
        vduOsContainer1.setNodeName("vnfc1_container");
        vduOsContainer1.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduOsContainer2 = new VduOsContainer();
        vduOsContainer1.setNodeName("test_cnf_container");
        vduOsContainer1.setType("tosca.nodes.nfv.Vdu.OsContainer");

        var vduVirtualBlockStorage1 = new VduVirtualBlockStorage();
        vduVirtualBlockStorage1.setName("vnfc1_storage");
        vduVirtualBlockStorage1.setType("tosca.nodes.nfv.Vdu.VirtualBlockStorage");

        var vduOsContainerDeployableUnit2 = new VduOsContainerDeployableUnit();
        vduOsContainerDeployableUnit2.setVduComputeKey("test-cnf");
        Map<String, List<String>> requirements2 = new HashMap<>();
        requirements2.put("container", List.of("test_cnf_container"));
        requirements2.put("virtual_storage", List.of("vnfc1_storage"));
        vduOsContainerDeployableUnit2.setRequirements(requirements2);

        return createTemplate(List.of(vduOsContainerDeployableUnit1, vduOsContainerDeployableUnit2),
                              List.of(vduOsContainer1, vduOsContainer2),
                              List.of(vduVirtualBlockStorage1));
    }

    private static boolean containsResourceWithName(List<ResourceDefinition> resourceDefinitions, String name, int count) {
        final List<String> list = resourceDefinitions.stream()
                .map(ResourceDefinition::getResourceTemplateId)
                .flatMap(Collection::stream)
                .filter(templateId -> templateId.equals(name))
                .collect(Collectors.toList());
        return list.size() == count;
    }

    private static boolean containsResourceWithName(List<ResourceDefinition> resourceDefinitions, String name) {
        return containsResourceWithName(resourceDefinitions, name, 1);
    }

}
