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
package com.ericsson.vnfm.orchestrator.model.entity;

public enum LifecycleOperationType {
    INSTANTIATE, SCALE, SCALE_TO_LEVEL, CHANGE_FLAVOUR, TERMINATE,
    HEAL, OPERATE, CHANGE_EXT_CONN, MODIFY_INFO, CHANGE_PACKAGE_INFO,
    CHANGE_VNFPKG, ROLLBACK, SYNC
}
