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

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({ "prod" })
@Component
public class FlywayMigrationBeansRegistrar implements FlywayConfigurationCustomizer {
    private final List<BaseJavaMigration> migrationBeans;

    @Autowired
    public FlywayMigrationBeansRegistrar(final List<BaseJavaMigration> migrationBeans) {
        this.migrationBeans = migrationBeans; // NOSONAR
    }

    @Override
    public void customize(final FluentConfiguration configuration) {
        configuration.javaMigrations(migrationBeans.toArray(new BaseJavaMigration[migrationBeans.size()]));
    }
}
