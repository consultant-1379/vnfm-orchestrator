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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.ADDITIONAL_PARAMS_FIELD;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyLifecycleOperation;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.VnfInfoModifications;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes =  {
        VnfLcmOpOccMapperImpl.class,
        LocalDateMapper.class,
        ObjectMapper.class })
public class VnfLcmOpOccMapperTest {

    private static final String DAY0_CONFIGURATION_EXPOSED_MESSAGE = "VnfLcmOpOcc contains forbidden day0.configuration params";

    @Autowired
    private VnfLcmOpOccMapper vnfLcmOpOccMapper;

    @Test
    public void testMapperCorrectlyMapsLifecycleOperations() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);
        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOpOccMapper.toInternalModel(lifecycleOperation, null);
        String operationParams = vnfLcmOpOcc.getOperationParams().toString();
        Assertions.assertFalse(operationParams.contains(DAY0_CONFIGURATION_PREFIX), DAY0_CONFIGURATION_EXPOSED_MESSAGE);
    }

    @Test
    public void testMapperCorrectlyMapsLifecycleOperationsWithChangedInfo() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);
        ChangedInfo changedInfo = createChangedInfo();
        Map<String, String> expectedMetaData = new HashMap<>();
        expectedMetaData.put("additionalProp1", "metaValue1");
        expectedMetaData.put("additionalProp2", "metaValue2");
        expectedMetaData.put("additionalProp3", "metaValue3");
        lifecycleOperation.setVnfInfoModifiableAttributesExtensions(
                "{\"vnfControlledScaling\":{\"Aspect1\":\"ManualControlled\",\"Aspect2\":\"CISMControlled\"}}");
        changedInfo.setLifecycleOperation(lifecycleOperation);
        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOpOccMapper.toInternalModel(lifecycleOperation, changedInfo);

        VnfInfoModifications changedInfoFromVnfLcmOpOcc = vnfLcmOpOcc.getChangedInfo();

        Assertions.assertNotNull(changedInfoFromVnfLcmOpOcc);
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfInstanceName()).isEqualTo(changedInfo.getVnfInstanceName());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfInstanceDescription()).isEqualTo(changedInfo.getVnfInstanceDescription());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfPkgId()).isEqualTo(changedInfo.getVnfPkgId());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfDescriptorId()).isEqualTo(changedInfo.getVnfDescriptorId());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfProviderName()).isEqualTo(changedInfo.getVnfProviderName());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfProductName()).isEqualTo(changedInfo.getVnfProductName());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfSoftwareVersion()).isEqualTo(changedInfo.getVnfSoftwareVersion());
        assertThat(changedInfoFromVnfLcmOpOcc.getVnfdVersion()).isEqualTo(changedInfo.getVnfdVersion());
        assertThat(changedInfoFromVnfLcmOpOcc.getMetadata()).isEqualTo(expectedMetaData);
        assertThat(changedInfoFromVnfLcmOpOcc.getExtensions()).isEqualTo(changedInfo.getVnfInfoModifiableAttributesExtensions());
    }

    @Test
    public void testMapperShouldWorkWithNullAdditionalParameter() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON);
        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOpOccMapper.toInternalModel(lifecycleOperation, null);
        String operationParams = vnfLcmOpOcc.getOperationParams().toString();
        JSONObject params = new JSONObject(operationParams);
        Assertions.assertTrue(params.has(ADDITIONAL_PARAMS_FIELD));
        Assertions.assertTrue(params.isNull(ADDITIONAL_PARAMS_FIELD));
    }

    private ChangedInfo createChangedInfo() {
        ChangedInfo changedInfo = new ChangedInfo();
        changedInfo.setVnfInstanceName("test-vnf");
        changedInfo.setVnfInstanceDescription("description");
        changedInfo.setVnfPkgId("12345-pkg-id");
        changedInfo.setMetadata("{\"additionalProp1\":\"metaValue1\",\"additionalProp2\":\"metaValue2\",\"additionalProp3\":\"metaValue3\"}");
        changedInfo.setVnfDescriptorId("12345-vnfd-id");
        changedInfo.setVnfProviderName("provider");
        changedInfo.setVnfProductName("product-name");
        changedInfo.setVnfSoftwareVersion("1.10");
        changedInfo.setVnfdVersion("1.2");
        changedInfo.setVnfInfoModifiableAttributesExtensions(
                "{\"vnfControlledScaling\":{\"Aspect1\":\"CISMControlled\",\"Aspect2\":\"ManualControlled\"}}");
        return changedInfo;
    }

}