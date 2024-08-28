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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATUS;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

@Component
public class ClusterConfigQuery extends CreateQueryFilter<ClusterConfigFile, ClusterConfigFileRepository> {

    private static final Map<String, MappingData> MAPPINGS = Map.of(
            NAME, new MappingData(NAME, DataType.STRING),
            STATUS, new MappingData(STATUS, DataType.ENUMERATION)
    );

    private final EnumValueConverter<ConfigFileStatus> statusValueConverter;

    @Autowired
    public ClusterConfigQuery(final ClusterConfigFileRepository jpaRepository) {
        super(MAPPINGS, jpaRepository);
        this.statusValueConverter = new EnumValueConverter<>(ConfigFileStatus.class);
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionOneValue createFilterExpressionOneValue(final String key, final String value, final String operand) {
        if (STATUS.equals(key)) {
            return QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), value, statusValueConverter);
        }
        return QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), value, Function.identity());
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionMultiValue createFilterExpressionMultiValue(final String key, final List<String> value, final String operand) {
        if (STATUS.equals(key)) {
            return QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), value, statusValueConverter);
        }
        return QueryUtility.getFilterExpression(operand, MAPPINGS.get(key), value, Function.identity());
    }
}
