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
package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@SuppressWarnings("squid:S00101")
public class V92_1__UpdateLifecycleOperationsWithUsername extends BaseJavaMigration {

    private static final String USERNAME = "username";

    @Override
    public void migrate(Context context) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(
                new SingleConnectionDataSource(context.getConnection(), true)
        );
        persistDefaultUsernameValue(jdbcTemplate);
    }

    private static void persistDefaultUsernameValue(final JdbcTemplate jdbcTemplate) {
        String sqlString = String.format(
                "UPDATE app_lifecycle_operations SET %1$s = '' WHERE %1$s IS NULL", USERNAME);
        jdbcTemplate.update(sqlString);
    }
}
