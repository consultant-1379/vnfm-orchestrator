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

import java.util.List;

import org.json.JSONObject;

import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeType;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VirtualCp;
import com.ericsson.vnfm.orchestrator.model.CpProtocolData;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VirtualCpAddressData;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.model.dto.VirtualCpDto;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;

public class VirtualCpMapperImpl implements VirualCpMapper {

    @Override
    public List<VirtualCpDto> maptoDto(final InstantiateVnfRequest request, final JSONObject vnfd) {

        NodeType currentNodeType = ReplicaDetailsUtility.getNodeType(vnfd.toString());
        NodeTemplate template = NodeTemplateUtility.createNodeTemplate(currentNodeType, vnfd);

        List<String> virtualCpNames = template.getVirtualCps().stream()
                .map(VirtualCp::getName)
                .collect(toList());

        List<VnfExtCpData> vnfExtCpData = request.getExtVirtualLinks().stream()
                .flatMap(e -> e.getExtCps().stream())
                .filter(e -> virtualCpNames.contains(e.getCpdId()))
                .collect(toList());

        return vnfExtCpData.stream().map(this::extCpDataToVirtualCp).collect(toList());
    }

    private VirtualCpDto extCpDataToVirtualCp(VnfExtCpData vnfExtCpData) {
        VirtualCpDto virtualCpDto = new VirtualCpDto();
        virtualCpDto.setCpId(vnfExtCpData.getCpdId());

        List<String> addressPoolNames = vnfExtCpData.getCpConfig().values().stream()
                .flatMap(e -> e.getCpProtocolData().stream())
                .map(CpProtocolData::getVirtualCpAddress)
                .map(VirtualCpAddressData::getAddressPoolName)
                .collect(toList());

        virtualCpDto.setAddressPoolName(addressPoolNames);

        List<String> loadBalancerIps = vnfExtCpData.getCpConfig().values().stream()
                .flatMap(e -> e.getCpProtocolData().stream())
                .map(CpProtocolData::getVirtualCpAddress)
                .map(VirtualCpAddressData::getLoadBalancerIp)
                .collect(toList());

        virtualCpDto.setLoadBalancerIp(loadBalancerIps);

        return virtualCpDto;
    }
}
