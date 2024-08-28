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

import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfInstances.VNF_INSTANCE_NAME;

import org.springframework.data.domain.Pageable;

import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;

public class GetInstanceByPageAndSizePositiveBase extends GetInstancePositiveBase {

    @Override
    protected Pageable getPage() {
        return new PaginationUtils.PageableBuilder()
                .defaults(2, VNF_INSTANCE_NAME)
                .page(2)
                .build();
    }
}
