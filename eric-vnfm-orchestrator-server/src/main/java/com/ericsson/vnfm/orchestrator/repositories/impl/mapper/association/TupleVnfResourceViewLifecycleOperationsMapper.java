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
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToLifecycleOperationMapper;

public class TupleVnfResourceViewLifecycleOperationsMapper implements TupleAssociationMapper<VnfResourceView, LifecycleOperation> {
    @Override
    public void map(final Tuple tuple,
                    final Root<VnfResourceView> root,
                    final Join<VnfResourceView, LifecycleOperation> join,
                    final Map<String, VnfResourceView> result) {
        String vnfInstanceId = tuple.get(root.get("vnfInstanceId"));
        VnfResourceView vnfResourceView = result.get(vnfInstanceId);
        final String operationOccurrenceId = tuple.get(join.get("operationOccurrenceId"));
        if (Objects.nonNull(operationOccurrenceId)) {
            LifecycleOperation lifecycleOperation = TupleToLifecycleOperationMapper.map(tuple, join);
            vnfResourceView.getAllOperations().add(lifecycleOperation);
        }
    }
}
