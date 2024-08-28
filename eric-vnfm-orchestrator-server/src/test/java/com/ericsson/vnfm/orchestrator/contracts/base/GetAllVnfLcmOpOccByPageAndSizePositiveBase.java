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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

public class GetAllVnfLcmOpOccByPageAndSizePositiveBase extends GetAllVnfLcmOpOccPositiveBase {

    @BeforeEach
    public void setUpForPageAndSize() {
        when(lifecycleOperationPageMock.getSize()).thenReturn(2);
        when(lifecycleOperationPageMock.getTotalPages()).thenReturn(3);
        when(lifecycleOperationPageMock.getNumber()).thenReturn(1); // 0 Indexed
    }
}
