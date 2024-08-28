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
package com.ericsson.vnfm.orchestrator.presentation.services.validator;

import java.util.Map;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;

public interface InstantiateVnfRequestValidatingService {

    /**
     * The following rule is used for validation:
     * "The target size for VNF instantiation may be specified in either instantiationLevelId or targetScaleLevelInfo,
     * but not both. If none of the two attributes (instantiationLevelId or targetScaleLevelInfo) are present,
     * the default instantiation level as declared in the VNFD shall be used"
     *
     * @param instantiateVnfRequest InstantiateVnfRequest object
     */
    void validateScaleLevelInfo(InstantiateVnfRequest instantiateVnfRequest);

    void validateTimeouts(Map<?, ?> additionalParams);

    void validateNetworkDataTypes(String vnfd, InstantiateVnfRequest instantiateVnfRequest);

    void validateNamespace(Map<?, ?> additionalParams);

    void validateSkipVerification(Map<?, ?> additionalParams);

}
