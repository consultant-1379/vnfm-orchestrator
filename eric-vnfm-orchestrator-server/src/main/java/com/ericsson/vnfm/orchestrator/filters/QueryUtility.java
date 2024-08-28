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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ADDED_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;

import com.ericsson.am.shared.filter.FilterErrorMessage;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

import jakarta.persistence.criteria.JoinType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QueryUtility {

    public static FilterExpressionOneValue createFilterExpressionOneValueForVnfInstance(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        if (key.equals(INSTANTIATION_STATE)) {
            return getFilterExpressionForInstantiationState(key, value, operand, mapping);
        } else if (key.equals(ADDED_TO_OSS)) {
            return getFilterExpressionForBooleanDataType(key, value, operand, mapping);
        } else {
            return getFilterExpressionForStringDataType(key, value, operand, mapping);
        }
    }

    public static FilterExpressionMultiValue createFilterExpressionMultiValueForVnfInstance(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        if (key.equals(INSTANTIATION_STATE)) {
            return getFilterExpressionForInstantiationState(key, values, operand, mapping);
        } else {
            return getFilterExpressionForStringDataType(key, values, operand, mapping);
        }
    }

    public static FilterExpressionOneValue<LifecycleOperationState> getFilterExpressionForOperationSate(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<LifecycleOperationState> filterExpressionLifecycleOperationState = new
                FilterExpressionOneValue<>();
        filterExpressionLifecycleOperationState.setKey(mapping.get(key).getMapping());
        filterExpressionLifecycleOperationState.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateEnumeration(value, LifecycleOperationState.class);
        filterExpressionLifecycleOperationState.setValue(LifecycleOperationState.valueOf(value));
        return filterExpressionLifecycleOperationState;
    }

    public static FilterExpressionMultiValue<LifecycleOperationState> getFilterExpressionForOperationSate(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<LifecycleOperationState> filterExpressionLifecycleOperationState =
                new FilterExpressionMultiValue<>();
        filterExpressionLifecycleOperationState.setKey(mapping.get(key).getMapping());
        filterExpressionLifecycleOperationState.setOperation(OperandMultiValue.fromFilterOperation(operand));
        List<LifecycleOperationState> allLifecycleOperationState = new ArrayList<>();
        for (String value: values) {
            validateEnumeration(value, LifecycleOperationState.class);
            allLifecycleOperationState.add(LifecycleOperationState.valueOf(value));
        }
        filterExpressionLifecycleOperationState.setValues(allLifecycleOperationState);
        return filterExpressionLifecycleOperationState;
    }

    public static FilterExpressionOneValue<ChronoLocalDateTime<?>> getFilterExpressionForDateValue(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<ChronoLocalDateTime<?>> filterExpressionTime = new FilterExpressionOneValue<>();
        filterExpressionTime.setKey(mapping.get(key).getMapping());
        filterExpressionTime.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateDate(value);
        filterExpressionTime.setValue(LocalDateTime.parse(value));
        return filterExpressionTime;
    }

    public static FilterExpressionMultiValue<ChronoLocalDateTime<?>> getFilterExpressionForDateValue(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<ChronoLocalDateTime<?>> filterExpressionTime = new FilterExpressionMultiValue<>();
        filterExpressionTime.setKey(mapping.get(key).getMapping());
        filterExpressionTime.setOperation(OperandMultiValue.fromFilterOperation(operand));
        List<ChronoLocalDateTime<?>> allTime = new ArrayList<>();
        for (String value: values) {
            validateDate(value);
            allTime.add(LocalDateTime.parse(value));
        }
        filterExpressionTime.setValues(allTime);
        return filterExpressionTime;
    }

    public static FilterExpressionOneValue<LifecycleOperationType> getFilterExpressionForLifecycleOperationType(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<LifecycleOperationType> filterExpressionLifecycleOperationType = new
                FilterExpressionOneValue<>();
        filterExpressionLifecycleOperationType.setKey(mapping.get(key).getMapping());
        filterExpressionLifecycleOperationType.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateEnumeration(value, LifecycleOperationType.class);
        filterExpressionLifecycleOperationType.setValue(LifecycleOperationType.valueOf(value));
        return filterExpressionLifecycleOperationType;
    }

    public static FilterExpressionMultiValue<LifecycleOperationType> getFilterExpressionForLifecycleOperationType(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<LifecycleOperationType> filterExpressionLifecycleOperationType =
                new FilterExpressionMultiValue<>();
        filterExpressionLifecycleOperationType.setKey(mapping.get(key).getMapping());
        filterExpressionLifecycleOperationType.setOperation(OperandMultiValue.fromFilterOperation(operand));
        List<LifecycleOperationType> allLifecycleOperationType = new ArrayList<>();
        for (String value: values) {
            validateEnumeration(value, LifecycleOperationType.class);
            allLifecycleOperationType.add(LifecycleOperationType.valueOf(value));
        }
        filterExpressionLifecycleOperationType.setValues(allLifecycleOperationType);
        return filterExpressionLifecycleOperationType;
    }

    public static FilterExpressionOneValue<CancelModeType> getFilterExpressionForCancelModeType(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<CancelModeType> filterExpressionCancelModeType = new
                FilterExpressionOneValue<>();
        filterExpressionCancelModeType.setKey(mapping.get(key).getMapping());
        filterExpressionCancelModeType.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateEnumeration(value, CancelModeType.class);
        filterExpressionCancelModeType.setValue(CancelModeType.valueOf(value));
        return filterExpressionCancelModeType;
    }

    public static FilterExpressionMultiValue<CancelModeType> getFilterExpressionForCancelModeType(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<CancelModeType> filterExpressionCancelModeType =
                new FilterExpressionMultiValue<>();
        filterExpressionCancelModeType.setKey(mapping.get(key).getMapping());
        filterExpressionCancelModeType.setOperation(OperandMultiValue.fromFilterOperation(operand));
        List<CancelModeType> allCancelModeType = new ArrayList<>();
        for (String value: values) {
            validateEnumeration(value, CancelModeType.class);
            allCancelModeType.add(CancelModeType.valueOf(value));
        }
        filterExpressionCancelModeType.setValues(allCancelModeType);
        return filterExpressionCancelModeType;
    }

    public static FilterExpressionOneValue<String> getFilterExpressionForJoin(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<String> filterExpressionInstanceId = new FilterExpressionOneValue<>();
        filterExpressionInstanceId.setKey(mapping.get(key).getMapping());
        filterExpressionInstanceId.setOperation(OperandOneValue.fromFilterOperation(operand));
        filterExpressionInstanceId.setValue(value);
        filterExpressionInstanceId.setJoinType(JoinType.LEFT);
        return filterExpressionInstanceId;
    }

    public static FilterExpressionMultiValue<String> getFilterExpressionForJoin(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<String> filterExpressionInstanceId = new FilterExpressionMultiValue<>();
        filterExpressionInstanceId.setKey(mapping.get(key).getMapping());
        filterExpressionInstanceId.setOperation(OperandMultiValue.fromFilterOperation(operand));
        filterExpressionInstanceId.setValues(values);
        filterExpressionInstanceId.setJoinType(JoinType.LEFT);
        return filterExpressionInstanceId;
    }

    public static FilterExpressionOneValue<String> getFilterExpressionForStringDataType(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<String> filterExpression = new FilterExpressionOneValue<>();
        filterExpression.setKey(mapping.get(key).getMapping());
        filterExpression.setOperation(OperandOneValue.fromFilterOperation(operand));
        filterExpression.setValue(value);
        return filterExpression;
    }

    public static FilterExpressionMultiValue<String> getFilterExpressionForStringDataType(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<String> filterExpression = new FilterExpressionMultiValue<>();
        filterExpression.setKey(mapping.get(key).getMapping());
        filterExpression.setOperation(OperandMultiValue.fromFilterOperation(operand));
        filterExpression.setValues(values);
        return filterExpression;
    }

    public static FilterExpressionOneValue<Boolean> getFilterExpressionForBooleanDataType(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<Boolean> filterExpressionBoolean = new FilterExpressionOneValue<>();
        filterExpressionBoolean.setKey(mapping.get(key).getMapping());
        filterExpressionBoolean.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateBoolean(value);
        filterExpressionBoolean.setValue(Boolean.parseBoolean(value));
        return filterExpressionBoolean;
    }

    public static FilterExpressionOneValue<InstantiationState> getFilterExpressionForInstantiationState(
            String key, String value, String operand, Map<String, MappingData> mapping) {
        FilterExpressionOneValue<InstantiationState> filterExpressionInstantiationState = new
                FilterExpressionOneValue<>();
        filterExpressionInstantiationState.setKey(mapping.get(key).getMapping());
        filterExpressionInstantiationState.setOperation(OperandOneValue.fromFilterOperation(operand));
        validateEnumeration(value, InstantiationState.class);
        filterExpressionInstantiationState.setValue(InstantiationState.valueOf(value));
        return filterExpressionInstantiationState;
    }

    public static FilterExpressionMultiValue<InstantiationState> getFilterExpressionForInstantiationState(
            String key, List<String> values, String operand, Map<String, MappingData> mapping) {
        FilterExpressionMultiValue<InstantiationState> filterExpressionInstantiationState = new
                FilterExpressionMultiValue<>();
        filterExpressionInstantiationState.setKey(mapping.get(key).getMapping());
        filterExpressionInstantiationState.setOperation(OperandMultiValue.fromFilterOperation(operand));
        List<InstantiationState> allInstantiationState = new ArrayList<>();
        for (String value: values) {
            validateEnumeration(value, InstantiationState.class);
            allInstantiationState.add(InstantiationState.valueOf(value));
        }
        filterExpressionInstantiationState.setValues(allInstantiationState);
        return filterExpressionInstantiationState;
    }

    private static void validateDate(final String value) {
        try {
            LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException pe) {
            throw new IllegalArgumentException(String.format(FilterErrorMessage.INVALID_DATE_VALUE_ERROR_MESSAGE, value));
        }
    }

    private static void validateBoolean(String value) {
        if (!BooleanUtils.TRUE.equalsIgnoreCase(value) && !BooleanUtils.FALSE.equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(String.format(FilterErrorMessage.INVALID_BOOLEAN_VALUE_ERROR_MESSAGE, value));
        }
    }

    private static <E extends Enum<E>> void validateEnumeration(String value, Class<E> enumeration) {
        if (!EnumUtils.isValidEnum(enumeration, value)) {
            throw new IllegalArgumentException(String.format(FilterErrorMessage.INVALID_ENUMERATION_VALUE_ERROR_MESSAGE,
                                                             enumeration.getName(),
                                                             EnumSet.allOf(enumeration)));
        }
    }

    public static <V extends Comparable<V>> FilterExpressionOneValue<V> getFilterExpression(String op, MappingData mappingData,
                                                                      String val, Function<String, V> parser) {
        OperandOneValue operand = OperandOneValue.fromFilterOperation(op);
        V value = parser.apply(val);
        FilterExpressionOneValue<V> filterExpression = new FilterExpressionOneValue<>();
        filterExpression.setKey(mappingData.getMapping());
        filterExpression.setOperation(operand);
        filterExpression.setValue(value);
        return filterExpression;
    }

    public static <V extends Comparable<V>> FilterExpressionMultiValue<V> getFilterExpression(String op, MappingData mappingData,
                                                                        List<String> val, Function<String, V> parser) {
        OperandMultiValue operand = OperandMultiValue.fromFilterOperation(op);
        List<V> values = val.stream().map(parser).collect(Collectors.toList());
        FilterExpressionMultiValue<V> filterExpression = new FilterExpressionMultiValue<>();
        filterExpression.setKey(mappingData.getMapping());
        filterExpression.setOperation(operand);
        filterExpression.setValues(values);
        return filterExpression;
    }

    public static Boolean validateAndParseBoolean(String value) {
        if (BooleanUtils.TRUE.equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        } else if (BooleanUtils.FALSE.equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(
                String.format(FilterErrorMessage.INVALID_BOOLEAN_VALUE_ERROR_MESSAGE, value));
    }

    public static LocalDateTime validateAndParseDate(String value) {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException pe) {
            throw new IllegalArgumentException(
                    String.format(FilterErrorMessage.INVALID_DATE_VALUE_ERROR_MESSAGE, value));
        }
    }
}
