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
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common;

import lombok.Getter;
import lombok.Setter;

public class FieldsTransformingTask {
    @Getter
    private final TableDefinition tableDefinition;
    @Getter
    private final Object id;
    @Getter
    private final String[] selectedValues;
    @Getter
    private final String[] updateValues;
    @Getter
    @Setter
    private boolean succeeded;
    @Getter
    @Setter
    private Throwable failureCause;

    public FieldsTransformingTask(final TableDefinition tableDefinition,
                                  final Object id, final String[] selectedValues) {
        this.tableDefinition = tableDefinition;
        this.id = id;
        this.selectedValues = selectedValues;
        this.updateValues = new String[tableDefinition.getUpdateColumns().length];
    }
}
