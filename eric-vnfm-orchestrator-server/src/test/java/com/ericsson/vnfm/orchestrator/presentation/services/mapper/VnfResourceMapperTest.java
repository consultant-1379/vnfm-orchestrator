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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public class VnfResourceMapperTest {

    private final VnfResourceMapper vnfResourceMapper = new VnfResourceMapperImpl();

    @Test
    public void shouldConvertInstantiateOssTopologyToMap() {
        // given
        final var instance = new VnfInstance();
        instance.setInstantiateOssTopology(
                "{\"managedElementId\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"elementId\"}," +
                        "\"networkElementType\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"nodetype\"}," +
                        "\"networkElementVersion\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"nodeVersion\"}," +
                        "\"nodeIpAddress\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"my-ip\"}," +
                        "\"networkElementUsername\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"admin\"}," +
                        "\"networkElementPassword\":{\"type\":\"string\",\"required\":\"false\",\"default\":\"password\"}}");

        // when and then
        assertThat(vnfResourceMapper.toInternalModel(instance).getInstantiateOssTopology()).isEqualTo(Map.of(
                "managedElementId", Map.of("type", "string", "required", "false", "default", "elementId"),
                "networkElementType", Map.of("type", "string", "required", "false", "default", "nodetype"),
                "networkElementVersion", Map.of("type", "string", "required", "false", "default", "nodeVersion"),
                "nodeIpAddress", Map.of("type", "string", "required", "false", "default", "my-ip"),
                "networkElementUsername", Map.of("type", "string", "required", "false", "default", "admin"),
                "networkElementPassword", Map.of("type", "string", "required", "false", "default", "password")));
    }
}
