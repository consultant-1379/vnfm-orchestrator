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

import java.util.List;
import java.util.regex.Pattern;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;

@Component
@SuppressWarnings("squid:S00101")
public class V98__UpdateHelmChartAndHelmChartHistoryRecord extends BaseJavaMigration {

    private static final String SELECT_HELM_CHART_URL_LIMIT_1 = "SELECT helm_chart_url FROM helm_chart LIMIT 1;";
    private static final String UPDATE_URL_FOR_HELM_CHART = "UPDATE helm_chart SET helm_chart_url = REPLACE(helm_chart_url, ?, ?);";
    private static final String UPDATE_URL_FOR_HELM_CHART_HISTORY_RECORD = "UPDATE helm_chart_history "
            + "SET helm_chart_url = REPLACE(helm_chart_url, ?, ?);";
    private static final Pattern PATTERN = Pattern.compile("^(https?://)?([^:/]+)?(:\\d+)?");

    @Autowired
    private NfvoConfig nfvoConfig;

    private String helmRegistry;

    @Value("${helm.registry.host}")
    public void setHelmRegistry(final String helmRegistry) {
        this.helmRegistry = helmRegistry;
    }

    @Override
    public void migrate(final Context context) {
        if (!nfvoConfig.isEnabled() && !helmRegistry.isEmpty()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
            final List<String> helmChartUrls = jdbcTemplate.queryForList(SELECT_HELM_CHART_URL_LIMIT_1, String.class);
            final String chartHost = getChartHost(helmChartUrls);
            jdbcTemplate.update(UPDATE_URL_FOR_HELM_CHART, chartHost, helmRegistry);
            jdbcTemplate.update(UPDATE_URL_FOR_HELM_CHART_HISTORY_RECORD, chartHost, helmRegistry);
        }
    }

    private static String getChartHost(final List<String> helmChartUrls) {
        String domain = "";
        if (!helmChartUrls.isEmpty()) {
            domain = PATTERN.matcher(helmChartUrls.get(0))
                    .results()
                    .map(mr -> mr.group(0))
                    .findFirst()
                    .orElse("");
        }
        return domain;
    }
}
