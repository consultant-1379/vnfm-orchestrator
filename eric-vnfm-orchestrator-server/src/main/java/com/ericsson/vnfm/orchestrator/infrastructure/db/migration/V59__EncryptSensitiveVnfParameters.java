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
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.StringIdConverter;
import com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.TableDefinition;
import com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.TransformFieldsMigration;
import com.ericsson.vnfm.orchestrator.infrastructure.db.migration.crypto.EncryptingTransformer;

@Profile({"disabled"})
@Component
@SuppressWarnings("squid:S00101")
public class V59__EncryptSensitiveVnfParameters extends TransformFieldsMigration {
    private static final String[] SENSITIVE_COLUMNS_VNF_INSTANCE = new String[] {
        "combined_values_file",
        "oss_topology", "add_node_oss_topology", "instantiate_oss_topology", "oss_node_protocol_file",
        "temp_instance", "sitebasic_file"
    };
    private static final String[] SENSITIVE_COLUMNS_LIFECYCLE_OPERATIONS = new String[] {
        "operation_params", "values_file_params", "combined_values_file"
    };
    private static final List<TableDefinition> MIGRATION_TABLES = List.of(
            new TableDefinition("app_vnf_instance",
                                SENSITIVE_COLUMNS_VNF_INSTANCE, SENSITIVE_COLUMNS_VNF_INSTANCE,
                                new String[] {"vnf_id"}, new StringIdConverter(SENSITIVE_COLUMNS_VNF_INSTANCE.length + 1)),
            new TableDefinition("app_lifecycle_operations",
                                SENSITIVE_COLUMNS_LIFECYCLE_OPERATIONS, SENSITIVE_COLUMNS_LIFECYCLE_OPERATIONS,
                                new String[] {"operation_occurrence_id"},
                                new StringIdConverter(SENSITIVE_COLUMNS_LIFECYCLE_OPERATIONS.length + 1))
    );

    @Autowired
    public V59__EncryptSensitiveVnfParameters(final EncryptingTransformer encryptingTransformer) {
        super(true, 8, encryptingTransformer);
    }

    @Override
    protected List<TableDefinition> getTableDefinitions() {
        return MIGRATION_TABLES;
    }
}
