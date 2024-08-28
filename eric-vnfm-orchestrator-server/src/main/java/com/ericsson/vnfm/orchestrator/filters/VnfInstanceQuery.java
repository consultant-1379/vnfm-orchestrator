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
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.VNF_DESCRIPTOR_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_OCCURRENCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.ASPECT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.SCALE_INFO_ENTITY_ASPECT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.SCALE_INFO_ENTITY_SCALE_LEVEL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.SCALE_LEVEL;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_PROVIDER_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNFD_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNFD_VERSION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_INSTANCE_DESCRIPTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PACKAGE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PKG_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PRODUCT_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_PROVIDER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.VNF_SOFTWARE_VERSION;

import java.util.List;
import java.util.Map;

import com.ericsson.am.shared.filter.CreateQueryFilter;
import com.ericsson.am.shared.filter.model.DataType;
import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.MappingData;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

public class VnfInstanceQuery extends CreateQueryFilter<VnfInstance, VnfInstanceRepository> {

    private static final Map<String, MappingData> MAPPINGS = Map.ofEntries(
            entry(OPERATION_OCCURRENCE_ID, new MappingData(OPERATION_OCCURRENCE_ID, DataType.STRING)),
            entry(ID, new MappingData(VNF_INSTANCE_ID, DataType.STRING)),
            entry(VNF_INSTANCE_NAME, new MappingData(VNF_INSTANCE_NAME, DataType.STRING)),
            entry(VNF_INSTANCE_DESCRIPTION, new MappingData(VNF_INSTANCE_DESCRIPTION,
                                                            DataType.STRING)),
            entry(VNFD_ID, new MappingData(VNF_DESCRIPTOR_ID, DataType.STRING)),
            entry(VNF_PROVIDER, new MappingData(VNF_PROVIDER_NAME, DataType.STRING)),
            entry(VNF_PRODUCT_NAME, new MappingData(VNF_PRODUCT_NAME, DataType.STRING)),
            entry(VNF_SOFTWARE_VERSION, new MappingData(VNF_SOFTWARE_VERSION, DataType.STRING)),
            entry(VNFD_VERSION, new MappingData(VNFD_VERSION, DataType.STRING)),
            entry(VNF_PKG_ID, new MappingData(VNF_PACKAGE_ID, DataType.STRING)),
            entry(CLUSTER_NAME, new MappingData(CLUSTER_NAME, DataType.STRING)),
            entry(INSTANTIATION_STATE, new MappingData(INSTANTIATION_STATE,
                                                       DataType.ENUMERATION)),
            entry(ASPECT_ID, new MappingData(SCALE_INFO_ENTITY_ASPECT_ID, DataType.STRING)),
            entry(SCALE_LEVEL, new MappingData(SCALE_INFO_ENTITY_SCALE_LEVEL, DataType.NUMBER))
    );

    public VnfInstanceQuery(VnfInstanceRepository vnfInstanceRepository) {
        super(MAPPINGS, vnfInstanceRepository);
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionOneValue createFilterExpressionOneValue(String key, String value, String operand) {
        switch (key) {
            case ASPECT_ID:
            case SCALE_LEVEL:
                return QueryUtility.getFilterExpressionForJoin(key, value, operand, MAPPINGS);
            default:
                return QueryUtility.createFilterExpressionOneValueForVnfInstance(key, value, operand, MAPPINGS);
        }
    }

    /**
     * Raw types refactor: EO-172074
     */
    @Override
    public FilterExpressionMultiValue createFilterExpressionMultiValue(
            String key, List<String> values, String operand) {
        switch (key) {
            case ASPECT_ID:
            case SCALE_LEVEL:
                return QueryUtility.getFilterExpressionForJoin(key, values, operand, MAPPINGS);
            default:
                return QueryUtility.createFilterExpressionMultiValueForVnfInstance(key, values, operand, MAPPINGS);
        }
    }
}
