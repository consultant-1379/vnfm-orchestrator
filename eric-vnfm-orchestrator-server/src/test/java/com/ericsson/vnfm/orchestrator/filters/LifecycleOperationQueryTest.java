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

import static com.ericsson.vnfm.orchestrator.TestUtils.assertMultiValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.TestUtils.assertOneValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CANCEL_MODE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.IS_AUTOMATIC_INVOCATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.IS_CANCEL_PENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.START_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;


@SpringBootTest(classes = {
      LifecycleOperationQuery.class
})
@MockBean(classes = LifecycleOperationRepository.class)
public class LifecycleOperationQueryTest {

    @Autowired
    private LifecycleOperationQuery lifecycleQuery;

    @Test
    public void testCreateFilterExpressionOneValueOperationState() {
        LifecycleOperationState expectedValue = LifecycleOperationState.COMPLETED;
        String value = expectedValue.name();
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(OPERATION_STATE, value,
                                                "eq");

        assertOneValueFilterExpression(OPERATION_STATE, OperandOneValue.EQUAL, expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueDate() {
        LocalDateTime expectedValue = LocalDateTime.now();
        String value = expectedValue.toString();
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(STATE_ENTERED_TIME, value,
                                                "eq");

        assertOneValueFilterExpression(STATE_ENTERED_TIME, OperandOneValue.EQUAL, expectedValue, result);

        result = lifecycleQuery
                .createFilterExpressionOneValue(START_TIME, value,
                                                "eq");

        assertOneValueFilterExpression(START_TIME, OperandOneValue.EQUAL, expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueOperationType() {
        LifecycleOperationType expectedValue = LifecycleOperationType.TERMINATE;
        String value = expectedValue.name();
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(OPERATION, value,
                                                "neq");

        assertOneValueFilterExpression("lifecycleOperationType", OperandOneValue.NOT_EQUAL,
                                       expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueCancelMode() {
        CancelModeType expectedValue = CancelModeType.FORCEFUL;
        String value = expectedValue.name();
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(CANCEL_MODE, value,
                                                "neq");

        assertOneValueFilterExpression(CANCEL_MODE, OperandOneValue.NOT_EQUAL,
                                       expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueVnfInstanceId() {
        String value = "vnfInstanceId";
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(VNF_INSTANCE_ID, value,
                                                "neq");

        assertOneValueFilterExpression("vnfInstance.vnfInstanceId", OperandOneValue.NOT_EQUAL,
                                       value, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionOneValueBoolean() {
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue(IS_CANCEL_PENDING, "true",
                                                "neq");

        assertOneValueFilterExpression("cancelPending", OperandOneValue.NOT_EQUAL,
                                       true, result);

        result = lifecycleQuery
                .createFilterExpressionOneValue(IS_AUTOMATIC_INVOCATION, "false",
                                                "neq");

        assertOneValueFilterExpression("automaticInvocation", OperandOneValue.NOT_EQUAL,
                                       false, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueString() {
        String value = "occurrenceId";
        FilterExpressionOneValue result = lifecycleQuery
                .createFilterExpressionOneValue("id", value,
                                                "eq");

        assertOneValueFilterExpression("operationOccurrenceId", OperandOneValue.EQUAL,
                                       value, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueOperationalState() {
        List<LifecycleOperationState> expectedValues = Arrays.asList(LifecycleOperationState.FAILED,
                                                                     LifecycleOperationState.PROCESSING);
        List<String> values = expectedValues.stream().map(LifecycleOperationState::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue(OPERATION_STATE, values,
                                                  "cont");

        assertMultiValueFilterExpression(OPERATION_STATE, OperandMultiValue.CONTAINS,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueDate() {
        List<LocalDateTime> expectedValues = Arrays.asList(LocalDateTime.now().minusDays(2), LocalDateTime.now());
        List<String> values = expectedValues.stream().map(LocalDateTime::toString).collect(Collectors.toList());
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue(STATE_ENTERED_TIME, values,
                                                  "in");

        assertMultiValueFilterExpression(STATE_ENTERED_TIME, OperandMultiValue.IN,
                                         expectedValues, result);

        result = lifecycleQuery
                .createFilterExpressionMultiValue(START_TIME, values,
                                                  "in");

        assertMultiValueFilterExpression(START_TIME, OperandMultiValue.IN,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueOperationType() {
        List<LifecycleOperationType> expectedValues = Arrays.asList(LifecycleOperationType.INSTANTIATE,
                                                                    LifecycleOperationType.CHANGE_VNFPKG);
        List<String> values = expectedValues.stream().map(LifecycleOperationType::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue(OPERATION, values,
                                                  "in");

        assertMultiValueFilterExpression("lifecycleOperationType", OperandMultiValue.IN,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueCancelMode() {
        List<CancelModeType> expectedValues = Arrays.asList(CancelModeType.FORCEFUL,
                                                            CancelModeType.GRACEFUL);
        List<String> values = expectedValues.stream().map(CancelModeType::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue(CANCEL_MODE, values,
                                                  "in");

        assertMultiValueFilterExpression(CANCEL_MODE, OperandMultiValue.IN,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueVnfInstanceId() {
        List<String> values = Arrays.asList("vnfInstanceId1", "vnfInstanceId2");
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue(VNF_INSTANCE_ID, values,
                                                  "in");

        assertMultiValueFilterExpression("vnfInstance.vnfInstanceId", OperandMultiValue.IN,
                                         values, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionMultiValueString() {
        List<String> values = Arrays.asList("operationOccurrenceId1", "operationOccurrenceId2");
        FilterExpressionMultiValue result = lifecycleQuery
                .createFilterExpressionMultiValue("id", values,
                                                  "in");

        assertMultiValueFilterExpression("operationOccurrenceId", OperandMultiValue.IN,
                                         values, result);
    }
}
