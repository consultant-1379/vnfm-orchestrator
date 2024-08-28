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

import static java.util.Map.entry;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CANCEL_MODE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.IS_AUTOMATIC_INVOCATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.IS_CANCEL_PENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.START_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.AUTOMATIC_INVOCATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CANCEL_PENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.GrantingConstants.GRANT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_WITH_VNF_INSTANCE_ID;

import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;

public class LifecycleOperationQuery extends CreateQueryFilter<LifecycleOperation, LifecycleOperationRepository> {

    private static final Map<String, MappingData> MAPPINGS = Map.ofEntries(
            entry(ID, new MappingData(OPERATION_OCCURRENCE_ID, DataType.STRING)),
            entry(OPERATION_STATE, new MappingData(OPERATION_STATE, DataType.ENUMERATION)),
            entry(STATE_ENTERED_TIME, new MappingData(STATE_ENTERED_TIME, DataType.DATE)),
            entry(START_TIME, new MappingData(START_TIME, DataType.DATE)),
            entry(VNF_INSTANCE_ID, new MappingData(VNF_INSTANCE_WITH_VNF_INSTANCE_ID, DataType.STRING)),
            entry(GRANT_ID, new MappingData(GRANT_ID, DataType.STRING)),
            entry(OPERATION, new MappingData(LIFECYCLE_OPERATION_TYPE, DataType.ENUMERATION)),
            entry(IS_AUTOMATIC_INVOCATION, new MappingData(AUTOMATIC_INVOCATION, DataType.BOOLEAN)),
            entry(IS_CANCEL_PENDING, new MappingData(CANCEL_PENDING, DataType.BOOLEAN)),
            entry(CANCEL_MODE, new MappingData(CANCEL_MODE, DataType.ENUMERATION))
    );

    public LifecycleOperationQuery(LifecycleOperationRepository lifecycleOperationRepository) {
        super(MAPPINGS, lifecycleOperationRepository);
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionOneValue createFilterExpressionOneValue(String key, String value, String operand) {
        switch (key) {
            case OPERATION_STATE:
                return QueryUtility.getFilterExpressionForOperationSate(key, value, operand, MAPPINGS);
            case STATE_ENTERED_TIME:
            case START_TIME:
                return QueryUtility.getFilterExpressionForDateValue(key, value, operand, MAPPINGS);
            case OPERATION:
                return QueryUtility.getFilterExpressionForLifecycleOperationType(key, value, operand, MAPPINGS);
            case CANCEL_MODE:
                return QueryUtility.getFilterExpressionForCancelModeType(key, value, operand, MAPPINGS);
            case VNF_INSTANCE_ID:
                return QueryUtility.getFilterExpressionForJoin(key, value, operand, MAPPINGS);
            case IS_AUTOMATIC_INVOCATION:
            case IS_CANCEL_PENDING:
                return QueryUtility.getFilterExpressionForBooleanDataType(key, value, operand, MAPPINGS);
            default:
                return QueryUtility.getFilterExpressionForStringDataType(key, value, operand, MAPPINGS);
        }
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionMultiValue createFilterExpressionMultiValue(
            String key, List<String> values, String operand) {
        switch (key) {
            case OPERATION_STATE:
                return QueryUtility.getFilterExpressionForOperationSate(key, values, operand, MAPPINGS);
            case STATE_ENTERED_TIME:
            case START_TIME:
                return QueryUtility.getFilterExpressionForDateValue(key, values, operand, MAPPINGS);
            case OPERATION:
                return QueryUtility.getFilterExpressionForLifecycleOperationType(key, values, operand, MAPPINGS);
            case CANCEL_MODE:
                return QueryUtility.getFilterExpressionForCancelModeType(key, values, operand, MAPPINGS);
            case VNF_INSTANCE_ID:
                return QueryUtility.getFilterExpressionForJoin(key, values, operand, MAPPINGS);
            default:
                return QueryUtility.getFilterExpressionForStringDataType(key, values, operand, MAPPINGS);
        }
    }
}
