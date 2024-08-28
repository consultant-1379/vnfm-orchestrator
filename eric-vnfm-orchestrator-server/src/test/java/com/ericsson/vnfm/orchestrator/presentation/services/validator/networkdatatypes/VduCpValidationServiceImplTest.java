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
package com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_MISSED_PROTOCOL_DATA_FIELD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.VDU_CP_NAD_NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.CpProtocolData;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl.VduCpValidationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        VduCpValidationServiceImpl.class
})
public class VduCpValidationServiceImplTest {
    private static final String VALID_INSTANTIATE_REQUEST_FILENAME = "validInstantiateRequest.json";
    private static final String INVALID_INSTANTIATE_REQUEST_NOT_EXISTED_NAD_FILENAME = "invalidInstantiateRequestNotExistedNad.json";
    private static final String INVALID_VIRTUAL_LINK_WITH_WRONG_LAYER_PROTOCOL_FILENAME = "invalidVirtualLinkWithWrongVduCpLayerProtocol.json";
    private static final String INVALID_VIRTUAL_LINK_WITH_MISSED_IP_OVER_ETHERNET_FIELD = "invalidVirtualLinkWithMissedIpOverEthernetField.json";
    private static final String INVALID_INSTANTIATE_REQUEST_NOT_DEFINED_VDU_CP_NAD_JSON = "invalidInstantiateRequestNotDefinedVduCpNad.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VduCpValidationServiceImpl vduCpValidationServiceImpl;

    @Test
    public void validateVduCpSuccess() throws Exception{
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_INSTANTIATE_REQUEST_FILENAME),
                InstantiateVnfRequest.class);
        ExtVirtualLinkData extVirtualLinks = instantiateVnfRequest.getExtVirtualLinks().get(0);
        VnfExtCpData extCp = extVirtualLinks.getExtCps().get(0);

        assertThatNoException().isThrownBy(() -> vduCpValidationServiceImpl.validate(extVirtualLinks, extCp));
    }

    @Test
    public void validateVduCpWithNotExistedNad() throws Exception {
        String vduCpWithNotExitedNad = "test-cnf-vnfc1_vdu_cp_macvlan";
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_INSTANTIATE_REQUEST_NOT_EXISTED_NAD_FILENAME),
                InstantiateVnfRequest.class);
         ExtVirtualLinkData extVirtualLinks = instantiateVnfRequest.getExtVirtualLinks().stream()
                .filter(extVirtualLink -> extVirtualLink.getExtCps()
                        .stream().anyMatch(extCp -> extCp.getCpdId().equals(vduCpWithNotExitedNad)))
                .findAny().get();
        VnfExtCpData extCp = extVirtualLinks.getExtCps().stream()
                .filter(extCps -> extCps.getCpdId().equals(vduCpWithNotExitedNad)).findFirst().get();

        assertThatThrownBy(() -> vduCpValidationServiceImpl.validate(extVirtualLinks, extCp))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(VDU_CP_NAD_NOT_FOUND, vduCpWithNotExitedNad, "not-existed-nad"));
    }

    @Test
    public void validateVduCpWithWrongLayerProtocol() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_VIRTUAL_LINK_WITH_WRONG_LAYER_PROTOCOL_FILENAME),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);


        assertThatThrownBy(() -> vduCpValidationServiceImpl.validate(virtualLinkData, extCpData))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE,
                                          extCpData.getCpdId(), CpProtocolData.LayerProtocolEnum.OVER_ETHERNET));
    }

    @Test
    public void validateVduCpWithMissedIpOverEthernetField() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_VIRTUAL_LINK_WITH_MISSED_IP_OVER_ETHERNET_FIELD),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);


        assertThatThrownBy(() -> vduCpValidationServiceImpl.validate(virtualLinkData, extCpData))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_MISSED_PROTOCOL_DATA_FIELD,
                                          extCpData.getCpdId(), "ipOverEthernet"));
    }

    @Test
    public void validateVduCpNotDefinedVduCpNad() throws Exception {
        String vduCpWithNotExitedNad = "test-cnf-vnfc1_vdu_cp_macvlan";

        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_INSTANTIATE_REQUEST_NOT_DEFINED_VDU_CP_NAD_JSON),
                InstantiateVnfRequest.class);
        ExtVirtualLinkData extVirtualLinks = instantiateVnfRequest.getExtVirtualLinks().stream()
                .filter(extVirtualLink -> extVirtualLink.getExtCps()
                        .stream().anyMatch(extCp -> extCp.getCpdId().equals(vduCpWithNotExitedNad)))
                .findAny().orElseThrow();
        VnfExtCpData extCp = extVirtualLinks.getExtCps().stream()
                .filter(extCps -> extCps.getCpdId().equals(vduCpWithNotExitedNad)).findFirst().orElseThrow();

        assertThatThrownBy(() -> vduCpValidationServiceImpl.validate(extVirtualLinks, extCp))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(VDU_CP_NAD_NOT_FOUND, vduCpWithNotExitedNad, "[nad3]"));
    }
}