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

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToLifecycleOperationMapper;

public class TupleVnfInstanceLifecycleOperationsMapper implements TupleAssociationMapper<VnfInstance, LifecycleOperation> {
    @Override
    public void map(final Tuple tuple,
                    final Root<VnfInstance> root,
                    final Join<VnfInstance, LifecycleOperation> join,
                    final Map<String, VnfInstance> result) {
        String vnfInstanceId = tuple.get(root.get("vnfInstanceId"));
        VnfInstance vnfInstance = result.get(vnfInstanceId);
        final String operationOccurrenceId = tuple.get(join.get("operationOccurrenceId"));
        if (Objects.nonNull(operationOccurrenceId)) {
            LifecycleOperation lifecycleOperation = TupleToLifecycleOperationMapper.map(tuple, join);
            vnfInstance.getAllOperations().add(lifecycleOperation);
        }
    }
}
