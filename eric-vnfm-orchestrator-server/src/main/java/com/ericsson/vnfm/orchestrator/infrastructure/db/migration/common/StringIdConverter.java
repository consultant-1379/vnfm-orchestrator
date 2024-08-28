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

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StringIdConverter implements IdConverter {
    private final int selectIdPosition;
    private final int updateIdPosition;

    public StringIdConverter(final int idPosition) {
        this.selectIdPosition = idPosition;
        this.updateIdPosition = idPosition;
    }

    @Override
    public Object extractId(final ResultSet resultSet) throws SQLException {
        return resultSet.getString(selectIdPosition);
    }

    @Override
    public void setId(final PreparedStatement preparedStatement, final Object id) throws SQLException {
        preparedStatement.setString(updateIdPosition, (String) id);
    }
}
