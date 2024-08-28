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

import static db.mapper.ConfigFilesRowMapper.CONFIG_FILE;
import static db.mapper.ConfigFilesRowMapper.DEFAULT_CONFIG;
import static db.migration.MigrationUtilities.CLUSTER_SERVER;
import static db.migration.MigrationUtilities.ID_COLUMN;
import static db.migration.MigrationUtilities.KUBE_CONFIG_STRING;
import static db.migration.MigrationUtilities.createDefaultKubeConfig;

import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import com.ericsson.am.shared.vnfd.service.CryptoService;

import db.mapper.ConfigFilesRowMapper;

@Component
@SuppressWarnings("squid:S00101")
public class V81__UpdateDefaultClusterConfigFile extends BaseJavaMigration {

    @Autowired
    private CryptoService cryptoService;

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        updateDefaultConfigFile(jdbcTemplate);
    }

    private static Map<String, String> getDefaultClusterConfigFile(final JdbcTemplate jdbcTemplate) {
        String defaultConfigQuery = "SELECT id, config_file, config_file_name FROM app_cluster_config_file WHERE "
                + "config_file_name = ?";
        return jdbcTemplate.queryForObject(defaultConfigQuery, new ConfigFilesRowMapper(), DEFAULT_CONFIG);
    }

    private void updateDefaultConfigFile(final JdbcTemplate jdbcTemplate) {
        Map<String, String> clusterConfig = getDefaultClusterConfigFile(jdbcTemplate);
        if (clusterConfig != null) {
            String id = clusterConfig.get(ID_COLUMN);
            Map<String, String> defaultKubeConfig = createDefaultKubeConfig();
            MapSqlParameterSource in = new MapSqlParameterSource();
            in.addValue(ID_COLUMN, id);
            in.addValue(CONFIG_FILE, cryptoService.encryptString(defaultKubeConfig.get(KUBE_CONFIG_STRING)));
            in.addValue(CLUSTER_SERVER, cryptoService.encryptString(defaultKubeConfig.get(CLUSTER_SERVER)));
            String sqlString = String.format("UPDATE app_cluster_config_file "
                                                     + "SET %1$s = :%1$s, %2$s = :%2$s where id = :%3$s",
                                             CONFIG_FILE, CLUSTER_SERVER, ID_COLUMN);
            NamedParameterJdbcTemplate jdbcTemplateObject = new
                    NamedParameterJdbcTemplate(jdbcTemplate);
            jdbcTemplateObject.update(sqlString, in);
        }
    }
}
