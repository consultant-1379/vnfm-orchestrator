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
package com.ericsson.vnfm.orchestrator.presentation.services.drac;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotAuthorizedException;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.JwtDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = ObjectMapper.class)
public class DracServiceTest {

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private JwtDecoder JwtDecoder;

    @Test
    public void creationShouldTolerateNullConfig() {
        assertThatNoException().isThrownBy(() -> new DracService(false, null, JwtDecoder, mapper));
    }

    @Test
    public void creationShouldTolerateBlankConfig() {
        assertThatNoException().isThrownBy(() -> new DracService(false, " ", JwtDecoder, mapper));
    }

    @Test
    public void creationShouldTolerateInvalidJsonConfig() {
        assertThatNoException().isThrownBy(() -> new DracService(false, "}}}", JwtDecoder, mapper));
    }

    @Test
    public void creationShouldTolerateConfigWithMissingFields() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-with-missing-fields.json");
        assertThatNoException().isThrownBy(() -> new DracService(false, configJson, JwtDecoder, mapper));
    }

    @Test
    public void checkPermissionForNodeTypeShouldIgnoreEntriesWithMissingRoleName() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-with-missing-fields.json");
        final var dracConfig = new DracService(false, configJson, JwtDecoder, mapper);

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-1", "role-2"));

        assertThatThrownBy(() -> dracConfig.checkPermissionForNodeType("type-2-1"))
                .isInstanceOf(NotAuthorizedException.class);
        assertThatThrownBy(() -> dracConfig.checkPermissionForNodeType("type-2-2"))
                .isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    public void checkPermissionForNodeTypeShouldTolerateEntriesWithNullIndividualNodeType() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-with-missing-individual-node-type.json");
        final var dracConfig = new DracService(false, configJson, JwtDecoder, mapper);

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-3"));

        assertThatNoException().isThrownBy(() -> dracConfig.checkPermissionForNodeType("type-3-2"));
    }

    @Test
    public void checkPermissionForNodeTypeShouldReturnEmptyForUnknownNodeType() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-valid.json");
        final var dracConfig = new DracService(false, configJson, JwtDecoder, mapper);

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-4"));

        assertThatThrownBy(() -> dracConfig.checkPermissionForNodeType("unknown type"))
                .isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    public void checkPermissionForNodeTypeShouldSupportSingleRoleByNodeType() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-valid.json");
        final var dracConfig = new DracService(false, configJson, JwtDecoder, mapper);

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-4"));

        assertThatNoException().isThrownBy(() -> dracConfig.checkPermissionForNodeType("type-4-1"));
        assertThatNoException().isThrownBy(() -> dracConfig.checkPermissionForNodeType("type-4-2"));
    }

    @Test
    public void checkPermissionForNodeTypeShouldSupportMultipleRolesByNodeType() throws IOException, URISyntaxException {
        final var configJson = readDataFromFile("drac-config/drac-config-with-shared-node-type.json");
        final var dracConfig = new DracService(false, configJson, JwtDecoder, mapper);

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-containing-shared-type-1"));
        assertThatNoException().isThrownBy(() -> dracConfig.checkPermissionForNodeType("shared-type"));

        when(JwtDecoder.extractUserRoles(any())).thenReturn(List.of("role-containing-shared-type-2"));
        assertThatNoException().isThrownBy(() -> dracConfig.checkPermissionForNodeType("shared-type"));
    }
}
