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
package com.ericsson.vnfm.orchestrator.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.assertMultiValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.TestUtils.assertOneValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CANCEL_MODE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATUS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.ASPECT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.SCALE_INFO_ENTITY_ASPECT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ADDED_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;

import org.junit.jupiter.api.Test;

import com.ericsson.am.shared.filter.FilterErrorMessage;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

public class QueryUtilityTest {

    private static Map<String, MappingData> mapping;

    static {
        mapping = new HashMap<>();
        mapping.put(ID, new MappingData(OPERATION_OCCURRENCE_ID, DataType.STRING));
        mapping.put(OPERATION_STATE, new MappingData(OPERATION_STATE, DataType.ENUMERATION));
        mapping.put(STATE_ENTERED_TIME, new MappingData(STATE_ENTERED_TIME, DataType.DATE));
        mapping.put(OPERATION, new MappingData(LIFECYCLE_OPERATION_TYPE, DataType.ENUMERATION));
        mapping.put(CANCEL_MODE, new MappingData(CANCEL_MODE, DataType.ENUMERATION));
        mapping.put(INSTANTIATION_STATE, new MappingData(INSTANTIATION_STATE, DataType.ENUMERATION));
        mapping.put(ADDED_TO_OSS, new MappingData(ADDED_TO_OSS, DataType.BOOLEAN));
        mapping.put(ASPECT_ID, new MappingData(SCALE_INFO_ENTITY_ASPECT_ID, DataType.STRING));
        mapping.put(STATUS, new MappingData(STATUS, DataType.ENUMERATION));
    }

    @Test
    public void testCreateFilterExpressionOneValueForVnfInstanceForInstantiate() {
        FilterExpressionOneValue result = QueryUtility
                .createFilterExpressionOneValueForVnfInstance(INSTANTIATION_STATE, InstantiationState.INSTANTIATED.name(),
                                                              "eq", mapping);

        assertOneValueFilterExpression(INSTANTIATION_STATE, OperandOneValue.EQUAL,
                                                 InstantiationState.INSTANTIATED, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForVnfInstanceForAddedToOss() {
        FilterExpressionOneValue result = QueryUtility
                .createFilterExpressionOneValueForVnfInstance(ADDED_TO_OSS, "true",
                                                              "eq", mapping);

        assertOneValueFilterExpression(ADDED_TO_OSS, OperandOneValue.EQUAL, true, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForVnfInstanceForStringValue() {
        FilterExpressionOneValue result = QueryUtility
                .createFilterExpressionOneValueForVnfInstance("id", "testId",
                                                              "eq", mapping);

        assertOneValueFilterExpression(OPERATION_OCCURRENCE_ID, OperandOneValue.EQUAL, "testId", result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForVnfInstanceForInstantiate() {
        List<String> values = Arrays.stream(InstantiationState.values())
                .map(InstantiationState::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = QueryUtility
                .createFilterExpressionMultiValueForVnfInstance(INSTANTIATION_STATE, values,
                                                              "cont", mapping);

        List<InstantiationState> expectedValues = Arrays.asList(InstantiationState.values());

        assertMultiValueFilterExpression(INSTANTIATION_STATE, OperandMultiValue.CONTAINS, expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForVnfInstanceForStringValues() {
        List<String> values = Arrays.asList("Aspect1", "Aspect2");
        FilterExpressionMultiValue result = QueryUtility
                .createFilterExpressionMultiValueForVnfInstance(ASPECT_ID, values,
                                                                "in", mapping);

        assertMultiValueFilterExpression(SCALE_INFO_ENTITY_ASPECT_ID, OperandMultiValue.IN, values, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForOperationState() {
        FilterExpressionOneValue<LifecycleOperationState> result = QueryUtility
                .getFilterExpressionForOperationSate(OPERATION_STATE, LifecycleOperationState.COMPLETED.name(),
                                                     "neq", mapping);

        assertOneValueFilterExpression(OPERATION_STATE, OperandOneValue.NOT_EQUAL, LifecycleOperationState.COMPLETED, result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForOperationState() {
        List<LifecycleOperationState> expectedValues = Arrays.asList(LifecycleOperationState.FAILED, LifecycleOperationState.STARTING);
        List<String> values = expectedValues.stream().map(LifecycleOperationState::name).collect(Collectors.toList());
        FilterExpressionMultiValue<LifecycleOperationState> result = QueryUtility
                .getFilterExpressionForOperationSate(OPERATION_STATE, values,
                                                                "nin", mapping);

        assertMultiValueFilterExpression(OPERATION_STATE, OperandMultiValue.NOT_IN, expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForDate() {
        LocalDateTime value = LocalDateTime.now();
        FilterExpressionOneValue<ChronoLocalDateTime<?>> result = QueryUtility
                .getFilterExpressionForDateValue(STATE_ENTERED_TIME, value.toString(),
                                                     "eq", mapping);

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(STATE_ENTERED_TIME);
        assertThat(result.getOperation()).isEqualTo(OperandOneValue.EQUAL);
        assertThat(result.getValue()).isEqualTo(value);

        assertOneValueFilterExpression(STATE_ENTERED_TIME, OperandOneValue.EQUAL, value, result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForDate() {
        List<LocalDateTime> expectedValues = Arrays.asList(LocalDateTime.now().minusDays(2), LocalDateTime.now());
        List<String> values = expectedValues.stream().map(LocalDateTime::toString).collect(Collectors.toList());
        FilterExpressionMultiValue<ChronoLocalDateTime<?>> result = QueryUtility
                .getFilterExpressionForDateValue(STATE_ENTERED_TIME, values,
                                                                "in", mapping);

        assertMultiValueFilterExpression(STATE_ENTERED_TIME, OperandMultiValue.IN, expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForLifecycleOperationType() {
        FilterExpressionOneValue<LifecycleOperationType> result = QueryUtility
                .getFilterExpressionForLifecycleOperationType(OPERATION, LifecycleOperationType.INSTANTIATE.name(),
                                                     "neq", mapping);

        assertOneValueFilterExpression(LIFECYCLE_OPERATION_TYPE, OperandOneValue.NOT_EQUAL,
                                       LifecycleOperationType.INSTANTIATE, result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForLifecycleOperationType() {
        List<LifecycleOperationType> expectedValues = Arrays.asList(LifecycleOperationType.INSTANTIATE, LifecycleOperationType.HEAL);
        List<String> values = expectedValues.stream().map(LifecycleOperationType::name).collect(Collectors.toList());
        FilterExpressionMultiValue<LifecycleOperationType> result = QueryUtility
                .getFilterExpressionForLifecycleOperationType(OPERATION, values,
                                                                "ncont", mapping);

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(LIFECYCLE_OPERATION_TYPE);
        assertThat(result.getOperation()).isEqualTo(OperandMultiValue.NOT_CONTAINS);

        assertThat(result.getValues()).hasSize(values.size())
                .hasSameElementsAs(expectedValues);

        assertMultiValueFilterExpression(LIFECYCLE_OPERATION_TYPE, OperandMultiValue.NOT_CONTAINS,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForCancelModeType() {
        FilterExpressionOneValue<CancelModeType> result = QueryUtility
                .getFilterExpressionForCancelModeType(CANCEL_MODE, CancelModeType.FORCEFUL.name(),
                                                              "neq", mapping);

        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo(CANCEL_MODE);
        assertThat(result.getOperation()).isEqualTo(OperandOneValue.NOT_EQUAL);
        assertThat(result.getValue()).isEqualTo(CancelModeType.FORCEFUL);

        assertOneValueFilterExpression(CANCEL_MODE, OperandOneValue.NOT_EQUAL, CancelModeType.FORCEFUL, result);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForCancelModeType() {
        List<CancelModeType> expectedValues = List.of(CancelModeType.FORCEFUL);
        List<String> values = expectedValues.stream().map(CancelModeType::name).collect(Collectors.toList());
        FilterExpressionMultiValue<CancelModeType> result = QueryUtility
                .getFilterExpressionForCancelModeType(CANCEL_MODE, values,
                                                              "ncont", mapping);

        assertMultiValueFilterExpression(CANCEL_MODE, OperandMultiValue.NOT_CONTAINS, expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueForJoin() {
        FilterExpressionOneValue<String> result = QueryUtility
                .getFilterExpressionForJoin(ASPECT_ID, "Aspect1","eq", mapping);

        assertOneValueFilterExpression(SCALE_INFO_ENTITY_ASPECT_ID, OperandOneValue.EQUAL, "Aspect1", result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionMultipleValueForJoin() {
        List<String> values = Arrays.asList("Aspect1", "Aspect2");
        FilterExpressionMultiValue<String> result = QueryUtility
                .getFilterExpressionForJoin(ASPECT_ID, values,"ncont", mapping);

        assertMultiValueFilterExpression(SCALE_INFO_ENTITY_ASPECT_ID, OperandMultiValue.NOT_CONTAINS, values, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testGetFilterExpression() {
        FilterExpressionOneValue<ConfigFileStatus> result = QueryUtility.getFilterExpression("eq", mapping.get(STATUS),
                                                                           ConfigFileStatus.IN_USE.name(),
                                                                           new EnumValueConverter<>(ConfigFileStatus.class));

        assertOneValueFilterExpression(STATUS, OperandOneValue.EQUAL, ConfigFileStatus.IN_USE, result);

    }

    @Test
    public void testGetFilterExpressionMulti() {
        List<ConfigFileStatus> expectedValues = List.of(ConfigFileStatus.IN_USE);
        List<String> values = expectedValues.stream().map(ConfigFileStatus::name).collect(Collectors.toList());
        FilterExpressionMultiValue<ConfigFileStatus> result = QueryUtility.getFilterExpression("cont", mapping.get(STATUS),
                                                                           values,
                                                                           new EnumValueConverter<>(ConfigFileStatus.class));

        assertMultiValueFilterExpression(STATUS, OperandMultiValue.CONTAINS, expectedValues, result);
    }

    @Test
    public void testValidateAndParseBoolean() {
        Boolean trueResult = QueryUtility.validateAndParseBoolean("true");
        assertThat(trueResult).isTrue();
        Boolean falseResult = QueryUtility.validateAndParseBoolean("false");
        assertThat(falseResult).isFalse();

        assertThatThrownBy(() -> QueryUtility.validateAndParseBoolean("wrongValue"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(FilterErrorMessage.INVALID_BOOLEAN_VALUE_ERROR_MESSAGE, "wrongValue"));
    }

    @Test
    public void testValidateAndParseDate() {
        LocalDateTime result = QueryUtility.validateAndParseDate(LocalDateTime.now().toString());
        assertThat(result).isInstanceOf(LocalDateTime.class);

        assertThatThrownBy(() -> QueryUtility.validateAndParseDate("wrongValue"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(FilterErrorMessage.INVALID_DATE_VALUE_ERROR_MESSAGE, "wrongValue"));
    }
}
