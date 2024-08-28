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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Root;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToLifecycleOperationMapper;

public class TupleLifecycleOperationMapper implements TupleEntityMapper<LifecycleOperation> {
    @Override
    public Map<String, LifecycleOperation> map(final List<Tuple> tuples, final Root<LifecycleOperation> root) {
        return tuples.stream()
                .map(tuple -> TupleToLifecycleOperationMapper.map(tuple, root))
                .collect(Collectors.toMap(LifecycleOperation::getOperationOccurrenceId,
                                          Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
    }
}
