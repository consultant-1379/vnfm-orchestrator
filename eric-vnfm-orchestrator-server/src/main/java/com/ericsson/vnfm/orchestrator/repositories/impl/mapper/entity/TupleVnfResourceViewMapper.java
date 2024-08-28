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

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper.TupleToVnfResourceViewMapper;

public class TupleVnfResourceViewMapper implements TupleEntityMapper<VnfResourceView> {
    @Override
    public Map<String, VnfResourceView> map(final List<Tuple> tuples, final Root<VnfResourceView> root) {
        return tuples.stream()
                .map(tuple -> TupleToVnfResourceViewMapper.map(tuple, root))
                .collect(Collectors.toMap(VnfResourceView::getVnfInstanceId,
                                          Function.identity(), (k1, k2) -> k1, LinkedHashMap::new));
    }
}
