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

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_DESCRIPTOR_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RetrieveDataException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    VnfInstanceServiceImpl.class,
    ReplicaCountCalculationServiceImpl.class,
    ReplicaDetailsMapper.class,
    ExtensionsMapper.class,
    ObjectMapper.class
})
public class VnfInstanceServiceImplTest  {
    private static final String PAYLOAD = "Payload";
    private static final String INVALID_SCALE_PARAMETERS = "Invalid format of policies stored in db for instance id %s";
    private static final String POLICIES_NOT_PRESENT_ERROR_MESSAGE = "Policies not present for " + "instance %s";
    private static final String SCALE_INFO_MISSING_IN_VNF_INSTANCE =
        "Scale not supported as scale info is not present for vnf instance %s";
    private static final String DUMMY_PACKAGE_ID = "dummyPackageId";

    @Autowired
    private VnfInstanceServiceImpl vnfInstanceService;

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void testGetResourcesModelForInstanceWithoutPolicies() {
        VnfInstance vnfInstance = vnfInstanceWithBasicSetUp();
        assertThat(replicaCountCalculationService.getResourceDetails(vnfInstance)).isBlank();
    }

    @Test
    public void testGetResourcesModelForInstance() {
        VnfInstance vnfInstance = vnfInstanceWithBasicSetUp();
        vnfInstance.setPolicies(readDataFromFile(getClass(), "vnf-instance-service/policies-for-get-resources-model.json"));

        assertThat(replicaCountCalculationService.getResourceDetails(vnfInstance))
                .isNotBlank().isEqualTo("{\"TL_scaled_vm\":1,\"PL__scaled_vm\":1}");
    }

    @Test
    public void testGetScaleInfoForAspect() {
        VnfInstance vnfInstance = vnfInstanceWithBasicSetUp();
        List<ScaleInfoEntity> scaleInfoEntityList = doubleScaleInfoEntities();
        vnfInstance.setScaleInfoEntity(scaleInfoEntityList);

        Optional<ScaleInfoEntity> scaleInfo = vnfInstanceService.getScaleInfoForAspect(vnfInstance, PAYLOAD);
        assertThat(scaleInfo.isPresent()).isTrue();
    }

    @Test
    public void testGetScaleInfoForAspectNoPresent() {
        List<ScaleInfoEntity> scaleInfoEntityList = doubleScaleInfoEntities();
        VnfInstance vnfInstance = vnfInstanceWithBasicSetUp();
        vnfInstance.setScaleInfoEntity(scaleInfoEntityList);
        Optional<ScaleInfoEntity> scaleInfo = vnfInstanceService.getScaleInfoForAspect(vnfInstance, "test");
        assertThat(scaleInfo.isPresent()).isFalse();
    }

    @Test
    public void testGetCurrentScaleLevelWithEmptyVnfInstance() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId("test");
        assertThatThrownBy(() -> vnfInstanceService
            .getCurrentScaleInfo(instance)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage(String.format(SCALE_INFO_MISSING_IN_VNF_INSTANCE, "test"));
    }

    @Test
    public void testGetCurrentScaleLevelWithScaleNotSupported() {
        VnfInstance instance = vnfInstanceWithBasicSetUp();
        assertThatThrownBy(() -> vnfInstanceService
            .getCurrentScaleInfo(instance)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage(String.format(SCALE_INFO_MISSING_IN_VNF_INSTANCE, instance.getVnfInstanceId()));
    }

    @Test
    public void testGetPolicies() {
        VnfInstance vnfInstance = vnfInstanceWithBasicSetUp();
        vnfInstance.setPolicies(readDataFromFile(getClass(), "vnf-instance-service/policies-for-get-resources-model.json"));

        Policies policies = vnfInstanceService.getPolicies(vnfInstance);
        assertThat(policies).isNotNull();
        assertThat(policies.getAllInitialDelta()).isNotNull().isNotEmpty();
        assertThat(policies.getAllScalingAspectDelta()).isNotNull().isNotEmpty();
        assertThat(policies.getAllScalingAspects()).isNotNull().isNotEmpty();
        assertThat(policies.getAllInitialDelta().size()).isEqualTo(1);
        assertThat(policies.getAllScalingAspectDelta().size()).isEqualTo(1);
        assertThat(policies.getAllScalingAspects().size()).isEqualTo(1);
    }

    @Test
    public void testGetPoliciesWithPoliciesNotPresent() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId("test");
        assertThatThrownBy(() -> vnfInstanceService
            .getPolicies(instance))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(format(POLICIES_NOT_PRESENT_ERROR_MESSAGE, "test"));
    }

    @Test
    public void testGetPoliciesWithInvalidPoliciesInInstanceModel() {
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId("test");
        instance.setPolicies("[{\"ScalingAspects\": {}}]");

        assertThatThrownBy(() -> vnfInstanceService
                .getPolicies(instance)).isInstanceOf(RetrieveDataException.class)
                .hasMessage(String.format(INVALID_SCALE_PARAMETERS, "test"));
    }

    @Test
    public void testGetMaxScaleLevel() {
        VnfInstance vnfInstance = new VnfInstance();
        String aspectId = "Aspect1";
        int aspectMaxLevel = 10;

        String policies = TestUtils.createPoliciesWithSpecificInstantiationLevel(TestUtils.INST_LEVEL_1);
        vnfInstance.setPolicies(policies);

        int result = vnfInstanceService.getMaxScaleLevel(vnfInstance, aspectId);

        assertThat(result).isEqualTo(aspectMaxLevel);
    }

    private static List<ScaleInfoEntity> doubleScaleInfoEntities() {
        ScaleInfoEntity scaleInfo1 = new ScaleInfoEntity();
        scaleInfo1.setScaleInfoId("scale-id-1");
        scaleInfo1.setAspectId("Payload");
        scaleInfo1.setScaleLevel(0);
        ScaleInfoEntity scaleInfo2 = new ScaleInfoEntity();
        scaleInfo2.setScaleInfoId("scale-id-2");
        scaleInfo2.setAspectId("Payload_2");
        scaleInfo2.setScaleLevel(0);
        List<ScaleInfoEntity> scaleInfoEntityList = List.of(scaleInfo1, scaleInfo2);
        return scaleInfoEntityList;
    }

    private static VnfInstance vnfInstanceWithBasicSetUp() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(INSTANCE_ID);
        vnfInstance.setVnfInstanceName(DUMMY_INSTANCE_NAME);
        vnfInstance.setVnfInstanceDescription(DUMMY_INSTANCE_DESCRIPTION);
        vnfInstance.setVnfDescriptorId(DUMMY_DESCRIPTOR_ID);
        vnfInstance.setVnfPackageId(DUMMY_PACKAGE_ID);
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        return vnfInstance;
    }
}