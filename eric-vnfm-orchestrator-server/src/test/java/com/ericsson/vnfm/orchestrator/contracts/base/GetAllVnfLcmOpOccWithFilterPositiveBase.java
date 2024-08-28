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

public class GetAllVnfLcmOpOccWithFilterPositiveBase extends GetAllVnfLcmOpOccPositiveBase {

    @BeforeEach
    public void setUpForWithFilter() {
        when(lifecycleOperationPageMock.getTotalElements()).thenReturn(1L);
    }
}
