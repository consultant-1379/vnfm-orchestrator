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
package com.ericsson.vnfm.orchestrator.presentation.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;

public interface LifecycleOperationsService {
    /**
     * Retrieve all operations with pagination information
     *
     * @return Page<VnfResourceLifecycleOperation>
     */
    Page<VnfResourceLifecycleOperation> getLifecycleOperationsPage(String filters, Pageable pageable);

}
