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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.LEVEL_ID_NOT_PRESENT_IN_VNFD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RetrieveDataException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.InstantiationLevelServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.sync.SyncOperationValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
      ReplicaDetailsMapper.class,
      InstantiationLevelServiceImpl.class,
      ObjectMapper.class
})
@MockBean(SyncOperationValidator.class)
public class InstantiationLevelServiceImplTest {

    @Autowired
    private InstantiationLevelServiceImpl instantiationLevelService;

    @Test
    public void testSetDefaultInstantiationLevel() {
        VnfInstance vnfInstance = createVnfInstance("validVnfInstancePolicies.json");
        instantiationLevelService.setDefaultInstantiationLevelToVnfInstance(vnfInstance);

        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo("instantiation_level_1");
    }

    @Test
    public void testErrorOnMultipleInstantiationLevels() {
        VnfInstance vnfInstance = createVnfInstance("vnfInstancePoliciesWithMultipleInstantiationLevels.json");

        assertThatThrownBy(() -> instantiationLevelService.setDefaultInstantiationLevelToVnfInstance(vnfInstance))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("More than one Instantiation level in VNFD");
    }

    @Test
    public void testTargetScaleLevelSetProperlyWithoutInstantiationLevelsInPolicies() throws JsonProcessingException {
        VnfInstance instance = createVnfInstanceWithScaleLevelInfo("vnfInstancePoliciesWithoutInstantiationLevels.json");
        List<ScaleInfo> targetScaleLevelInfo = List.of(
                new ScaleInfo().aspectId("aspect-1").scaleLevel(2),
                new ScaleInfo().aspectId("aspect-2").scaleLevel(5)
        );

        Policies policies = new ObjectMapper().readValue(instance.getPolicies(), Policies.class);
        instantiationLevelService.setScaleLevelForTargetScaleLevelInfo(instance, targetScaleLevelInfo, policies);

        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-1", 2);
        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-2", 5);
        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-3", 0);
    }

    @Test
    public void testTargetScaleLevelSetProperlyWithDefaultInstantiationLevel() throws JsonProcessingException {
        VnfInstance instance = createVnfInstanceWithScaleLevelInfo("vnfInstancePoliciesWithInstantiationLevels.json");
        List<ScaleInfo> targetScaleLevelInfo = List.of(
                new ScaleInfo().aspectId("aspect-2").scaleLevel(5)
        );

        Policies policies = new ObjectMapper().readValue(instance.getPolicies(), Policies.class);
        instantiationLevelService.setScaleLevelForTargetScaleLevelInfo(instance, targetScaleLevelInfo, policies);

        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-1", 0);
        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-2", 5);
        assertScaleInfoEntityLevel(instance.getScaleInfoEntity(), "aspect-3", 12);
    }

    @Test
    public void testValidateInstantiationLevelIdForNotPresentInstantiationLevelIdInVnfd() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("kube-system", "default");
        instantiateVnfRequest.setInstantiationLevelId("test-instantiation-level");

        VnfInstance vnfInstance = createVnfInstance("vnfInstancePoliciesWithNotPresentInstantiationLevelId.json");

        assertThatThrownBy(() -> instantiationLevelService.
                validateInstantiationLevelInPoliciesOfVnfInstance(vnfInstance, instantiateVnfRequest.getInstantiationLevelId()))
                .isInstanceOf(InvalidInputException.class).hasMessage(String.format(LEVEL_ID_NOT_PRESENT_IN_VNFD, "test-instantiation-level"));
    }

    @Test
    public void testValidateInstantiationLevelIdForWithInvalidPolicyInVnfd() {
        InstantiateVnfRequest instantiateVnfRequest = createInstantiateVnfRequestBody("kube-system", "default");
        instantiateVnfRequest.setInstantiationLevelId("test-instantiation-level");

        VnfInstance vnfInstance = createVnfInstance("invalidVnfInstancePolicies.json");

        assertThatThrownBy(() -> instantiationLevelService.
                validateInstantiationLevelInPoliciesOfVnfInstance(vnfInstance, instantiateVnfRequest.getInstantiationLevelId()))
                .isInstanceOf(RetrieveDataException.class).hasMessage("Invalid format of policies stored in db for instance id "
                                                                                 + vnfInstance.getVnfInstanceId());
    }

    @Test
    public void testSetDefaultInstantiationLevelWithNullScaleInfo() throws JsonProcessingException {
        VnfInstance vnfInstance = createVnfInstance("vnfInstancePoliciesWithNullScaleInfo.json");

        var vnfInstancePolicies = new ObjectMapper().readValue(vnfInstance.getPolicies(), Policies.class);
        String defaultLevel = instantiationLevelService.getDefaultInstantiationLevelFromPolicies(vnfInstancePolicies);
        vnfInstance.setInstantiationLevel(defaultLevel);
        instantiationLevelService.setScaleLevelForInstantiationLevel(vnfInstance, defaultLevel, vnfInstancePolicies);

        assertThat(vnfInstance.getInstantiationLevel()).isEqualTo("instantiation_level_1");
    }

    private static void assertScaleInfoEntityLevel(List<ScaleInfoEntity> scaleInfoEntities, String aspectId, int expectedLevel) {
        assertThat(scaleInfoEntities.stream()
                .filter( el -> el.getAspectId().equals(aspectId))
                .findFirst().get().getScaleLevel()
        ).isEqualTo(expectedLevel);
    }

    private ScaleInfoEntity createScaleLevelInfoEntity(String aspectId) {
        ScaleInfoEntity entity = new ScaleInfoEntity();
        entity.setAspectId(aspectId);
        entity.setScaleLevel(-1);
        return entity;
    }

    private VnfInstance createVnfInstanceWithScaleLevelInfo(String vnfPolicyFile) {
        VnfInstance instance = createVnfInstance(vnfPolicyFile);
        instance.setScaleInfoEntity(List.of(
                createScaleLevelInfoEntity("aspect-1"),
                createScaleLevelInfoEntity("aspect-2"),
                createScaleLevelInfoEntity("aspect-3")
        ));

        return instance;
    }

    private VnfInstance createVnfInstance(String fileName) {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("someId");
        vnfInstance.setScaleInfoEntity(List.of());
        vnfInstance.setPolicies(readDataFromFile(getClass(), fileName));
        return vnfInstance;
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
}