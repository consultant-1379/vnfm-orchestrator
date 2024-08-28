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

@SuppressWarnings("squid:S00101")
public class V23_3__AddReleaseNameMappingToChartTable extends BaseJavaMigration {

    @Override
    public void migrate(final Context context) {
        MigrationUtilities.migrateDataInHelmCharts(context);
    }
}
