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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

public class ConfigFilesRowMapper implements RowMapper<Map<String, String>> {

    public static final String CONFIG_FILE_NAME = "config_file_name";
    public static final String CONFIG_FILE = "config_file";
    public static final String DEFAULT_CONFIG = "default.config";

    @Override
    public Map<String, String> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        Map<String, String> row = new HashMap<>();
        row.put(ID_COLUMN, rs.getString(ID_COLUMN));
        row.put(CONFIG_FILE, rs.getString(CONFIG_FILE));
        row.put(CONFIG_FILE_NAME, rs.getString(CONFIG_FILE_NAME));
        return row;
    }
}
