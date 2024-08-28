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
package com.ericsson.vnfm.orchestrator.e2e.instantiate;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.E2E_INSTANTIATE_PACKAGE_VNFD_ID;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;


public class InstantiatePositiveTest extends AbstractEndToEndTest {

    @Test
    public void successfulInstantiateVnfInstanceContainsSupportedOperations() throws Exception {
        //Create Identifier
        final String releaseName = "end-to-end-all-supported-operations";
        VnfInstanceResponse vnfInstanceResponse = requestHelper.executeCreateVnfRequest(releaseName, E2E_INSTANTIATE_PACKAGE_VNFD_ID);

        //Assertions on state of instance
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceResponse.getId());
        assertThat(instance.getSupportedOperations()).isNotNull();
        assertThat(instance.getSupportedOperations()).hasSize(9);
    }
}
