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
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToVnfInstanceMapper;

public class TupleLifecycleOperationVnfInstanceMapper implements TupleAssociationMapper<LifecycleOperation, VnfInstance> {
    @Override
    public void map(final Tuple tuple, final Root<LifecycleOperation> root,
                    final Join<LifecycleOperation, VnfInstance> join, final Map<String, LifecycleOperation> result) {
        final String operationOccurrenceId = tuple.get(root.get("operationOccurrenceId"));
        LifecycleOperation lifecycleOperation = result.get(operationOccurrenceId);
        final String vnfInstanceId = tuple.get(join.get("vnfInstanceId"));
        if (Objects.nonNull(vnfInstanceId)) {
            // TODO: create context to save instances and fetch from it if exists
            VnfInstance vnfInstance = TupleToVnfInstanceMapper.map(tuple, join);
            lifecycleOperation.setVnfInstance(vnfInstance);
        }
    }
}
