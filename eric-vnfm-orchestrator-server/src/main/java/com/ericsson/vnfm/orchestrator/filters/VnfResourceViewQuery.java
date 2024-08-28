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

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.CLUSTER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.CANCEL_MODE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.START_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.VNF_DESCRIPTOR_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.AUTOMATIC_INVOCATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CANCEL_PENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CURRENT_OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.CURRENT_OPERATION_JOIN_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LAST_OPERATION_JOIN_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LAST_OPERATION_JOIN_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATIONS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATIONS_JOIN_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATION_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.GrantingConstants.GRANT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.CANCEL_MODE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.CURRENT_OPERATION_STATE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.GRANT_ID_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.IS_AUTOMATIC_INVOCATION_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.IS_CANCEL_PENDING_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_OCCURRENCE_ID_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_STATE_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.START_TIME_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.STATE_ENTERED_TIME_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.ADDED_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_PROVIDER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.LAST_STATE_CHANGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNFD_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNFD_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_INSTANCE_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PKG_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PRODUCT_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PROVIDER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_SOFTWARE_VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.vnfm.orchestrator.filters.core.FilterSpecification;
import com.ericsson.vnfm.orchestrator.filters.core.SpecificationContext;
import com.ericsson.vnfm.orchestrator.model.entity.CancelModeType;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;

@Component
public class VnfResourceViewQuery extends CreateQueryFilter<VnfResourceView, VnfResourceViewRepository> {

    private static final Map<String, MappingData> MAPPINGS = Map.ofEntries(
            entry(INSTANCE_ID, new MappingData(VNF_INSTANCE_ID, DataType.STRING)),
            entry(VNF_INSTANCE_NAME, new MappingData(VNF_INSTANCE_NAME, DataType.STRING)),
            entry(VNF_INSTANCE_DESCRIPTION, new MappingData(VNF_INSTANCE_DESCRIPTION, DataType.STRING)),
            entry(VNFD_ID, new MappingData(VNF_DESCRIPTOR_ID, DataType.STRING)),
            entry(VNF_PROVIDER, new MappingData(VNF_PROVIDER_NAME, DataType.STRING)),
            entry(VNF_PRODUCT_NAME, new MappingData(VNF_PRODUCT_NAME, DataType.STRING)),
            entry(VNF_SOFTWARE_VERSION, new MappingData(VNF_SOFTWARE_VERSION, DataType.STRING)),
            entry(VNFD_VERSION, new MappingData(VNFD_VERSION, DataType.STRING)),
            entry(VNF_PKG_ID, new MappingData(VNF_PKG_ID, DataType.STRING)),
            entry(CLUSTER_NAME, new MappingData(CLUSTER_NAME, DataType.STRING)),
            entry(NAMESPACE, new MappingData(NAMESPACE, DataType.STRING)),
            entry(INSTANTIATION_STATE, new MappingData(INSTANTIATION_STATE, DataType.ENUMERATION)),
            entry(ADDED_TO_OSS, new MappingData(ADDED_TO_OSS, DataType.BOOLEAN)),
            entry(OPERATION_OCCURRENCE_ID_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + OPERATION_OCCURRENCE_ID, DataType.STRING)),
            entry(OPERATION_STATE_FILTER, new MappingData(LAST_OPERATION_JOIN_PREFIX + OPERATION_STATE, DataType.ENUMERATION)),
            entry(CURRENT_OPERATION_STATE_FILTER, new MappingData(CURRENT_OPERATION_JOIN_PREFIX + OPERATION_STATE, DataType.ENUMERATION)),
            entry(STATE_ENTERED_TIME_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + STATE_ENTERED_TIME, DataType.DATE)),
            entry(START_TIME_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + START_TIME, DataType.DATE)),
            entry(GRANT_ID_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + GRANT_ID, DataType.STRING)),
            entry(OPERATION_FILTER, new MappingData(LAST_OPERATION_JOIN_PREFIX + LIFECYCLE_OPERATION_TYPE, DataType.ENUMERATION)),
            entry(IS_AUTOMATIC_INVOCATION_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + AUTOMATIC_INVOCATION, DataType.BOOLEAN)),
            entry(IS_CANCEL_PENDING_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + CANCEL_PENDING, DataType.BOOLEAN)),
            entry(CANCEL_MODE_FILTER, new MappingData(LIFECYCLE_OPERATIONS_JOIN_PREFIX + CANCEL_MODE, DataType.ENUMERATION))
    );

    private static final Map<String, String> JOIN_MAPPINGS = Map.of(CURRENT_OPERATION, LIFECYCLE_OPERATIONS);

    private static final EnumValueConverter<LifecycleOperationState> OPERATION_STATE_CONVERTER =
            new EnumValueConverter<>(LifecycleOperationState.class);
    private static final EnumValueConverter<LifecycleOperationType> OPERATION_TYPE_CONVERTER =
            new EnumValueConverter<>(LifecycleOperationType.class);

    private static final Map<String, EnumValueConverter<?>> ENUM_CONVERTERS = Map.of(
            INSTANTIATION_STATE, new EnumValueConverter<>(InstantiationState.class),
            LAST_OPERATION_JOIN_PREFIX + OPERATION_STATE, OPERATION_STATE_CONVERTER,
            CURRENT_OPERATION_JOIN_PREFIX + OPERATION_STATE, OPERATION_STATE_CONVERTER,
            LAST_OPERATION_JOIN_PREFIX + LIFECYCLE_OPERATION_TYPE, OPERATION_TYPE_CONVERTER,
            LIFECYCLE_OPERATIONS_JOIN_PREFIX + CANCEL_MODE, new EnumValueConverter<>(CancelModeType.class)
    );

    @Autowired
    public VnfResourceViewQuery(final VnfResourceViewRepository jpaRepository) {
        super(MAPPINGS, jpaRepository);
    }

    @Override
    public Specification<VnfResourceView> createSpecification(List<FilterExpressionOneValue<String>> allFilterExpressionOne,
                                                              List<FilterExpressionMultiValue<String>> allMultiFilterExpression) {
        List<FilterExpressionOneValue<?>> oneValueFilters = new ArrayList<>(allFilterExpressionOne.size());
        for (FilterExpressionOneValue filterExpressionOneValue : allFilterExpressionOne) {
            oneValueFilters.add(filterExpressionOneValue);
        }
        List<FilterExpressionMultiValue<?>> multiValueFilters = new ArrayList<>(allMultiFilterExpression.size());
        for (FilterExpressionMultiValue filterExpressionMultiValue : allMultiFilterExpression) {
            multiValueFilters.add(filterExpressionMultiValue);
        }
        return new VnfResourceViewSpecification(oneValueFilters, multiValueFilters);
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionOneValue createFilterExpressionOneValue(final String key, final String value, final String operand) {
        MappingData mappingData = MAPPINGS.get(key);
        FilterExpressionOneValue<?> filterExpression;
        if (mappingData.getDataType() == DataType.ENUMERATION) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, value, ENUM_CONVERTERS.get(mappingData.getMapping()));
        } else if (mappingData.getDataType() == DataType.BOOLEAN) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, value, QueryUtility::validateAndParseBoolean);
        } else if (mappingData.getDataType() == DataType.DATE) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, value, QueryUtility::validateAndParseDate);
        } else {
            filterExpression = QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), value, Function.identity());
        }
        if (mappingData.getMapping().indexOf('.') != -1) {
            filterExpression.setJoinType(JoinType.INNER);
        }
        return filterExpression;
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionMultiValue createFilterExpressionMultiValue(final String key, final List<String> values, final String operand) {
        MappingData mappingData = MAPPINGS.get(key);
        FilterExpressionMultiValue<?> filterExpression;
        if (mappingData.getDataType() == DataType.ENUMERATION) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, values, ENUM_CONVERTERS.get(mappingData.getMapping()));
        } else if (mappingData.getDataType() == DataType.BOOLEAN) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, values, QueryUtility::validateAndParseBoolean);
        } else if (mappingData.getDataType() == DataType.DATE) {
            filterExpression = QueryUtility.getFilterExpression(operand, mappingData, values, QueryUtility::validateAndParseDate);
        } else {
            filterExpression = QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), values, Function.identity());
        }
        if (mappingData.getMapping().indexOf('.') != -1) {
            filterExpression.setJoinType(JoinType.INNER);
        }
        return filterExpression;
    }

    private static class VnfResourceViewSpecification extends FilterSpecification<VnfResourceView> {
        private static final long serialVersionUID = 1;

        VnfResourceViewSpecification(final List<FilterExpressionOneValue<? extends Comparable>> oneValueFilters,
                                     final List<FilterExpressionMultiValue<? extends Comparable>> multiValueFilters) {
            super(oneValueFilters, multiValueFilters, JOIN_MAPPINGS, true);
        }

        @Override
        protected Collection<? extends Predicate> createExtraPredicates(final SpecificationContext<VnfResourceView> context) {
            CriteriaBuilder builder = context.getCriteriaBuilder();
            Join<?, VnfResourceView> lastOperationJoin = Optional.ofNullable(context.getJoins().get(LAST_OPERATION_JOIN_KEY))
                    .orElseGet(() -> context.getRoot().join(LAST_OPERATION_JOIN_KEY, JoinType.INNER));

            Predicate notInstantiated = builder.equal(context.getRoot().get(INSTANTIATION_STATE), InstantiationState.NOT_INSTANTIATED);
            Predicate modifyInfo = builder.equal(lastOperationJoin.get(LIFECYCLE_OPERATION_TYPE), LifecycleOperationType.MODIFY_INFO);

            Predicate terminated = builder.equal(lastOperationJoin.get(LIFECYCLE_OPERATION_TYPE), LifecycleOperationType.TERMINATE);
            Predicate completed = builder.equal(lastOperationJoin.get(OPERATION_STATE), LifecycleOperationState.COMPLETED);

            Predicate notTerminated = builder.and(terminated, completed).not();
            Predicate notModifyInfo = builder.and(notInstantiated, modifyInfo).not();
            return List.of(builder.isNotNull(context.getRoot().get(LAST_STATE_CHANGE)), notTerminated, notModifyInfo);
        }
    }
}
