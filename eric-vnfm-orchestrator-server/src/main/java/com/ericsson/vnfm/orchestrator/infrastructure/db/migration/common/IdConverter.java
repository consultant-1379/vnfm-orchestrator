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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * To perform a data migration, an id column(s) needed to be retrieved on data loading and later
 * used in update query. This interface defines methods for these purposes.
 *
 * Note: both methods depends on id position in corresponding SQL query. By default id column(s)
 * follows data columns.
 */
public interface IdConverter {
    /**
     * Extracts id in opaque form from current database row
     * @param resultSet result set to extract id from
     * @return    current row's key whatever it is
     * @throws SQLException
     */
    Object extractId(ResultSet resultSet) throws SQLException;

    /**
     * Applies an id typically retrieved by method above to {@link PreparedStatement}
     * typically performing an update.
     * @param preparedStatement UPDATE statement being executed
     * @param id opaque id containing row id to be set to the statement above
     * @throws SQLException
     */
    void setId(PreparedStatement preparedStatement, Object id) throws SQLException;
}
