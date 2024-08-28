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

import static com.ericsson.vnfm.orchestrator.TestUtils.ADDITIONAL_PARAMS_FIELD;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON;
import static com.ericsson.vnfm.orchestrator.TestUtils.createDummyLifecycleOperation;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.DAY0_CONFIGURATION_PREFIX;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;

@SpringBootTest(classes = VnfLifecycleMapperImpl.class)
public class VnfLifecycleMapperTest {

    private static final String DAY0_CONFIGURATION_EXPOSED_MESSAGE = "VnfResourceLifecycleOperation contains forbidden day0.configuration params";

    @Autowired
    private VnfLifecycleMapper vnfLifecycleMapper;

    @Test
    public void testMapperCorrectlyMapsLifecycleOperations() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_PARAMS_JSON);
        VnfResourceLifecycleOperation vnfResourceLifecycleOperation = vnfLifecycleMapper.toInternalModel(lifecycleOperation);
        String operationParams = vnfResourceLifecycleOperation.getOperationParams();
        Assertions.assertFalse(operationParams.contains(DAY0_CONFIGURATION_PREFIX), DAY0_CONFIGURATION_EXPOSED_MESSAGE);
    }

    @Test
    public void testMapperWithNullAdditionalParameter() {
        LifecycleOperation lifecycleOperation = createDummyLifecycleOperation(LIFECYCLE_OPERATION_WITHOUT_ADDITIONAL_PARAMS_JSON);
        VnfResourceLifecycleOperation vnfResourceLifecycleOperation = vnfLifecycleMapper.toInternalModel(lifecycleOperation);
        String operationParams = vnfResourceLifecycleOperation.getOperationParams();
        JSONObject params = new JSONObject(operationParams);
        Assertions.assertTrue(params.has(ADDITIONAL_PARAMS_FIELD));
        Assertions.assertTrue(params.isNull(ADDITIONAL_PARAMS_FIELD));
    }
}