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
package com.ericsson.vnfm.orchestrator.service.tests.utils;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

public final class Utils {

    private static final Logger LOGGER = getLogger(Utils.class);

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static void delay(final long timeInMillis) {
        try {
            LOGGER.debug("Sleeping for {} milliseconds\n", timeInMillis);
            Thread.sleep(timeInMillis);
        } catch (final InterruptedException e) {
            LOGGER.debug("Caught InterruptedException: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
