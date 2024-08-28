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
package com.ericsson.vnfm.orchestrator.utils;

public final class BooleanUtils {
    private BooleanUtils() {
    }

    public static Boolean getBooleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            if ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)) {
                return Boolean.valueOf((String) value);
            } else {
                throw new IllegalArgumentException(String.format("The value [%s] is not a boolean", value));
            }
        } else {
            throw new IllegalArgumentException(String.format("The value [%s] is not a boolean", value));
        }
    }

    public static Boolean getBooleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            if ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)) {
                return Boolean.valueOf((String) value);
            } else {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
}
