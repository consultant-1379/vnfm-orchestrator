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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmParamsExtCp;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmParamsVdu;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.Multus;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduCp;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.NetAttDefResourceData;
import com.ericsson.vnfm.orchestrator.model.ResourceHandle;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.model.dto.VduCpDto;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class VduCpMapperImpl implements VduCpMapper {

    private static final String VIRTUAL_BINDING = "virtual_binding";

    @Autowired
    private VnfdHelper vnfdHelper;

    @Override
    public List<VduCpDto> mapToDto(final InstantiateVnfRequest instantiateVnfRequest, final JSONObject vnfd) {
        var currentNodeType = ReplicaDetailsUtility.getNodeType(vnfd.toString());
        var template = NodeTemplateUtility.createNodeTemplate(currentNodeType, vnfd);

        Map<String, Map<String, Integer>> vduCps = getVduCpsDataFromVnfd(template.getVduCps());
        List<String> vduCpIds = getVduCpIds(vduCps);

        List<NetAttDefResourceData> netAttDefResourceData = instantiateVnfRequest.getExtVirtualLinks().stream()
                .flatMap(e -> e.getExtNetAttDefResourceData().stream())
                .collect(Collectors.toList());

        List<VnfExtCpData> vnfExtCpData = getVnfExtCpDataFromRequest(instantiateVnfRequest.getExtVirtualLinks(), vduCpIds);

        Optional<Policies> policies = vnfdHelper.getVnfdScalingInformation(vnfd);
        Map<String, HelmParamsExtCp> helmParamsExtCpMap = new HashMap<>();
        Map<String, HelmParamsVdu> helmParamsVduMap = new HashMap<>();

        if (policies.isPresent()) {
            helmParamsExtCpMap = getHelmParamExtCp(policies.get(), vduCpIds);
            helmParamsVduMap = getHelmParamVdu(policies.get(), vduCps);
        }

        return collectDataFromRequest(vnfExtCpData, netAttDefResourceData, vduCps, helmParamsExtCpMap, helmParamsVduMap);
    }

    private List<VduCpDto> collectDataFromRequest(List<VnfExtCpData> vnfExtCpData, List<NetAttDefResourceData> netAttDefResourceData,
                                                  Map<String, Map<String, Integer>> vduCps, Map<String, HelmParamsExtCp> helmParamsExtCpMap,
                                                  Map<String, HelmParamsVdu> helmParamsVduMap) {
        List<VduCpDto> vduCpDtos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> vduParams: vduCps.entrySet()) {
            var vduCpDto = new VduCpDto();
            List<VduCpDto.VduCp> vduCpList = new ArrayList<>();

            setMultusParams(vduCpDto, helmParamsVduMap.get(vduParams.getKey()).getMultus());

            for (Map.Entry<String, Integer> vduCpParams: vduParams.getValue().entrySet()) {
                final Optional<VnfExtCpData> extCpData = vnfExtCpData.stream()
                        .filter(e -> e.getCpdId().equals(vduCpParams.getKey()))
                        .findFirst();

                if (extCpData.isEmpty()) {
                    continue;
                }

                var vduCp = new VduCpDto.VduCp();

                vduCp.setCpId(vduCpParams.getKey());
                vduCp.setOrder(vduCpParams.getValue());

                final List<String> interfaceNames = helmParamsExtCpMap.get(vduCpParams.getKey()).getInterfaceNames();

                List<NadParams> nadParams = new ArrayList<>();

                extCpData.get().getCpConfig().values().stream()
                        .flatMap(cpConfig -> cpConfig.getNetAttDefResourceId().stream())
                        .forEach(nadId -> {
                            final Optional<@NotNull @Valid ResourceHandle> resourceHandle = netAttDefResourceData.stream()
                                    .filter(nadData -> nadData.getNetAttDefResourceId().equals(nadId))
                                    .map(NetAttDefResourceData::getResourceHandle)
                                    .findFirst();
                            resourceHandle.ifPresent(handle -> nadParams.add(new NadParams(String.valueOf(handle.getResourceId()),
                                                                                               handle.getContainerNamespace())));
                        });
                vduCp.setInterfaceToNadParams(getInterfaceNadParams(interfaceNames, nadParams));

                vduCpList.add(vduCp);
            }

            if (!vduCpList.isEmpty()) {
                vduCpDto.setVduCps(vduCpList);
                vduCpDtos.add(vduCpDto);
            }
        }

        return vduCpDtos;
    }

    private List<Triple<String, String, String>> getInterfaceNadParams(List<String> interfaceNames, List<NadParams> nadParams) {
        List<Triple<String, String, String>> interfaceNadParams = new ArrayList<>();
        for (var i = 0; i < interfaceNames.size(); i++) {
            interfaceNadParams.add(Triple.of(interfaceNames.get(i), nadParams.get(i).getNadName(), nadParams.get(i).getContainerNamespace()));
        }
        return interfaceNadParams;
    }

    private Map<String, Map<String, Integer>> getVduCpsDataFromVnfd(List<VduCp> vduCps) {
        Map<String, Map<String, Integer>> vdus = new HashMap<>();
        for (VduCp vduCp: vduCps) {
            Map<String, Integer> vduCpParams = new HashMap<>();

            String vduName = vduCp.getRequirements().get(VIRTUAL_BINDING);
            vduCpParams.put(vduCp.getName(), vduCp.getOrder());

            if (vdus.containsKey(vduName)) {
                vdus.get(vduName).putAll(vduCpParams);
            } else {
                vdus.put(vduName, vduCpParams);
            }
        }

        return vdus;
    }

    private List<VnfExtCpData> getVnfExtCpDataFromRequest(List<ExtVirtualLinkData> extVirtualLinkData, List<String> vduCpIds) {
        return extVirtualLinkData.stream()
                .flatMap(e -> e.getExtCps().stream())
                .filter(e -> vduCpIds.contains(e.getCpdId()))
                .collect(toList());
    }

    private List<String> getVduCpIds(Map<String, Map<String, Integer>> vduCps) {
        return vduCps.values().stream()
                .flatMap(vduCp -> vduCp.keySet().stream())
                .collect(toList());
    }

    private Map<String, HelmParamsVdu> getHelmParamVdu(Policies policies, Map<String, Map<String, Integer>> vduCps) {
        return policies.getAllHelmParamsMappings().values().stream()
                .flatMap(e -> {
                    if (MapUtils.isNotEmpty(e.getVdus())) {
                        return e.getVdus().entrySet().stream();
                    } else {
                        return Stream.empty();
                    }
                })
                .filter(e -> vduCps.containsKey(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, HelmParamsExtCp> getHelmParamExtCp(Policies policies, List<String> vduCpIds) {
        return policies.getAllHelmParamsMappings().values().stream()
                .flatMap(e -> {
                    if (MapUtils.isNotEmpty(e.getExtCps())) {
                        return e.getExtCps().entrySet().stream();
                    } else {
                        return Stream.empty();
                    }
                })
                .filter(e -> vduCpIds.contains(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void setMultusParams(VduCpDto vduCpDto, Multus multus) {
        vduCpDto.setFormat(multus.getFormat());
        vduCpDto.setPath(multus.getPath());
        vduCpDto.setParam(multus.getParam());
        vduCpDto.setMultusInterface(multus.getMultusInterface());
        vduCpDto.setSet(multus.getSet());
    }

    @Getter
    @AllArgsConstructor
    private static class NadParams {
        private String nadName;
        private String containerNamespace;
    }
}
