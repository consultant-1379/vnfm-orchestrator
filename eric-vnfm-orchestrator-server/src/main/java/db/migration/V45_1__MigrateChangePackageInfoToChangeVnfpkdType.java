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

import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@SuppressWarnings("squid:S00101")
public class V45_1__MigrateChangePackageInfoToChangeVnfpkdType extends BaseJavaMigration {

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllOperations(jdbcTemplate).stream()
                .forEach(row -> convertToChangeVnfPkg(row, jdbcTemplate));
    }

    private static List<String> getAllOperations(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList(
                "SELECT operation_occurrence_id FROM app_lifecycle_operations WHERE lifecycle_operation_type "
                        + "= 'CHANGE_PACKAGE_INFO'",
                String.class);
    }

    private static void convertToChangeVnfPkg(final String operationId, final JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("UPDATE app_lifecycle_operations set lifecycle_operation_type = 'CHANGE_VNFPKG' where operation_occurrence_id = ?",
                operationId);
    }
}
