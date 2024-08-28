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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.DEFAULT_TITLE_ERROR_FOR_LCM_OP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LcmOpErrorManagementServiceImpl implements LcmOpErrorManagementService {

    @Autowired
    private LcmOpErrorProcessorFactory lcmOpErrorProcessorFactory;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(final LifecycleOperation operation, final HttpStatus status, final String errorTitle, final String errorDetail) {
        final LcmOpErrorProcessor errorProcessor = lcmOpErrorProcessorFactory.getProcessor(operation.getLifecycleOperationType());
        errorProcessor.process(operation, status, errorTitle, errorDetail);
    }

    @Override
    @Transactional
    public void process(final LifecycleOperation operation, final Exception e) {
        if (e instanceof HttpServiceInaccessibleException) {
            LOGGER.warn("Skipping exception processing: external service is inaccessible, the operation will be retried automatically");
            return;
        }
        HttpStatus httpStatus = e instanceof NotFoundException ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        process(operation, httpStatus, DEFAULT_TITLE_ERROR_FOR_LCM_OP, e.getMessage());
    }
}
