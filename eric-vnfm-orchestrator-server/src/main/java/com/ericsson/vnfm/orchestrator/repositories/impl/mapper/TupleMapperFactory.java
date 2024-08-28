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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation_;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView_;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleAssociationMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleLifecycleOperationVnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfInstanceHelmChartsMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfInstanceLifecycleOperationsMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfInstanceScaleInfoMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfInstanceTerminatedHelmChartsMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfResourceViewLifecycleOperationMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfResourceViewLifecycleOperationsMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association.TupleVnfResourceViewScaleInfoMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity.TupleEntityMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity.TupleLifecycleOperationMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity.TupleVnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity.TupleVnfResourceViewMapper;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.model.Association;

@Component
public class TupleMapperFactory {

    private final Map<Association<?, ?>, TupleAssociationMapper<?, ?>> associationMapperCache = new HashMap<>();
    private final Map<Class<?>, TupleEntityMapper<?>> entityMapperCache = new HashMap<>();

    public TupleMapperFactory() {
        associationMapperCache.put(new Association<>(VnfInstance_.allOperations.getName(), VnfInstance.class, LifecycleOperation.class),
                                   new TupleVnfInstanceLifecycleOperationsMapper());
        associationMapperCache.put(new Association<>(VnfInstance_.helmCharts.getName(), VnfInstance.class, HelmChart.class),
                                   new TupleVnfInstanceHelmChartsMapper());
        associationMapperCache.put(new Association<>(VnfInstance_.terminatedHelmCharts.getName(), VnfInstance.class, TerminatedHelmChart.class),
                                   new TupleVnfInstanceTerminatedHelmChartsMapper());
        associationMapperCache.put(new Association<>(VnfInstance_.scaleInfoEntity.getName(), VnfInstance.class, ScaleInfoEntity.class),
                                   new TupleVnfInstanceScaleInfoMapper());
        associationMapperCache.put(new Association<>(VnfResourceView_.allOperations.getName(), VnfResourceView.class, LifecycleOperation.class),
                                   new TupleVnfResourceViewLifecycleOperationsMapper());
        associationMapperCache.put(new Association<>(VnfResourceView_.lastLifecycleOperation.getName(),
                                                     VnfResourceView.class,
                                                     LifecycleOperation.class),
                                   new TupleVnfResourceViewLifecycleOperationMapper());
        associationMapperCache.put(new Association<>(VnfResourceView_.scaleInfoEntity.getName(), VnfResourceView.class, ScaleInfoEntity.class),
                                   new TupleVnfResourceViewScaleInfoMapper());
        associationMapperCache.put(new Association<>(LifecycleOperation_.vnfInstance.getName(), LifecycleOperation.class, VnfInstance.class),
                                   new TupleLifecycleOperationVnfInstanceMapper());

        entityMapperCache.put(VnfInstance.class, new TupleVnfInstanceMapper());
        entityMapperCache.put(LifecycleOperation.class, new TupleLifecycleOperationMapper());
        entityMapperCache.put(VnfResourceView.class, new TupleVnfResourceViewMapper());
    }

    @SuppressWarnings("unchecked")
    public <R, A> TupleAssociationMapper<R, A> getAssociationMapper(String associationName, Class<R> rootClass, Class<A> associationClass) {
        final Association<R, A> associationModel = new Association<>(associationName, rootClass, associationClass);
        return (TupleAssociationMapper<R, A>) associationMapperCache.get(associationModel);
    }

    @SuppressWarnings("unchecked")
    public <R> TupleEntityMapper<R> getEntityMapper(Class<R> rootClass) {
        return (TupleEntityMapper<R>) entityMapperCache.get(rootClass);
    }
}
