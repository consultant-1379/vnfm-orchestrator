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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;

public class GetInstanceByFilterPositiveBase extends GetInstancePositiveBase {

    @MockBean
    private VnfInstanceQuery vnfInstanceQuery;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate nfvoRetryTemplate;

    @BeforeEach
    public void mockRepository() {
        given(vnfInstanceQuery.getPageWithFilter(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(Collections.singletonList(getVnfInstance("1")), getPage(), 1));
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void stubOnboardingResponse() throws Exception {
        given(nfvoRetryTemplate.execute(any(RetryCallback.class)))
                .willReturn(new ResponseEntity<>(TestUtils.readDataFromFile("granting/vnfd/rel4_vnfd.yaml"), HttpStatus.OK));
    }
}
