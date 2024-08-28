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
package com.ericsson.vnfm.orchestrator.validator;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;

public interface RespondentValidator<P, R> {

    /**
     * Validates the passed parameter. Returns a value that can be the result of a validation.
     * @param validatedParameter   - object that will be validated.
     * @return                     - any object that represents validation result.
     * @throws ValidationException - if object is not valid.
     */
    R validate(P validatedParameter);

}
