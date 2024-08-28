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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracLcmOperationsInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.drac.DracVnfInstancesInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.ClusterManagementLicenseInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.EnmIntegrationLicenseAddDeleteNodeInterceptor;
import com.ericsson.vnfm.orchestrator.presentation.interceptors.license.LcmOperationsLicenseInterceptor;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final String BASE_PATH = "/vnflcm/v1";
    private static final String ADD_NODE_PATH = "/**/addNode";
    private static final String DELETE_NODE_PATH = "/**/deleteNode";
    private static final String CLUSTER_MANAGEMENT_PATH = BASE_PATH + "/clusterconfigs/**";
    private static final String INSTANCES_PATH = BASE_PATH + "/vnf_instances/**";
    private static final String OPERATIONS_PATH = BASE_PATH + "/vnf_lcm_op_occs/**";

    @Autowired
    private EnmIntegrationLicenseAddDeleteNodeInterceptor enmIntegrationLicenseAddDeleteNodeInterceptor;

    @Autowired
    private ClusterManagementLicenseInterceptor clusterManagementLicenseInterceptor;

    @Autowired
    private LcmOperationsLicenseInterceptor lcmOperationsLicenseInterceptor;

    @Autowired
    private DracVnfInstancesInterceptor dracVnfInstancesInterceptor;

    @Autowired
    private DracLcmOperationsInterceptor dracLcmOperationsInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(enmIntegrationLicenseAddDeleteNodeInterceptor)
                .order(0)
                .addPathPatterns(ADD_NODE_PATH,
                                 DELETE_NODE_PATH);

        registry.addInterceptor(clusterManagementLicenseInterceptor)
                .order(1)
                .addPathPatterns(CLUSTER_MANAGEMENT_PATH);

        registry.addInterceptor(lcmOperationsLicenseInterceptor)
                .order(2)
                .addPathPatterns(INSTANCES_PATH,
                                 OPERATIONS_PATH)
                .excludePathPatterns(ADD_NODE_PATH,
                                     DELETE_NODE_PATH);

        registry.addInterceptor(dracVnfInstancesInterceptor)
                .order(10)
                .addPathPatterns(INSTANCES_PATH);

        registry.addInterceptor(dracLcmOperationsInterceptor)
                .order(11)
                .addPathPatterns(OPERATIONS_PATH);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
