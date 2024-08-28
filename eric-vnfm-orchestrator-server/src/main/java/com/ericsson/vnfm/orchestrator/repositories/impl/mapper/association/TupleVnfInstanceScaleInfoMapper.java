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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.association;

import java.util.Map;
import java.util.Objects;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToScaleInfoMapper;

public class TupleVnfInstanceScaleInfoMapper implements TupleAssociationMapper<VnfInstance, ScaleInfoEntity> {
    @Override
    public void map(Tuple tuple, final Root<VnfInstance> root,
                    final Join<VnfInstance, ScaleInfoEntity> join, Map<String, VnfInstance> result) {
        String vnfInstanceId = tuple.get(root.get("vnfInstanceId"));
        VnfInstance vnfInstance = result.get(vnfInstanceId);
        final String scaleInfoId = tuple.get(join.get("scaleInfoId"));
        if (Objects.nonNull(scaleInfoId)) {
            ScaleInfoEntity scaleInfoEntity = TupleToScaleInfoMapper.map(tuple, join);
            vnfInstance.getScaleInfoEntity().add(scaleInfoEntity);
        }
    }
}
