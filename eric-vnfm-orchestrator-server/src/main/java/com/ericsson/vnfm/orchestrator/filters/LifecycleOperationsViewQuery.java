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

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationView;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.USERNAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.START_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PRODUCT_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_SOFTWARE_VERSION;

@Component
public class LifecycleOperationsViewQuery extends CreateQueryFilter<LifecycleOperationView, LifecycleOperationViewRepository> {

    private static final Map<String, MappingData> MAPPINGS = Map.ofEntries(
            entry(OPERATION_OCCURRENCE_ID, new MappingData(OPERATION_OCCURRENCE_ID, DataType.STRING)),
            entry(NAMESPACE, new MappingData(NAMESPACE, DataType.STRING)),
            entry(VNF_INSTANCE_NAME, new MappingData(VNF_INSTANCE_NAME, DataType.STRING)),
            entry(VNF_SOFTWARE_VERSION, new MappingData(VNF_SOFTWARE_VERSION, DataType.STRING)),
            entry(VNF_PRODUCT_NAME, new MappingData(VNF_PRODUCT_NAME, DataType.STRING)),
            entry(CLUSTER_NAME, new MappingData(CLUSTER_NAME, DataType.STRING)),
            entry(START_TIME, new MappingData(START_TIME, DataType.DATE)),
            entry(STATE_ENTERED_TIME, new MappingData(STATE_ENTERED_TIME, DataType.DATE)),
            entry(OPERATION_STATE, new MappingData(OPERATION_STATE, DataType.ENUMERATION)),
            entry(LIFECYCLE_OPERATION_TYPE, new MappingData(LIFECYCLE_OPERATION_TYPE, DataType.ENUMERATION)),
            entry(VNF_INSTANCE_ID, new MappingData(VNF_INSTANCE_ID, DataType.STRING)),
            entry(USERNAME, new MappingData(USERNAME, DataType.STRING))
    );

    @Autowired
    public LifecycleOperationsViewQuery(final LifecycleOperationViewRepository lifecycleOperationViewRepository) {
        super(MAPPINGS, lifecycleOperationViewRepository);
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionOneValue createFilterExpressionOneValue(String key, String value, String operand) {
        switch (key) {
            case OPERATION_STATE:
                return QueryUtility.getFilterExpressionForOperationSate(key, value, operand, MAPPINGS);
            case LIFECYCLE_OPERATION_TYPE:
                return QueryUtility.getFilterExpressionForLifecycleOperationType(key, value, operand, MAPPINGS);
            case STATE_ENTERED_TIME:
            case START_TIME:
                return QueryUtility.getFilterExpressionForDateValue(key, value, operand, MAPPINGS);
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
            case LIFECYCLE_OPERATION_TYPE:
                return QueryUtility.getFilterExpressionForLifecycleOperationType(key, values, operand, MAPPINGS);
            case STATE_ENTERED_TIME:
            case START_TIME:
                return QueryUtility.getFilterExpressionForDateValue(key, values, operand, MAPPINGS);
            default:
                return QueryUtility.getFilterExpressionForStringDataType(key, values, operand, MAPPINGS);
        }
    }
}
