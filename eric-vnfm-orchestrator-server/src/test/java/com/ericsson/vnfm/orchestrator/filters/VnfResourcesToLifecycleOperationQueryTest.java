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
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.START_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.AUTOMATIC_INVOCATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CANCEL_PENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.CANCEL_MODE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.IS_AUTOMATIC_INVOCATION_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.IS_CANCEL_PENDING_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_OCCURRENCE_ID_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_STATE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.START_TIME_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.STATE_ENTERED_TIME_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.VNF_INSTANCE_ID_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_WITH_VNF_INSTANCE_ID;

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


@SpringBootTest(classes = {VnfResourcesToLifecycleOperationQuery.class})
@MockBean(classes = LifecycleOperationRepository.class)
public class VnfResourcesToLifecycleOperationQueryTest {

    @Autowired
    private VnfResourcesToLifecycleOperationQuery vnfResourcesToLifecycleOperationQuery;

    @Test
    public void testCreateFilterExpressionOneValueOperationState() {
        LifecycleOperationState expectedValue = LifecycleOperationState.COMPLETED;
        String value = expectedValue.name();
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(OPERATION_STATE_FILTER, value,
                                                              "eq");

        assertOneValueFilterExpression(OPERATION_STATE, OperandOneValue.EQUAL, expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueDate() {
        LocalDateTime expectedValue = LocalDateTime.now();
        String value = expectedValue.toString();
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(STATE_ENTERED_TIME_FILTER, value,
                                                              "eq");

        assertOneValueFilterExpression(STATE_ENTERED_TIME, OperandOneValue.EQUAL, expectedValue, result);

        result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(START_TIME_FILTER, value,
                                                "eq");

        assertOneValueFilterExpression(START_TIME, OperandOneValue.EQUAL, expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueOperationType() {
        LifecycleOperationType expectedValue = LifecycleOperationType.TERMINATE;
        String value = expectedValue.name();
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(OPERATION_FILTER, value,
                                                "neq");

        assertOneValueFilterExpression(LIFECYCLE_OPERATION_TYPE, OperandOneValue.NOT_EQUAL,
                                                 expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueCancelMode() {
        CancelModeType expectedValue = CancelModeType.FORCEFUL;
        String value = expectedValue.name();
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(CANCEL_MODE_FILTER, value,
                                                "neq");

        assertOneValueFilterExpression(CANCEL_MODE, OperandOneValue.NOT_EQUAL,
                                                 expectedValue, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueVnfInstanceId() {
        String value = VNF_INSTANCE_ID;
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(VNF_INSTANCE_ID_FILTER, value,
                                                "neq");

        assertOneValueFilterExpression(VNF_INSTANCE_WITH_VNF_INSTANCE_ID, OperandOneValue.NOT_EQUAL,
                                                 value, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionOneValueBoolean() {
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(IS_CANCEL_PENDING_FILTER, "true",
                                                "neq");

        assertOneValueFilterExpression(CANCEL_PENDING, OperandOneValue.NOT_EQUAL,
                                                 true, result);

        result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(IS_AUTOMATIC_INVOCATION_FILTER, "false",
                                                "neq");

        assertOneValueFilterExpression(AUTOMATIC_INVOCATION, OperandOneValue.NOT_EQUAL,
                                                 false, result);
    }

    @Test
    public void testCreateFilterExpressionOneValueString() {
        String value = OCCURRENCE_ID;
        FilterExpressionOneValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionOneValue(OPERATION_OCCURRENCE_ID_FILTER, value, "eq");

        assertOneValueFilterExpression("operationOccurrenceId", OperandOneValue.EQUAL,
                                                 value, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueOperationalState() {
        List<LifecycleOperationState> expectedValues = Arrays.asList(LifecycleOperationState.FAILED,
                                                                     LifecycleOperationState.PROCESSING);
        List<String> values = expectedValues.stream().map(LifecycleOperationState::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(OPERATION_STATE_FILTER, values,
                                                "cont");

        assertMultiValueFilterExpression(OPERATION_STATE, OperandMultiValue.CONTAINS,
                                                   expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueDate() {
        List<LocalDateTime> expectedValues = Arrays.asList(LocalDateTime.now().minusDays(2), LocalDateTime.now());
        List<String> values = expectedValues.stream().map(LocalDateTime::toString).collect(Collectors.toList());
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(STATE_ENTERED_TIME_FILTER, values,
                                                "in");

        assertMultiValueFilterExpression(STATE_ENTERED_TIME, OperandMultiValue.IN,
                                                   expectedValues, result);

        result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(START_TIME_FILTER, values,
                                                "in");

        assertMultiValueFilterExpression(START_TIME, OperandMultiValue.IN,
                                                   expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueOperationType() {
        List<LifecycleOperationType> expectedValues = Arrays.asList(LifecycleOperationType.INSTANTIATE,
                                                                    LifecycleOperationType.CHANGE_VNFPKG);
        List<String> values = expectedValues.stream().map(LifecycleOperationType::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(OPERATION_FILTER, values,
                                                  "in");

        assertMultiValueFilterExpression(LIFECYCLE_OPERATION_TYPE, OperandMultiValue.IN,
                                                   expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueCancelMode() {
        List<CancelModeType> expectedValues = Arrays.asList(CancelModeType.FORCEFUL,
                                                            CancelModeType.GRACEFUL);
        List<String> values = expectedValues.stream().map(CancelModeType::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
            .createFilterExpressionMultiValue(CANCEL_MODE_FILTER, values,
                                              "in");

        assertMultiValueFilterExpression(CANCEL_MODE, OperandMultiValue.IN,
                                         expectedValues, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValueVnfInstanceId() {
        List<String> values = Arrays.asList("vnfInstanceId1", "vnfInstanceId2");
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(VNF_INSTANCE_ID_FILTER, values,
                                                  "in");

        assertMultiValueFilterExpression(VNF_INSTANCE_WITH_VNF_INSTANCE_ID, OperandMultiValue.IN,
                                                   values, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionMultiValueString() {
        List<String> values = Arrays.asList("operationOccurrenceId1", "operationOccurrenceId2");
        FilterExpressionMultiValue result = vnfResourcesToLifecycleOperationQuery
                .createFilterExpressionMultiValue(OPERATION_OCCURRENCE_ID_FILTER, values,
                                                  "in");

        assertMultiValueFilterExpression(OPERATION_OCCURRENCE_ID, OperandMultiValue.IN,
                                                   values, result);
    }
}
