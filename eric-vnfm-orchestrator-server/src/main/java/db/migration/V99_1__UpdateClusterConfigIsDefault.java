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

import static java.util.stream.Collectors.toList;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("squid:S00101")
public class V99_1__UpdateClusterConfigIsDefault extends BaseJavaMigration {

    private static final String ID = "id";
    private static final String CONFIG_FILE_NAME = "config_file_name";
    private static final String CONFIG_FILE_STATUS = "config_file_status";
    private static final String IS_DEFAULT = "is_default";

    private static final String DEFAULT_CONFIG_NAME = "default.config";

    private static final String IS_DEFAULT_PARAM = "isDefault";
    private static final String ID_PARAM = "id";

    private static Random rand = new SecureRandom();

    @Override
    public void migrate(final Context context) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        final List<ClusterConfigFile> allClusterConfigs = getAllClusterConfigs(jdbcTemplate);

        chooseAndMarkDefaultConfig(allClusterConfigs, jdbcTemplate);
    }

    public static List<ClusterConfigFile> getAllClusterConfigs(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_cluster_config_file", new ClusterConfigRowMapper());
    }

    private static void chooseAndMarkDefaultConfig(final List<ClusterConfigFile> allClusterConfigs,
                                                   final JdbcTemplate jdbcTemplate) {

        if (configMarkedDefaultExist(allClusterConfigs)) {
            return;
        }

        final Optional<ClusterConfigFile> configNamedDefaultOptional = getConfigNamedDefault(allClusterConfigs);
        if (configNamedDefaultOptional.isEmpty()) {
            LOGGER.warn("Neither cluster config marked '{}' nor cluster config named '{}' has been found", IS_DEFAULT, DEFAULT_CONFIG_NAME);
            return;
        }

        final ClusterConfigFile configNamedDefault = configNamedDefaultOptional.get();
        final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        if (configNamedDefault.status == ConfigFileStatus.IN_USE) {
            markConfigDefault(configNamedDefault, namedParameterJdbcTemplate);
            return;
        }

        deleteConfig(configNamedDefault, namedParameterJdbcTemplate);
        if (allClusterConfigs.size() == 1) {
            return;
        }

        final ClusterConfigFile newDefaultConfig = chooseRandomly(removeElement(allClusterConfigs, configNamedDefault));
        markConfigDefault(newDefaultConfig, namedParameterJdbcTemplate);
    }

    private static boolean configMarkedDefaultExist(final List<ClusterConfigFile> allClusterConfigs) {
        return emptyIfNull(allClusterConfigs).stream()
                .anyMatch(config -> config.isDefault);
    }

    private static Optional<ClusterConfigFile> getConfigNamedDefault(final List<ClusterConfigFile> allClusterConfigs) {
        return emptyIfNull(allClusterConfigs).stream()
                .filter(config -> Objects.equals(config.name, DEFAULT_CONFIG_NAME))
                .findFirst();
    }

    private static void deleteConfig(final ClusterConfigFile config, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        final SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue(ID_PARAM, config.id);
        namedParameterJdbcTemplate.update("DELETE FROM app_cluster_config_file WHERE id = :id", namedParameters);
    }

    private static void markConfigDefault(final ClusterConfigFile config, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        final SqlParameterSource namedParameters = new MapSqlParameterSource()
                .addValue(ID_PARAM, config.id)
                .addValue(IS_DEFAULT_PARAM, true);
        namedParameterJdbcTemplate.update("UPDATE app_cluster_config_file SET is_default = :isDefault WHERE id = :id", namedParameters);
    }

    private static <T> List<T> removeElement(final List<T> list, final T toRemove) {
        return list.stream()
                .filter(element -> element != toRemove)
                .collect(toList());
    }

    private static ClusterConfigFile chooseRandomly(final List<ClusterConfigFile> allClusterConfigs) {
        return allClusterConfigs.get(rand.nextInt(allClusterConfigs.size()));
    }

    private static class ClusterConfigRowMapper implements RowMapper<ClusterConfigFile> {
        @Override
        public ClusterConfigFile mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new ClusterConfigFile(rs.getString(ID),
                                         rs.getString(CONFIG_FILE_NAME),
                                         ConfigFileStatus.valueOf(rs.getString(CONFIG_FILE_STATUS)),
                                         rs.getBoolean(IS_DEFAULT));
        }
    }

    private static final class ClusterConfigFile {

        private final String id;
        private final String name;
        private final ConfigFileStatus status;
        private final boolean isDefault;

        private ClusterConfigFile(final String id, final String name, final ConfigFileStatus status, final boolean isDefault) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.isDefault = isDefault;
        }
    }
}
