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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetCismClusterConfigsByPageAndSizePositiveBase extends ContractTestRunner{

    @MockBean
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws IOException {
        Pageable pageable = PageRequest.of(1, 2);

        Page<ClusterConfigFile> page = new PageImpl<>(getCismConfigFilesByPage(), pageable, 9L);

        given(databaseInteractionService.getClusterConfigs(any())).willReturn(page);

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<ClusterConfigFile> getCismConfigFilesByPage() {
        List<ClusterConfigFile> configsList = new ArrayList<>();

        final ClusterConfigFile item1 = new ClusterConfigFile();
        item1.setName("hahn061.config");
        item1.setContent(
                "{\"kind\":\"Config\",\"apiVersion\":\"v1\",\"preferences\":{},\n\"clusters\":[{\"name\":\"hahn061\","
                        + "\"cluster\":{\"server\":\"https://gevalia.rnd.gic.ericsson.se/k8s/clusters/c-xz4bt\"}}],"
                        + "\"users\":[{\"name\":\"hahn061\",\"user\":{\"token\":\"kubeconfig-u\"}}],\"contexts\":[{\"name\":\"hahn061\","
                        + "\"context\":{\"cluster\":\"hahn061\",\"user\":\"hahn061\"}}],\"current-context\":\"hahn061\"}");
        item1.setDefault(true);
        configsList.add(item1);

        final ClusterConfigFile item2 = new ClusterConfigFile();
        item2.setName("cluster01.config");
        item2.setContent(
                "{\"kind\":\"Config\",\"apiVersion\":\"v2\",\"preferences\":{},\n\"clusters\":[{\"name\":\"cluster01\","
                        + "\"cluster\":{\"server\":\"https://gevalia.rnd.gic.ericsson.se/k8s/clusters/abc123\"}}],"
                        + "\"users\":[{\"name\":\"cluster01\",\"user\":{\"token\":\"kubeconfig-u\"}}],\"contexts\":[{\"name\":\"cluster01\","
                        + "\"context\":{\"cluster\":\"cluster01\",\"user\":\"cluster01\"}}],\"current-context\":\"cluster01\"}");
        item2.setDefault(false);
        configsList.add(item2);
        return configsList;
    }

    public static PaginationInfo fakePagination() {
        return new PaginationInfo().number(2)
                .size(2)
                .totalPages(5)
                .totalElements(9);
    }
}