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

/**
 * Interface is responsible for merging set of files represented in String
 */
public interface FileMerger {
    /**
     * Method is responsible for merging set of files represented in String.
     * Each next file overrides properties in previous files past in case of overlapping
     * @param fileContent set of file as strings
     * @return merged files
     */
    String merge(String... fileContent);
}
