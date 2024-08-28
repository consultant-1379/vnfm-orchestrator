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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import java.util.Optional;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.PolicyUtility;
import com.ericsson.am.shared.vnfd.model.policies.Policies;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VnfdHelper {

    /**
     * Retrieve the Policies section from the VNFD of the package
     *
     * @param vnfd Vnfd
     * @return An Optional which will either be empty or contain the policies
     */
    public Optional<Policies> getVnfdScalingInformation(final JSONObject vnfd) {
        final Policies policies = PolicyUtility.createPolicies(vnfd);
        return Optional.ofNullable(policies);
    }
}
