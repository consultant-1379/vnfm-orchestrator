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
package com.ericsson.vnfm.orchestrator.filters.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KubernetesDuplicateLogFilter extends Filter<ILoggingEvent> {
    private static final Integer DEFAULT_CONFIG_MAP_MAX_REPEATS = 1;
    private static final Integer DEFAULT_SECRET_MAX_REPEATS = 1;
    private static final String DEFAULT_CONFIG_MAP_LOG_TEMPLATE = "config-map with name : '";
    private static final String DEFAULT_SECRET_LOG_TEMPLATE = "secret with name : ";
    private Map<String, Integer> configMaps;
    private Map<String, Integer> secrets;

    private Integer configMapRepeats;
    private Integer secretRepeats;


    @Override
    public void start() {
        configMapRepeats = configMapRepeats != null ? configMapRepeats : DEFAULT_CONFIG_MAP_MAX_REPEATS;
        secretRepeats = secretRepeats != null ? secretRepeats : DEFAULT_SECRET_MAX_REPEATS;
        configMaps = new ConcurrentHashMap<>();
        secrets = new ConcurrentHashMap<>();
        super.start();
    }

    @Override
    public void stop() {
        configMaps.clear();
        configMaps = null;
        secrets.clear();
        secrets = null;
        super.stop();
    }

    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        String message = iLoggingEvent.getMessage();
        if (message.startsWith(DEFAULT_CONFIG_MAP_LOG_TEMPLATE)) {
            return decide(configMaps, extractConfigMapNameFromMessage(message), configMapRepeats);
        } else if (message.startsWith(DEFAULT_SECRET_LOG_TEMPLATE)) {
            return decide(secrets, extractSecretNameFromMessage(message), secretRepeats);
        } else {
            return FilterReply.NEUTRAL;
        }
    }

    private static FilterReply decide(Map<String, Integer> cachedValues, String value, Integer maxRepeats) {
        Integer repeats = cachedValues.get(value);
        if (repeats == null) {
            cachedValues.put(value, 1);
            return FilterReply.ACCEPT;
        } else {
            if (repeats < maxRepeats) {
                cachedValues.put(value, repeats + 1);
                return FilterReply.ACCEPT;
            }
            return FilterReply.DENY;
        }
    }

    private static String extractConfigMapNameFromMessage(String message) {
        int startFrom = DEFAULT_CONFIG_MAP_LOG_TEMPLATE.length();
        return message.substring(startFrom, message.indexOf("'", startFrom));
    }

    private static String extractSecretNameFromMessage(String message) {
        int startFrom = DEFAULT_SECRET_LOG_TEMPLATE.length();
        return message.substring(startFrom, message.indexOf(" ", startFrom));
    }

    public Integer getConfigMapRepeats() {
        return configMapRepeats;
    }

    public void setConfigMapRepeats(int configMapRepeats) {
        this.configMapRepeats = configMapRepeats;
    }

    public Integer getSecretRepeats() {
        return secretRepeats;
    }

    public void setSecretRepeats(Integer secretRepeats) {
        this.secretRepeats = secretRepeats;
    }
}
