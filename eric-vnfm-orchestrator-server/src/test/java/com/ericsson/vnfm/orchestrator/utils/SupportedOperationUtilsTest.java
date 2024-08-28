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
package com.ericsson.vnfm.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.SupportedOperationUtils.getOperationDetailByOperationName;
import static com.ericsson.vnfm.orchestrator.utils.SupportedOperationUtils.validateOperationIsSupported;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SupportedOperationUtilsTest {

    private final String OPERATIONS_ALL_SUPPORTED = "supported-operations/all-supported.json";

    private final String OPERATIONS_ALL_NOT_SUPPORTED = "supported-operations/all-not-supported.json";

    private final String OPERATIONS_ALL_NOT_SUPPORTED_WITH_ERRORS = "supported-operations/all-not-supported-with-error.json";

    @Test
    public void testGetOperationDetailByOperationNameSuccess() {
        final VnfInstance vnfInstance = createVnfInstance(OPERATIONS_ALL_SUPPORTED, "test");
        Map<String, Boolean> expectedOperations = LCMOperationsEnum.getList()
                .stream()
                .filter(lcm -> !LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.equals(lcm))
                .collect(Collectors.toMap(LCMOperationsEnum::getOperation, value -> true));
        expectedOperations.forEach((key, value) -> {
            OperationDetail operationDetailByOperationName = getOperationDetailByOperationName(vnfInstance, key).get();
            assertEquals(value, operationDetailByOperationName.isSupported());
            assertNull(operationDetailByOperationName.getErrorMessage());
        });
    }

    @Test
    public void testGetOperationDetailByOperationNameFailWithoutErrorMessage() {
        final VnfInstance vnfInstance = createVnfInstance(OPERATIONS_ALL_NOT_SUPPORTED, "test");
        Map<String, Boolean> expectedOperations = LCMOperationsEnum.getList()
                .stream()
                .filter(lcm -> !LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.equals(lcm))
                .collect(Collectors.toMap(LCMOperationsEnum::getOperation, value -> false));
        expectedOperations.forEach((key, value) -> {
            OperationDetail operationDetailByOperationName = getOperationDetailByOperationName(vnfInstance, key).get();
            assertEquals(value, operationDetailByOperationName.isSupported());
            assertNull(operationDetailByOperationName.getErrorMessage());
        });
    }

    @Test
    public void testValidateOperationIsSupportedFailWithoutCustomErrorMessage() {
        final VnfInstance vnfInstance = createVnfInstance(OPERATIONS_ALL_NOT_SUPPORTED, "test");
        assertThatThrownBy(() -> validateOperationIsSupported(vnfInstance, LCMOperationsEnum.HEAL.getOperation()))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessage("Operation heal is not supported for package test");
    }

    @Test
    public void testValidateOperationIsSupportedFailWitCustomErrorMessage() {
        final VnfInstance vnfInstance = createVnfInstance(OPERATIONS_ALL_NOT_SUPPORTED_WITH_ERRORS, "test");
        assertThatThrownBy(() -> validateOperationIsSupported(vnfInstance, LCMOperationsEnum.HEAL.getOperation()))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessage("Operation heal is not supported for package test due to cause: Heal validation error message");
    }

    @Test
    public void testGetOperationDetailByOperationNameFailWitErrorMessage() {
        final VnfInstance vnfInstance = createVnfInstance(OPERATIONS_ALL_NOT_SUPPORTED_WITH_ERRORS, "test");
        Map<String, Boolean> expectedOperations = LCMOperationsEnum.getList()
                .stream()
                .filter(lcm -> !LCMOperationsEnum.CHANGE_CURRENT_PACKAGE.equals(lcm))
                .collect(Collectors.toMap(LCMOperationsEnum::getOperation, value -> true));
        expectedOperations.put("scale", false);
        expectedOperations.put("heal", false);
        expectedOperations.forEach((key, value) -> {
            OperationDetail operationDetailByOperationName = getOperationDetailByOperationName(vnfInstance, key).get();
            assertEquals(value, operationDetailByOperationName.isSupported());
            if (LCMOperationsEnum.HEAL.getOperation().equals(key)) {
                assertEquals("Heal validation error message", operationDetailByOperationName.getErrorMessage());
            } else if (LCMOperationsEnum.SCALE.getOperation().equals(key)) {
                assertEquals("Scale validation error message", operationDetailByOperationName.getErrorMessage());
            } else {
                assertNull(operationDetailByOperationName.getErrorMessage());
            }
        });
    }

    private VnfInstance createVnfInstance(String fileName, String instanceId) {
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.INSTANTIATED);
        value.setVnfInstanceId(instanceId);
        value.setVnfPackageId("test");
        List<OperationDetail> operationDetails = getOperationDetails(fileName);
        value.setSupportedOperations(operationDetails);
        return value;
    }

    private List<OperationDetail> getOperationDetails(String fileName) {
        String file;
        try {
            file = readDataFromFile(getClass(), fileName);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(file, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
