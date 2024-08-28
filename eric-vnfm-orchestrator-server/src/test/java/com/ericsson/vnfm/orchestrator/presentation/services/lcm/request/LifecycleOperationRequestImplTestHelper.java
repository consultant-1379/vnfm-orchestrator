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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.OPERATE;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.test.context.TestComponent;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

@TestComponent
public class LifecycleOperationRequestImplTestHelper extends LifecycleRequestHandler {

    @Override
    public LifecycleOperationType getType() {
        return OPERATE;
    }

    @Override
    public void specificValidation(final VnfInstance vnfInstance, final Object request) {
    }

    public void updateInstance(final VnfInstance vnfInstance,
                               final Object request,
                               final LifecycleOperationType type,
                               final LifecycleOperation operation,
                               final Map<String, Object> additionalParams) {
    }

    public void sendRequest(final VnfInstance vnfInstance, final LifecycleOperation operation, final Object request, final Path toValuesFile) {
    }

    @Override
    public void createTempInstance(VnfInstance vnfInstance, Object request) {
    }
}
