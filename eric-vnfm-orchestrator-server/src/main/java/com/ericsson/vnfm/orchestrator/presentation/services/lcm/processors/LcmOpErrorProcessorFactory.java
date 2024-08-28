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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;

@Component
public class LcmOpErrorProcessorFactory {
    private final Map<LifecycleOperationType, LcmOpErrorProcessor> processorCache;

    @Autowired
    @Qualifier("defaultLcmOpErrorProcessor")
    private LcmOpErrorProcessor defaultLcmOpErrorProcessor;

    public LcmOpErrorProcessorFactory(List<LcmOpErrorProcessor> processors) {
        this.processorCache = processors.stream()
                .filter(processor -> !Objects.isNull(processor.getType()))
                .collect(Collectors.toMap(LcmOpErrorProcessor::getType, Function.identity()));
    }

    public LcmOpErrorProcessor getProcessor(LifecycleOperationType lifecycleOperationType) {
        return Optional.ofNullable(processorCache.get(lifecycleOperationType))
                .orElse(defaultLcmOpErrorProcessor);
    }
}
