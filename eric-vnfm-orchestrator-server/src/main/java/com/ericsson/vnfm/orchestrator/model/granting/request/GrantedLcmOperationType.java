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
package com.ericsson.vnfm.orchestrator.model.granting.request;

public enum GrantedLcmOperationType {
    INSTANTIATE,
    SCALE,
    SCALE_TO_LEVEL,
    CHANGE_FLAVOUR,
    TERMINATE,
    HEAL,
    OPERATE,
    CHANGE_EXT_CONN,
    CHANGE_VNFPKG,
    CREATE_SNAPSHOT,
    REVERT_TO_SNAPSHOT
}
