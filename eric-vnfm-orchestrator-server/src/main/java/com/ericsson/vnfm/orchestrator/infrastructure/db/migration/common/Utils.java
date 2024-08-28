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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    public static String createSelectQuery(final String tableName, final List<String> columns) {
        String columnsStringList = String.join(", ", columns);
        return String.format("SELECT %s FROM %s", columnsStringList, tableName);
    }

    private static String createUpdateQuery(List<String> columns, String tableName, String id) {
        String setVariables = columns.stream()
                .filter(setParam -> !setParam.equals(id))
                .map(setParam -> String.format("%1$s = :%1$s", setParam))
                .collect(Collectors.joining(", "));
        return String.format("UPDATE %1$s SET %2$s WHERE %3$s = :%3$s",
                             tableName, setVariables, id);
    }

    public static void updateRows(final JdbcTemplate jdbcTemplate,
                                  final String tableName,
                                  final String id,
                                  final List<String> columns,
                                  final Map<String, String> parameters) {
        NamedParameterJdbcTemplate jdbcTemplateObject = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValues(parameters);
        String sqlString = createUpdateQuery(columns, tableName, id);
        jdbcTemplateObject.update(sqlString, parameters);
    }
}
