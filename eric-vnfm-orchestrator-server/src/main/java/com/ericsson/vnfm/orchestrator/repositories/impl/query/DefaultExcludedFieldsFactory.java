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
package com.ericsson.vnfm.orchestrator.repositories.impl.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart_;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation_;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity_;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView_;

public final class DefaultExcludedFieldsFactory {

    private static final Map<Class<?>, List<String>> ENTITY_EXCLUDED_FIELDS = new HashMap<>();
    private static final String SERIAL_VERSION_UID_EXCLUDED_FIELD = "serialVersionUID";
    private static final List<String> DEFAULT_EXCLUDED_FIELDS = List.of(SERIAL_VERSION_UID_EXCLUDED_FIELD);

    static {
        final List<String> vnfInstanceExcludedFields = List.of(SERIAL_VERSION_UID_EXCLUDED_FIELD, VnfInstance_.ossTopology.getName(),
                                                               VnfInstance_.addNodeOssTopology.getName(), VnfInstance_.combinedValuesFile.getName(),
                                                               VnfInstance_.tempInstance.getName(), VnfInstance_.sitebasicFile.getName(),
                                                               VnfInstance_.ossNodeProtocolFile.getName(), VnfInstance_.sensitiveInfo.getName(),
                                                               VnfInstance_.instantiateOssTopology.getName(),
                                                               VnfInstance_.allOperations.getName(), VnfInstance_.helmCharts.getName(),
                                                               VnfInstance_.terminatedHelmCharts.getName(), VnfInstance_.scaleInfoEntity.getName(),
                                                               VnfResourceView_.LAST_LIFECYCLE_OPERATION);

        final List<String> lifecycleOperationExcludedFields = List.of(SERIAL_VERSION_UID_EXCLUDED_FIELD,
                                                                      LifecycleOperation_.valuesFileParams.getName(),
                                                                      LifecycleOperation_.COMBINED_VALUES_FILE,
                                                                      LifecycleOperation_.OPERATION_PARAMS, LifecycleOperation_.VNF_INSTANCE,
                                                                      LifecycleOperation_.LIFECYCLE_OPERATION_STAGE);

        final List<String> helmChartExcludedFields = List.of(SERIAL_VERSION_UID_EXCLUDED_FIELD, HelmChart_.VNF_INSTANCE);

        final List<String> scaleInfoExcludedFields = List.of(SERIAL_VERSION_UID_EXCLUDED_FIELD, ScaleInfoEntity_.VNF_INSTANCE);

        ENTITY_EXCLUDED_FIELDS.put(VnfInstance.class, vnfInstanceExcludedFields);
        ENTITY_EXCLUDED_FIELDS.put(LifecycleOperation.class, lifecycleOperationExcludedFields);
        ENTITY_EXCLUDED_FIELDS.put(HelmChart.class, helmChartExcludedFields);
        ENTITY_EXCLUDED_FIELDS.put(TerminatedHelmChart.class, helmChartExcludedFields);
        ENTITY_EXCLUDED_FIELDS.put(ScaleInfoEntity.class, scaleInfoExcludedFields);
        ENTITY_EXCLUDED_FIELDS.put(VnfResourceView.class, vnfInstanceExcludedFields);
    }

    private DefaultExcludedFieldsFactory() {

    }

    public static <T> List<String> getEntityExcludedFields(Class<T> entityClass) {
        return ENTITY_EXCLUDED_FIELDS.getOrDefault(entityClass, DEFAULT_EXCLUDED_FIELDS);
    }
}
