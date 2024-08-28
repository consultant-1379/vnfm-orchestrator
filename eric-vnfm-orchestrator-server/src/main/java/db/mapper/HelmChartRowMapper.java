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
package db.mapper;

import static db.migration.MigrationUtilities.ID_COLUMN;
import static db.migration.MigrationUtilities.REPLICA_DETAILS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.RowMapper;

public class HelmChartRowMapper implements RowMapper<Pair<String, String>> {

    @Override
    public Pair<String, String> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        return new MutablePair<>(rs.getString(ID_COLUMN), rs.getString(REPLICA_DETAILS));
    }
}
