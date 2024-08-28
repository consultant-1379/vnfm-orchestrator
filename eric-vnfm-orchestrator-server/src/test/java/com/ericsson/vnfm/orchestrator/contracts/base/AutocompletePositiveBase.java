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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.AutoCompleteFilterImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.AutoCompleteService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceViewRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class AutocompletePositiveBase extends ContractTestRunner{

    @Autowired
    AutoCompleteService autoCompleteService;

    @Autowired
    AutoCompleteFilterImpl autoCompleteFilter;

    @MockBean
    VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    VnfInstanceViewRepository vnfInstanceViewRepository;

    @BeforeEach
    public void initMocks() {
        mockAllType("");
        mockAllPackageVersion("");
        mockAllClusterName("");
        mockAllSoftwareVersion("");
        mockAllPackageSourcePackage("");
        mockAllType("S");
        mockAllPackageVersion(null);
        mockAllClusterName(null);
        mockAllSoftwareVersion(null);
        mockAllPackageSourcePackage(null);
        RestAssuredMockMvc.standaloneSetup(autoCompleteFilter);
    }

    private void mockAllType(String value) {
        List<String> allType = new ArrayList<>();
        allType.add("SGSN-MME");
        allType.add("SAPC");
        allType.add("SASN");
        if (value == null) {
            given(vnfInstanceRepository.findDistinctVnfProductName(eq(null), any()))
                .willReturn(new ArrayList<>());
        } else {
            given(vnfInstanceRepository.findDistinctVnfProductName(eq(value), any()))
                .willReturn(allType);
        }

    }

    private void mockAllSoftwareVersion(String value) {
        List<String> allSoftwareVersion = new ArrayList<>();
        allSoftwareVersion.add("1.20 (CXS101289_R81E08)");
        if (value == null) {
            given(vnfInstanceRepository.findDistinctVnfSoftwareVersion(eq(null), any()))
                .willReturn(new ArrayList<>());
        } else {
            given(vnfInstanceRepository.findDistinctVnfSoftwareVersion(eq(value), any()))
                    .willReturn(allSoftwareVersion);
        }
    }

    private void mockAllClusterName(String value) {
        List<String> clusterName = new ArrayList<>();
        clusterName.add("cluster1");
        clusterName.add("cluster2");
        clusterName.add("cluster3");
        clusterName.add("cluster4");
        if (value == null) {
            given(vnfInstanceRepository.findDistinctClusterName(eq(null), any()))
                .willReturn(new ArrayList<>());
        } else {
            given(vnfInstanceRepository.findDistinctClusterName(eq(value), any()))
                .willReturn(clusterName);
        }
    }

    private void mockAllPackageVersion(String value) {
        List<String> allProvider = new ArrayList<>();
        allProvider.add("cxp9025898_4r81e08");
        allProvider.add("cxp9025898_4r81e09");
        allProvider.add("cxp9025898_4r81e10");
        allProvider.add("cxp9025898_4r81e11");
        if (value == null) {
            given(vnfInstanceRepository.findDistinctVnfdVersion(eq(null), any()))
                .willReturn(new ArrayList<>());
        } else {
            given(vnfInstanceRepository.findDistinctVnfdVersion(eq(value), any()))
                    .willReturn(allProvider);
        }
    }

    private void mockAllPackageSourcePackage(String value) {
        List<String> sourcePackage = new ArrayList<>();
        sourcePackage.add("Ericsson.SAPC.1.20 (CXS101289_R81E08)");
        sourcePackage.add("Ericsson.EPG.1.21 (CXS101289_R81E09)");
        sourcePackage.add("Ericsson.SASN.1.22 (CXS101289_R81E10)");
        sourcePackage.add("Ericsson.SGSN-MME.1.23 (CXS101289_R81E11)");
        if (value == null) {
            given(vnfInstanceViewRepository.findDistinctSoftwarePackage(eq(null), any()))
                .willReturn(new ArrayList<>());
        } else {
            given(vnfInstanceViewRepository.findDistinctSoftwarePackage(eq(value), any()))
                .willReturn(sourcePackage);
        }
    }
}
