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

import java.util.List;
import java.util.Map;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Root;

public interface TupleEntityMapper<R> {
    Map<String, R> map(List<Tuple> tuples, Root<R> root);
}
