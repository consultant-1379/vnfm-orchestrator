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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.ENABLED_MODULE;
import static com.ericsson.vnfm.orchestrator.utils.DeployableModulesUtils.getDeployableModulesFromArtifact;
import static com.ericsson.vnfm.orchestrator.utils.DeployableModulesUtils.isChartHasStateInAnyDeployableModules;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.areAllCnfChartsDisabled;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.nestedvnfd.DeployableModule;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeployableModulesServiceImpl implements DeployableModulesService {
    @Autowired
    private PackageService packageService;

    @Override
    public void updateVnfInstanceHelmChartsAccordingToDeployableModulesExtension(final VnfInstance vnfInstance,
                                                                                 final Map<String, String> deployableModulesValues) {
        if (MapUtils.isEmpty(deployableModulesValues)) {
            LOGGER.debug("Skipping updating helm charts with extensions as instance extensions are not present");
            return;
        }
        final Map<String, DeployableModule> deployableModulesFromNodeTemplate = fetchDeployableModulesFromVnfd(vnfInstance);

        final List<HelmChart> helmCharts = vnfInstance.getHelmCharts();
        for (HelmChart helmChart : helmCharts) {
            final String helmChartArtifact = helmChart.getHelmChartArtifactKey();
            final List<String> associatedDeployableModules = getDeployableModulesFromArtifact(deployableModulesFromNodeTemplate, helmChartArtifact);
            if (CollectionUtils.isEmpty(associatedDeployableModules)) {
                helmChart.setChartEnabled(true);
            } else {
                final boolean isChartEnabled = isChartHasStateInAnyDeployableModules(associatedDeployableModules, deployableModulesValues,
                                                                                     ENABLED_MODULE);
                helmChart.setChartEnabled(isChartEnabled);
            }
        }
        if (areAllCnfChartsDisabled(vnfInstance)) {
            throw new IllegalArgumentException("No enabled Helm Charts present");
        }
    }

    private Map<String, DeployableModule> fetchDeployableModulesFromVnfd(final VnfInstance vnfInstance) {
        final PackageResponse packageInfo = packageService.getPackageInfoWithDescriptorModel(vnfInstance.getVnfDescriptorId());
        final NodeTemplate nodeTemplate = ReplicaDetailsUtility.getNodeTemplate(packageInfo.getDescriptorModel());
        return nodeTemplate.getDeploymentModules();
    }
}
