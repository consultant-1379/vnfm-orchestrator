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
package com.ericsson.vnfm.orchestrator.presentation.controllers.internal;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Operations.SORT_COLUMNS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Operations.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildLinks;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildPaginationInfo;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.PAGE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.evnfm.orchestrator.api.OperationsApi;
import com.ericsson.vnfm.orchestrator.model.PagedOperationsResponse;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.services.LifecycleOperationsService;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;

@RestController
@RequestMapping("/api/v1")
public class EvnfmLifecycleOperationController implements OperationsApi {
    @Autowired
    private LifecycleOperationsService lifecycleOperationsService;

    @Override
    public ResponseEntity<PagedOperationsResponse> getAllOperations(final String filter,
                                                                    final Integer page,
                                                                    final Integer size,
                                                                    final List<String> sort) {

        Pageable pageable = new PaginationUtils.PageableBuilder()
                .defaults(STATE_ENTERED_TIME).page(page).size(size).sort(sort, SORT_COLUMNS).build();
        Page<VnfResourceLifecycleOperation> responsePage = lifecycleOperationsService.getLifecycleOperationsPage(filter, pageable);
        PaginationInfo paginationInfo = buildPaginationInfo(responsePage);
        PagedOperationsResponse response = new PagedOperationsResponse()
                .items(responsePage.getContent())
                .links(buildLinks(paginationInfo, PAGE))
                .page(paginationInfo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
