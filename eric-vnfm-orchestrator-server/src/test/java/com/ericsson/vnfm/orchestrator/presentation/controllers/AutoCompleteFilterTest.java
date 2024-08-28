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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_NUMBER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.PAGE_SIZE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.vnfm.orchestrator.model.AutoCompleteResponse;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.AutoCompleteFilterImpl;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.IdempotencyFilter;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracChangePackageInfoVnfRequestBodyAdvice;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracChangeVnfPkgRequestBodyAdvice;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracCreateInstanceRequestBodyAdvice;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracLcmOperationsInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracVnfInstancesInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.ClusterManagementLicenseInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.EnmIntegrationLicenseAddDeleteNodeInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.LcmOperationsLicenseInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.services.AutoCompleteService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerServiceImpl;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceViewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AutoCompleteFilterImpl.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class,
            excludeFilters = @ComponentScan.Filter(
                    value = { IdempotencyFilter.class },
                    type = FilterType.ASSIGNABLE_TYPE))
@Import(AutoCompleteService.class)
@MockBean(classes = {
      LicenseConsumerServiceImpl.class,
      EnmIntegrationLicenseAddDeleteNodeInterceptor.class,
      ClusterManagementLicenseInterceptor.class,
      DracCreateInstanceRequestBodyAdvice.class,
      DracChangePackageInfoVnfRequestBodyAdvice.class,
      DracChangeVnfPkgRequestBodyAdvice.class,
      LcmOperationsLicenseInterceptor.class,
      DracVnfInstancesInterceptor.class,
      DracLcmOperationsInterceptor.class
})
public class AutoCompleteFilterTest {

    private static final String AUTO_COMPLETE_URL = "/vnflcm/api/v1/instance/filter/autocomplete";
    private static final String AUTO_COMPLETE_URL_WITH_TYPE = AUTO_COMPLETE_URL + "?type=";

    @MockBean
    VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    VnfInstanceViewRepository vnfInstanceViewRepository;

    @Autowired
    private MockMvc mockMvc;

    private MvcResult mvcResult;

    @Test
    public void testPageNumberNotANumber() throws Exception {
        mvcResult = mockMvc.perform(MockMvcRequestBuilders
                                          .get(AUTO_COMPLETE_URL)
                                          .param(PAGE_NUMBER, "test")).andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
        assertThat(mvcResult.getResolvedException())
              .isInstanceOf(InvalidInputException.class)
              .hasMessage(String.format("%s only supports number value", PAGE_NUMBER));
    }

    @Test
    public void testPageSizeNotANumber() throws Exception {
        mvcResult = mockMvc.perform(MockMvcRequestBuilders
                                          .get(AUTO_COMPLETE_URL)
                                          .param(PAGE_SIZE, "test")).andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
        assertThat(mvcResult.getResolvedException())
              .isInstanceOf(InvalidInputException.class)
              .hasMessage(String.format("%s only supports number value", PAGE_SIZE));
    }

    @Test
    public void getAllValues() throws Exception {
        //given
        List<String> mockedProductNameList = List.of("SomeProductName", "SomeOtherProductName");
        List<String> mockedSoftwareVersionList = List.of("SomeSoftwareVersion", "SomeOtherSoftwareVersion");
        List<String> mockedVnfdVersionList = List.of("SomeVersion", "SomeOtherVersion");
        List<String> mockedClusterNameList = List.of("SomeClusterName", "SomeOtherClusterName");
        List<String> mockedSoftwarePackageList = List.of("SomeSoftwarePackage", "SomeOtherSoftwarePackage");

        //when
        when(vnfInstanceRepository.findDistinctVnfProductName(any(), any())).thenReturn(mockedProductNameList);
        when(vnfInstanceRepository.findDistinctVnfSoftwareVersion(any(), any())).thenReturn(mockedSoftwareVersionList);
        when(vnfInstanceRepository.findDistinctVnfdVersion(any(), any())).thenReturn(mockedVnfdVersionList);
        when(vnfInstanceRepository.findDistinctClusterName(any(), any())).thenReturn(mockedClusterNameList);
        when(vnfInstanceViewRepository.findDistinctSoftwarePackage(any(), any())).thenReturn(mockedSoftwarePackageList);

        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AUTO_COMPLETE_URL))
              .andExpect(status().is(200)).andReturn();
        AutoCompleteResponse response = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(),
                                                                     AutoCompleteResponse.class);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isNotNull().isNotEmpty().isEqualTo(mockedProductNameList);
        assertThat(response.getSoftwareVersion()).isNotNull().isNotEmpty().isEqualTo(mockedSoftwareVersionList);
        assertThat(response.getPackageVersion()).isNotNull().isNotEmpty().isEqualTo(mockedVnfdVersionList);
        assertThat(response.getCluster()).isNotNull().isNotEmpty().isEqualTo(mockedClusterNameList);
        assertThat(response.getSourcePackage()).isNotNull().isNotEmpty().isEqualTo(mockedSoftwarePackageList);
    }

    @Test
    public void getAllValuesWithTypeParameter() throws Exception {
        //when
        when(vnfInstanceRepository.findDistinctVnfProductName(any(), any())).thenReturn(List.of(""));
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(AUTO_COMPLETE_URL_WITH_TYPE))
              .andExpect(status().is(200)).andReturn();
        AutoCompleteResponse response = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(),
                                                                     AutoCompleteResponse.class);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getPackageVersion()).isNotNull().isEmpty();
        assertThat(response.getCluster()).isNotNull().isEmpty();
        assertThat(response.getSourcePackage()).isNotNull().isEmpty();
        assertThat(response.getSoftwareVersion()).isNotNull().isEmpty();
        assertThat(response.getType()).isNotNull().isNotEmpty();
    }

    @Test
    public void getAllValuesWithTypeParameterValue() throws Exception {
        //when
        when(vnfInstanceRepository.findDistinctVnfProductName(any(), any())).thenReturn(List.of("SG"));
        mvcResult = mockMvc.perform(MockMvcRequestBuilders
                                          .get(AUTO_COMPLETE_URL_WITH_TYPE + "SG"))
              .andExpect(status().is(200)).andReturn();
        AutoCompleteResponse response = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(),
                                                                     AutoCompleteResponse.class);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getPackageVersion()).isNotNull().isEmpty();
        assertThat(response.getCluster()).isNotNull().isEmpty();
        assertThat(response.getSourcePackage()).isNotNull().isEmpty();
        assertThat(response.getSoftwareVersion()).isNotNull().isEmpty();
        List<String> allType = response.getType();
        assertThat(allType).isNotNull().isNotEmpty();
        for (String type : allType) {
            assertThat(type).contains("SG");
        }
    }
}
