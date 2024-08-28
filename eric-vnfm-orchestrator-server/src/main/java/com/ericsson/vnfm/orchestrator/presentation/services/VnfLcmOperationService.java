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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;

public interface VnfLcmOperationService {

    VnfLcmOpOcc getLcmOperationByOccId(String id);

    Page<LifecycleOperation> getAllLcmOperationsPage(String filter, Pageable pageable);

    List<VnfLcmOpOcc> mapToVnfLcmOpOcc(Page<LifecycleOperation> lifecycleOperationList);

    void rollbackLifecycleOperationByOccId(String id);

    VnfLcmOpOcc failLifecycleOperationByOccId(String id);
}
