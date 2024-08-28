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
package com.ericsson.vnfm.orchestrator.filters;

import static com.ericsson.am.shared.filter.FilterErrorMessage.INVALID_ENUMERATION_VALUE_ERROR_MESSAGE;

import java.util.EnumSet;
import java.util.function.Function;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EnumValueConverter<E extends Enum<E>> implements Function<String, E> {
    private Class<E> klass;

    @Override
    public E apply(String val) {
        if (val == null) {
            throw new IllegalArgumentException(prepareErrorMessage());
        }
        try {
            return Enum.valueOf(klass, val);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(prepareErrorMessage(), e);
        }
    }

    private String prepareErrorMessage() {
        return String.format(INVALID_ENUMERATION_VALUE_ERROR_MESSAGE, klass.getName(), EnumSet.allOf(klass));
    }
}
