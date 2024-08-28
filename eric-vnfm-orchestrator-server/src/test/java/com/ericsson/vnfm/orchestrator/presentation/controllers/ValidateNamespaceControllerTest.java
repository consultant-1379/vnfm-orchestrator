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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.IdempotencyFilter;
import com.ericsson.vnfm.orchestrator.presentation.controllers.validatenamespace.ValidateNamespaceControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracLcmOperationsInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracVnfInstancesInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.ClusterManagementLicenseInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.EnmIntegrationLicenseAddDeleteNodeInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.LcmOperationsLicenseInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValidateNamespaceServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.drac.DracService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.HelmClientVersionValidator;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;

@WebMvcTest(controllers = {
    ValidateNamespaceControllerImpl.class,
    ValidateNamespaceServiceImpl.class,
    LifeCycleManagementHelper.class
},
    excludeAutoConfiguration = SecurityAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(
            value = { IdempotencyFilter.class },
    type = FilterType.ASSIGNABLE_TYPE)
)
@MockBean(classes = {
    VnfInstanceRepository.class,
    DatabaseInteractionService.class,
    ClusterConfigService.class,
    EnmIntegrationLicenseAddDeleteNodeInterceptor.class,
    ClusterManagementLicenseInterceptor.class,
    LcmOperationsLicenseInterceptor.class,
    DracVnfInstancesInterceptor.class,
    DracLcmOperationsInterceptor.class,
    DracService.class,
    PackageService.class,
    LicenseConsumerService.class,
    HelmClientVersionValidator.class,
    WorkflowRoutingService.class
})
public class ValidateNamespaceControllerTest {

    private static final String VALIDATE_NAMESPACE_URI = "/vnflcm/v1/validateNamespace";
    private static final String PATH_SEPERATOR = "/";

    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @Autowired
    private MockMvc mockMvc;

    private MvcResult mvcResult;

    @Test
    public void testValidateNamespace() throws Exception {
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(VALIDATE_NAMESPACE_URI + PATH_SEPERATOR + "valid-clustername" + PATH_SEPERATOR +
                                                                       "valid-namespace")).andExpect(status().is(HttpStatus.OK.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isBlank();
    }

    @Test
    public void testValidateNamespaceReservedNamespace() throws Exception {
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(VALIDATE_NAMESPACE_URI + PATH_SEPERATOR + "cluster-validate" + PATH_SEPERATOR +
                                                                       "kube-public")).andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();
        assertThat(response).contains("Cannot instantiate in any of the Kubernetes initialized namespaces : [default, kube-system, kube-public, kube-node-lease]");
    }

    @Test
    public void testValidateNamespaceCNFInSameNamespace() throws Exception {
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(VALIDATE_NAMESPACE_URI + PATH_SEPERATOR + "cluster-validate1" + PATH_SEPERATOR +
                                                                       "namespace-validate1")).andExpect(status().is(HttpStatus.OK.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isBlank();
    }

    @Test
    public void testValidateNamespaceCNFSameAsCRDNamespace() throws Exception {
        when(databaseInteractionService.getClusterConfigCrdNamespaceByClusterName(anyString())).thenReturn("multi-cluster-crd-ns");
        mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(VALIDATE_NAMESPACE_URI + PATH_SEPERATOR + "crdcluster" + PATH_SEPERATOR +
                "multi-cluster-crd-ns")).andExpect(status().is(HttpStatus.BAD_REQUEST.value())).andReturn();
        final String response = mvcResult.getResponse().getContentAsString();
        assertThat(response).isNotBlank();
        assertThat(response).contains("multi-cluster-crd-ns is reserved for CRDs. Cannot instantiate CNFs in CRD namespace");
    }
}
