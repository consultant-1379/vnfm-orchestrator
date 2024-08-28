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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.DEFAULT_PAGE_SIZE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.SORT_COLUMNS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_PERFORMED_TEXT;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildPaginationInfo;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.createPaginationHttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.vnfm.orchestrator.api.VnfLifecycleOperationsApi;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/vnflcm/v1")
public class VnfLcmOperationsController implements VnfLifecycleOperationsApi {

    @Autowired
    private VnfLcmOperationService vnfLcmOperationService;

    @Autowired
    private UsernameCalculationService usernameCalculationService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Override
    public ResponseEntity<List<VnfLcmOpOcc>> getAllLifecycleManagementOperations(final String accept,
                                                                                 final String filter,
                                                                                 final String nextpageOpaqueMarker,
                                                                                 final Integer size,
                                                                                 final List<String> sort,
                                                                                 final String type) {
        Pageable pageable = new PaginationUtils.PageableBuilder()
                .defaults(DEFAULT_PAGE_SIZE, STATE_ENTERED_TIME)
                .page(nextpageOpaqueMarker).size(size).sort(sort, SORT_COLUMNS)
                .build();

        Page<LifecycleOperation> lifecycleOperations = vnfLcmOperationService.getAllLcmOperationsPage(filter, pageable);

        PaginationInfo paginationInfo = buildPaginationInfo(lifecycleOperations);
        HttpHeaders httpHeaders = createPaginationHttpHeaders(paginationInfo);

        List<VnfLcmOpOcc> vnfLcmOpOccs = vnfLcmOperationService.mapToVnfLcmOpOcc(lifecycleOperations);

        return new ResponseEntity<>(new ArrayList<>(vnfLcmOpOccs), httpHeaders, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<VnfLcmOpOcc> getLifecycleManagementOperationById(String vnfLcmOpOccId, String accept) {

        VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.getLcmOperationByOccId(vnfLcmOpOccId);

        if (vnfLcmOpOcc != null) {
            return new ResponseEntity<>(vnfLcmOpOcc, HttpStatus.OK);
        } else {
            throw new NotFoundException(String.format("The vnfLcmOpOccId-%s does not exist", vnfLcmOpOccId));
        }
    }

    @Override
    public ResponseEntity<Void> rollbackLifecycleManagementOperationById(String vnfLcmOpOccId,
                                                                         String accept,
                                                                         String idempotencyKey) {

        Supplier<ResponseEntity<Void>> rollbackOperationSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(OPERATION_PERFORMED_TEXT, "Rollback a VNF-LCM", "VNF-LCM operation occurrence", vnfLcmOpOccId, requestUsername);

            vnfLcmOperationService.rollbackLifecycleOperationByOccId(vnfLcmOpOccId);

            LOGGER.info(OPERATION_IS_FINISHED_TEXT, "Rollback a VNF-LCM", "VNF-LCM operation occurrence", vnfLcmOpOccId);

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        };

        return idempotencyService.executeTransactionalIdempotentCall(rollbackOperationSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<VnfLcmOpOcc> failLifecycleManagementOperationById(String vnfLcmOpOccId,
                                                                            String accept,
                                                                            String idempotencyKey) {

        Supplier<ResponseEntity<VnfLcmOpOcc>> failOperationSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(OPERATION_PERFORMED_TEXT, "Update a VNF LCM operation occurrence to a FAILED state.", "VNF-LCM operation occurrence",
                        vnfLcmOpOccId, requestUsername);

            VnfLcmOpOcc vnfLcmOpOcc = vnfLcmOperationService.failLifecycleOperationByOccId(vnfLcmOpOccId);

            LOGGER.info(OPERATION_IS_FINISHED_TEXT, "Update a VNF LCM operation occurrence to a FAILED state.", "VNF-LCM operation occurrence",
                        vnfLcmOpOccId);

            return new ResponseEntity<>(vnfLcmOpOcc, HttpStatus.OK);
        };

        return idempotencyService.executeTransactionalIdempotentCall(failOperationSupplier, idempotencyKey);
    }
}
