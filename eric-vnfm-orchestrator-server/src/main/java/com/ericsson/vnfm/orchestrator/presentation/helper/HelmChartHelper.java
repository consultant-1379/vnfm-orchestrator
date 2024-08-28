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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmPackage;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Inputs;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VnfmLcmInterface;
import com.ericsson.am.shared.vnfd.utils.Constants;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class HelmChartHelper {

    @Autowired
    private HelmChartRepository helmChartRepository;

    public static Map<LCMOperationsEnum, List<HelmPackage>> getVnfLcmPrioritizedHelmPackageMap(PackageResponse packageResponse) {
        Map<LCMOperationsEnum, List<HelmPackage>> chartOperationsPriorityMap = new EnumMap<>(LCMOperationsEnum.class);
        JSONObject vnfd = new JSONObject(packageResponse.getDescriptorModel());
        List<VnfmLcmInterface> interfaces = VnfdUtility.getVnflcmInterfaces(vnfd).stream()
                .filter(lcmInterface -> !lcmInterface.getType().equals(VnfmLcmInterface.Type.HEAL))
                .collect(toList());

        interfaces.forEach(interfaceItem -> {
            List<HelmPackage> prioritizedInterfaceHelmPackages = getPrioritizedInterfaceHelmPackages(interfaceItem);
            if (!prioritizedInterfaceHelmPackages.isEmpty()) {
                LCMOperationsEnum operationType = getLCMOperationTypeFromInterface(interfaceItem);
                chartOperationsPriorityMap.put(operationType, prioritizedInterfaceHelmPackages);
            }
        });

        return chartOperationsPriorityMap;
    }

    private static LCMOperationsEnum getLCMOperationTypeFromInterface(final VnfmLcmInterface interfaceItem) {
        return LCMOperationsEnum.getList().stream()
                .filter(lcm -> lcm.getOperation().equals(interfaceItem.getType().getLabel()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Operation type %s not found", interfaceItem.getType().name())));
    }

    public void resetHelmChartStates(final List<HelmChart> helmCharts) {
        helmCharts.forEach(stateToNull());
    }

    public static List<HelmPackage> getCrdChartsFromLcmInterface(VnfmLcmInterface lcmInterface) {
        if (lcmInterface.getType().equals(VnfmLcmInterface.Type.TERMINATE)) {
            return Collections.emptyList();
        }

        return filterChartsFromLcmInterfaceByType(lcmInterface, Constants.CRD_PACKAGE_PREFIX);
    }

    public static List<HelmPackage> getHelmChartsFromLcmInterface(VnfmLcmInterface lcmInterface) {
        return filterChartsFromLcmInterfaceByType(lcmInterface, Constants.HELM_PACKAGE_PREFIX);
    }

    public void completeDisabledHelmCharts(List<HelmChart> helmCharts) {
        helmCharts.stream()
                .filter(helmChart -> !helmChart.isChartEnabled())
                .forEach(helmChart -> helmChart.setState(LifecycleOperationState.COMPLETED.toString()));
    }

    @NotNull
    private static List<HelmPackage> getPrioritizedInterfaceHelmPackages(VnfmLcmInterface vnfmLcmInterface) {
        List<HelmPackage> sortedHelmPackages = getSortedHelmPackages(vnfmLcmInterface);
        List<HelmPackage> prioritizedInterfaceHelmPackages = new ArrayList<>();

        int priorityIndex = 1;
        for (HelmPackage helmPackage : sortedHelmPackages) {
            HelmPackage prioritizedInterfaceHelmPackage = new HelmPackage(helmPackage.getId(), priorityIndex);
            if (helmPackage.getHelmValues() != null) {
                prioritizedInterfaceHelmPackage.setHelmValues(helmPackage.getHelmValues());
            }
            priorityIndex++;
            prioritizedInterfaceHelmPackages.add(prioritizedInterfaceHelmPackage);
        }
        return prioritizedInterfaceHelmPackages;
    }

    @NotNull
    private static List<HelmPackage> getSortedHelmPackages(VnfmLcmInterface interfaceItem) {
        List<HelmPackage> interfaceHelmPackages = new ArrayList<>();
        interfaceHelmPackages.addAll(getCrdChartsFromLcmInterface(interfaceItem));
        interfaceHelmPackages.addAll(getHelmChartsFromLcmInterface(interfaceItem));
        return interfaceHelmPackages;
    }

    private static List<HelmPackage> filterChartsFromLcmInterfaceByType(VnfmLcmInterface lcmInterface, String type) {
        Inputs inputs = lcmInterface.getInputs();

        if (inputs == null || inputs.getHelmPackages() == null) {
            return Collections.emptyList();
        }

        return inputs.getHelmPackages().stream()
                .filter(helmPackage -> helmPackage.getId().startsWith(type))
                .collect(toList());
    }

    private Consumer<HelmChart> stateToNull() {
        return chart -> {
            chart.setState(null);
            chart.setDeletePvcState(null);
            helmChartRepository.save(chart);
        };
    }
}
