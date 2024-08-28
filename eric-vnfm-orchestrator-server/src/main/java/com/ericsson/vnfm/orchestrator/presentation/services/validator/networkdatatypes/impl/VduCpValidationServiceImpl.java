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
package com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import static com.ericsson.vnfm.orchestrator.model.CpProtocolData.LayerProtocolEnum.OVER_ETHERNET;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_MISSED_PROTOCOL_DATA_FIELD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.VDU_CP_NAD_NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduCp;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.NetAttDefResourceData;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpConfig;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.ExtCpValidationService;

@Service
public class VduCpValidationServiceImpl implements ExtCpValidationService<VduCp> {

    @Override
    public void validate(final ExtVirtualLinkData extVirtualLinks, final VnfExtCpData extCp) {
        validateVduCpNadExistence(extVirtualLinks, extCp);
        validateLayerProtocol(extCp);
    }

    private void validateLayerProtocol(final VnfExtCpData extCp) {

        extCp.getCpConfig().values().stream()
                .filter(vnfExtCpConfig -> !CollectionUtils.isEmpty(vnfExtCpConfig.getCpProtocolData()))
                .flatMap(vnfExtCpConfig -> vnfExtCpConfig.getCpProtocolData().stream())
                .forEach(cpProtocolData -> {
                    if (cpProtocolData.getLayerProtocol() != OVER_ETHERNET) {
                        throw new InvalidInputException(String.format(EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE, extCp.getCpdId(),
                                                                      OVER_ETHERNET));
                    }

                    if (cpProtocolData.getIpOverEthernet() == null) {
                        throw new InvalidInputException(String.format(EXT_CP_MISSED_PROTOCOL_DATA_FIELD, extCp.getCpdId(),
                                                                      "ipOverEthernet"));
                    }
                });
    }

    private void validateVduCpNadExistence(ExtVirtualLinkData extVirtualLinks, VnfExtCpData extCp) {
        List<VnfExtCpConfig> vduCpConfigs = new ArrayList<>(extCp.getCpConfig().values());

        boolean isAllVduCpNadsContains = vduCpConfigs.stream().
                allMatch(e -> isNotEmpty(e.getNetAttDefResourceId()));

        Set<String> definedNads = extVirtualLinks.getExtNetAttDefResourceData()
                .stream()
                .map(NetAttDefResourceData::getNetAttDefResourceId)
                .collect(Collectors.toSet());

        if (!isAllVduCpNadsContains && isNotEmpty(definedNads)) {
            throw new InvalidInputException(String.format(VDU_CP_NAD_NOT_FOUND, extCp.getCpdId(), definedNads));
        }

        Set<String> nadsFromExtCp = extCp.getCpConfig().values().stream()
                .flatMap(vnfExtCpConfig -> vnfExtCpConfig.getNetAttDefResourceId().stream())
                .collect(Collectors.toSet());

        String missedNads = nadsFromExtCp.stream()
                .filter(nadFromExtCp -> !definedNads.contains(nadFromExtCp))
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(missedNads)) {
            throw new InvalidInputException(String.format(VDU_CP_NAD_NOT_FOUND, extCp.getCpdId(), missedNads));
        }
    }

    @Override
    public Class<VduCp> getType() {
        return VduCp.class;
    }
}
