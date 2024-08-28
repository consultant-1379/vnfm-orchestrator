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

import static com.ericsson.vnfm.orchestrator.model.CpProtocolData.LayerProtocolEnum.FOR_VIRTUAL_CP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_MISSED_PROTOCOL_DATA_FIELD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.NAD_PARAMETER_SHOULD_NOT_BE_PRESENT_FOR_VIRTUAL_CP;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.nestedvnfd.VirtualCp;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.ExtCpValidationService;

@Service
public class VirtualCpValidationServiceImpl implements ExtCpValidationService<VirtualCp> {

    @Override
    public void validate(final ExtVirtualLinkData extVirtualLinks, final VnfExtCpData extCp) {
        boolean isVirtualCpNadsContains = extCp.getCpConfig().values().stream()
                .anyMatch(e -> CollectionUtils.isNotEmpty(e.getNetAttDefResourceId()));
        if (isVirtualCpNadsContains) {
            throw new IllegalArgumentException(NAD_PARAMETER_SHOULD_NOT_BE_PRESENT_FOR_VIRTUAL_CP);
        }

        validateLayerProtocol(extCp);

    }

    private void validateLayerProtocol(final VnfExtCpData extCp) {

        extCp.getCpConfig().values().stream()
                .filter(vnfExtCpConfig -> !CollectionUtils.isEmpty(vnfExtCpConfig.getCpProtocolData()))
                .flatMap(vnfExtCpConfig -> vnfExtCpConfig.getCpProtocolData().stream())
                .forEach(cpProtocolData -> {
                    if (cpProtocolData.getLayerProtocol() != FOR_VIRTUAL_CP) {
                        throw new InvalidInputException(String.format(EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE, extCp.getCpdId(),
                                                                      FOR_VIRTUAL_CP));
                    }

                    if (cpProtocolData.getVirtualCpAddress() == null) {
                        throw new InvalidInputException(String.format(EXT_CP_MISSED_PROTOCOL_DATA_FIELD, extCp.getCpdId(),
                                                                      "virtualCpAddress"));
                    }
                });
    }

    @Override
    public Class<VirtualCp> getType() {
        return VirtualCp.class;
    }
}
