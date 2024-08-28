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

import static java.util.stream.Collectors.groupingBy;

import static org.zeroturnaround.zip.commons.FileUtilsV2_2.readFileToString;

import static com.ericsson.vnfm.orchestrator.utils.Utility.readFromInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MigrationUtilities {
    public static final String VNF_ID = "vnf_id";
    public static final String ID_COLUMN = "id";
    private static final String PRIORITY = "priority";
    public static final String KUBE_CONFIG_STRING = "kubeConfigString";
    public static final String CLUSTER_SERVER = "cluster_server";
    public static final String REPLICA_DETAILS = "replica_details";

    private MigrationUtilities() {
    }

    public static String generateUUIDUniqueToTheTable(final List<String> ids) {
        String uuid = UUID.randomUUID().toString();
        if (ids.contains(uuid)) {
            generateUUIDUniqueToTheTable(ids);
        }
        return uuid;
    }

    private static List<Map<String, Object>> getAllHelmCharts(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM helm_chart", new ChartUrlsRowMapper());
    }

    private static class ChartUrlsRowMapper implements RowMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, Object> row = new HashMap<>();
            row.put(ID_COLUMN, rs.getString(ID_COLUMN));
            row.put(PRIORITY, rs.getInt(PRIORITY));
            row.put(VNF_ID, rs.getString(VNF_ID));
            return row;
        }
    }

    private static void persistReleaseName(final Object vnfId, final List<Map<String, Object>> charts,
                                           final JdbcTemplate jdbcTemplate) {
        SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", vnfId);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        String vnfInstanceName = namedParameterJdbcTemplate
                .queryForObject("SELECT vnf_instance_name from app_vnf_instance where vnf_id = :id", namedParameters,
                        String.class);
        if (charts.size() == 1) {
            jdbcTemplate.update("UPDATE helm_chart set release_name = ? where id = ?", vnfInstanceName,
                    charts.get(0).get("id"));
        } else {
            for (Map<String, Object> chart : charts) {
                String releaseName = vnfInstanceName + "-" + chart.get(PRIORITY);
                jdbcTemplate
                        .update("UPDATE helm_chart set release_name = ? where id = ?", releaseName, chart.get("id"));
            }
        }
    }

    private static void persistRevisionNumber(final List<Map<String, Object>> charts,
                                              final JdbcTemplate jdbcTemplate) {
        for (Map<String, Object> chart : charts) {
            jdbcTemplate.update("UPDATE helm_chart set revision_number = 0 where id = ?", chart.get("id"));
        }
    }

    private static void persistRetryCount(final List<Map<String, Object>> charts,
                                          final JdbcTemplate jdbcTemplate) {
        for (Map<String, Object> chart : charts) {
            jdbcTemplate.update("UPDATE helm_chart set retry_count = 0 where id = ?", chart.get("id"));
        }
    }

    public static void migrateDataInHelmCharts(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllHelmCharts(jdbcTemplate).stream().collect(groupingBy(row -> row.get(MigrationUtilities.VNF_ID)))
                .forEach((vnfId, charts) -> persistReleaseName(vnfId, charts, jdbcTemplate));
    }

    public static void migrateDataInHelmChartsRevisionNumber(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllHelmCharts(jdbcTemplate).stream().collect(groupingBy(row -> row.get(MigrationUtilities.VNF_ID)))
                .forEach((vnfId, charts) -> persistRevisionNumber(charts, jdbcTemplate));
    }

    public static void migrateDataInHelmChartsRetryCount(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllHelmCharts(jdbcTemplate).stream().collect(groupingBy(row -> row.get(MigrationUtilities.VNF_ID)))
                .forEach((vnfId, charts) -> persistRetryCount(charts, jdbcTemplate));
    }

    public static Map<String, String> createDefaultKubeConfig() {
        String kubeConfigString = "";
        String caCertFilePath = "";
        String masterUrl = "";
        String token = "";

        try (KubernetesClient client = new DefaultKubernetesClient();
             InputStream kubeConfigStream =
                     Objects.requireNonNull(MigrationUtilities.class.getClassLoader().getResourceAsStream("templates/default.config"));
             ByteArrayOutputStream kubeConfigBytes = readFromInputStream(kubeConfigStream)) {
            kubeConfigString = kubeConfigBytes.toString(StandardCharsets.UTF_8);
            caCertFilePath = client.getConfiguration().getCaCertFile();

            String certificateContent = StringUtils.isNoneBlank(caCertFilePath)
                    ? readFileToString(new File(caCertFilePath))
                    : "";
            byte[] caCertData = Base64.getEncoder().encode(certificateContent.getBytes(StandardCharsets.UTF_8));
            masterUrl = client.getConfiguration().getMasterUrl();
            token = client.getConfiguration().getOauthToken();
            kubeConfigString = kubeConfigString.replace("<CA_CERT_FILE>", new String(caCertData, StandardCharsets.UTF_8))
                    .replace("<MASTER_URL>", masterUrl)
                    .replace("<TOKEN>", token);
        } catch (KubernetesClientException ex) {
            LOGGER.warn("Kubernetes client exception occurred", ex);
        } catch (IOException e) {
            LOGGER.warn("Can not read kubernetes config template duting migration", e);
        }
        if (masterUrl.isEmpty() || token.isEmpty()) {
            throw new InternalRuntimeException("Can't generate valid cluster config file");
        }
        Map<String, String> kubeConfigDetails = new HashMap<>();
        kubeConfigDetails.put(CLUSTER_SERVER, masterUrl);
        kubeConfigDetails.put(KUBE_CONFIG_STRING, kubeConfigString);
        return kubeConfigDetails;
    }
}
