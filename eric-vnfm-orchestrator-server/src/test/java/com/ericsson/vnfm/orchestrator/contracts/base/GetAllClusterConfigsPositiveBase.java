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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.filters.ClusterConfigQuery;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllClusterConfigsPositiveBase extends ContractTestRunner {

    private static final String FILTER_PARAM = "(eq,status,IN_USE)";
    private static final int TOTAL_ITEMS_NUMBER = 24;
    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ClusterConfigQuery clusterConfigQuery;
    @MockBean
    private ClusterConfigFileRepository clusterConfigFileRepository;


    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

        given(clusterConfigFileRepository.findAll(
                argThat((Pageable pageable) -> pageable != null && pageable.getPageNumber() == 0)
        )).willAnswer(invocationOnMock -> new PageImpl<>(populateClusterConfigs(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        given(clusterConfigFileRepository.findAll(
                argThat((Pageable pageable) -> pageable != null && pageable.getPageNumber() == 1)
        )).willAnswer(invocationOnMock -> new PageImpl<>(populateClusterConfigsPage2(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        // Mock behavior when sort object initialized in Pageable
        given(clusterConfigFileRepository.findAll(
                argThat((Pageable pageable) -> {
                    Sort sort = pageable.getSort();
                    List<Sort.Order> orders = sort.get().collect(Collectors.toList());
                    Sort.Order order = orders.get(0);
                    Sort.Direction direction = order.getDirection();
                    String property = order.getProperty();

                    return pageable != null &&
                           orders.size() == 1 &&
                           direction.equals(Sort.Direction.DESC) &&
                           property.equals("name");
                }))
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateClusterConfigsDescByNameOrder(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        given(clusterConfigQuery.getPageWithFilter(
                eq(FILTER_PARAM),
                argThat((Pageable pageable) -> pageable != null && pageable.getPageNumber() == 0)
        )).willAnswer(invocationOnMock -> new PageImpl<>(populateClusterConfigsInUseStatus(), invocationOnMock.getArgument(1), TOTAL_ITEMS_NUMBER));


    }

    private ClusterConfigFile createClusterConfig(String id, String name, ConfigFileStatus status, String description, String ns, boolean isDefault) {
        final ClusterConfigFile clusterConfigFile = new ClusterConfigFile();
        clusterConfigFile.setId(id);
        clusterConfigFile.setName(name);
        clusterConfigFile.setStatus(status);
        clusterConfigFile.setDescription(description);
        clusterConfigFile.setCrdNamespace(ns);
        clusterConfigFile.setDefault(isDefault);

        return clusterConfigFile;
    }

    private List<ClusterConfigFile> populateClusterConfigs() {
        List<ClusterConfigFile> configs = new ArrayList<>();
        configs.add(createClusterConfig("default", "default.config", ConfigFileStatus.IN_USE, "Default cluster config file", "eric-crd-ns", true));
        configs.add(createClusterConfig("503",
                                        "cluster503ForDeregister.config",
                                        ConfigFileStatus.NOT_IN_USE,
                                        "Description for config file 503",
                                        "eric-crd-ns", false));
        for (int i = 1; i < 14; i++) {
            String clusterName = "cluster" + i + ".config";
            String description = "Description for config file " + i;
            String namespace = "namespace-" + i;
            configs.add(createClusterConfig(String.valueOf(i), clusterName, ConfigFileStatus.NOT_IN_USE, description, namespace, false));
        }
        return configs;
    }
    private List<ClusterConfigFile> populateClusterConfigsPage2() {
        List<ClusterConfigFile> configs = new ArrayList<>();
        for (int i = 15; i < 23; i++) {
            String clusterName = "cluster" + i + ".config";
            String description = "Description for config file " + i;
            String namespace = "namespace-" + i;
            configs.add(createClusterConfig(String.valueOf(i), clusterName, ConfigFileStatus.NOT_IN_USE, description, namespace, false));
        }
        configs.add(createClusterConfig("cbb3a411-d98b-4a41-876a-b0642fe923cf",
                "cluster002.config",
                ConfigFileStatus.NOT_IN_USE,
                "Cluster 002 config file",
                "eric-crd-ns", false));
        return configs;
    }

    private List<ClusterConfigFile> populateClusterConfigsDescByNameOrder() {
        List<ClusterConfigFile> configs = new ArrayList<>();

        char[] alphabet = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o'};

        for (int i = 15; i > 0; i--) {
            String clusterName = alphabet[i-1] + " cluster.config";
            String description = "Description for config file " + alphabet[i-1];
            String namespace = "eric-crd-ns";
            configs.add(createClusterConfig(String.valueOf(i), clusterName, ConfigFileStatus.IN_USE, description, namespace, false));
        }
        return configs;
    }

    private List<ClusterConfigFile> populateClusterConfigsInUseStatus() {
        List<ClusterConfigFile> configs = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            String clusterName = "cluster" + i + ".config";
            String description = "Description for config file " + i;
            String namespace = "eric-crd-ns";
            configs.add(createClusterConfig(String.valueOf(i), clusterName, ConfigFileStatus.IN_USE, description, namespace, false));
        }
        return configs;
    }





}
