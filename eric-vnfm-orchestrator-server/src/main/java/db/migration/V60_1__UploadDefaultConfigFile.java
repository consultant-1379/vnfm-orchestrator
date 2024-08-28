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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.UUID;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.lob.DefaultLobHandler;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.ClusterConfigFileNotValidException;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("squid:S00101")
public class V60_1__UploadDefaultConfigFile extends BaseJavaMigration {

    @Override
    public void migrate(final Context context) throws IOException {
        String kubeConfigString = "";
        String caCertFile = "";
        String masterUrl = "";
        String token = "";

        try (KubernetesClient client = new DefaultKubernetesClient();
             InputStream kubeConfigInputStream = getClass().getClassLoader().getResourceAsStream("templates/default.config")) {
            kubeConfigString = kubeConfigInputStream.toString();
            caCertFile = client.getConfiguration().getCaCertFile();
            if (caCertFile == null) {
                caCertFile = "";
            }
            masterUrl = client.getConfiguration().getMasterUrl();
            token = client.getConfiguration().getOauthToken();
            kubeConfigString = kubeConfigString.replace("<CA_CERT_FILE>", caCertFile)
                    .replace("<MASTER_URL>", masterUrl)
                    .replace("<TOKEN>", token);
        } catch (KubernetesClientException ex) {
            LOGGER.warn("Kubernetes client exception occurred", ex);
        }
        if (masterUrl.isEmpty() || token.isEmpty()) {
            throw new ClusterConfigFileNotValidException("Can't generate valid cluster config file");
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        byte[] kubeConfigBytes = kubeConfigString.getBytes(StandardCharsets.UTF_8);
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", UUID.randomUUID().toString());
        SqlLobValue sqlLobValue = new SqlLobValue(new ByteArrayInputStream(kubeConfigBytes),
                kubeConfigBytes.length, new DefaultLobHandler());
        in.addValue("config", sqlLobValue, Types.BLOB);
        in.addValue("cluster_server", masterUrl);
        in.addValue("user_token", token);
        String sqlString = "INSERT INTO app_cluster_config_file " +
                "(id, config_file_name, config_file_status, config_file_blob, config_file_description, cluster_server, user_token) " +
                "VALUES  (:id, 'default.config', 'NOT_IN_USE', :config, 'Default cluster config file', :cluster_server, :user_token);";
        NamedParameterJdbcTemplate jdbcTemplateObject = new
                NamedParameterJdbcTemplate(jdbcTemplate);
        jdbcTemplateObject.update(sqlString, in);
        sqlLobValue.cleanup();
    }
}

