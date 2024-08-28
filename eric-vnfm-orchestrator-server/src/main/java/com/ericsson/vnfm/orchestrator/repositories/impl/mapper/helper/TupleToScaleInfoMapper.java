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
package com.ericsson.vnfm.orchestrator.repositories.impl.mapper.helper;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;

import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;

public final class TupleToScaleInfoMapper {
    private TupleToScaleInfoMapper() {

    }

    public static <T> ScaleInfoEntity map(Tuple tuple, Join<T, ScaleInfoEntity> join) {
        return ScaleInfoEntity.builder()
                .scaleInfoId(tuple.get(join.get("scaleInfoId")))
                .scaleLevel(tuple.get(join.get("scaleLevel")))
                .aspectId(tuple.get(join.get("aspectId")))
                .build();
    }
}
