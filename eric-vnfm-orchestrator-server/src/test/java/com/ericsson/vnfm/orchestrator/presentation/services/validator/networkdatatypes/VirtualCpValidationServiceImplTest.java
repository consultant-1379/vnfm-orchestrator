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
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.NAD_PARAMETER_SHOULD_NOT_BE_PRESENT_FOR_VIRTUAL_CP;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.CpProtocolData;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl.VirtualCpValidationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        VirtualCpValidationServiceImpl.class
})
public class VirtualCpValidationServiceImplTest {

    private static final String VALID_VIRTUAL_LINK_WITH_VIRTUAL_CP_FILENAME = "validVirtualLinkWithVirtualCp.json";
    private static final String VALID_VIRTUAL_LINK_WITHOUT_CP_PROTOCOL_DATA_FILENAME = "validVirtualLinkWithoutCpProtocolData.json";
    private static final String INVALID_VIRTUAL_LINK_WITH_WRONG_LAYER_PROTOCOL_FILENAME = "invalidVirtualLinkWithWrongVirtualCpLayerProtocol.json";
    private static final String INVALID_VIRTUAL_LINK_WITH_MISSED_VIRTUAL_CP_ADDRESS_FIELD = "invalidVirtualLinkWithMissedVirtualCpAddressField"
            + ".json";
    private static final String INVALID_INSTANTIATE_REQUEST_VIRTUAL_CP_NAD_JSON = "invalidInstantiateRequestVirtualCpNad.json";


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VirtualCpValidationServiceImpl virtualCpValidationService;

    @Test
    public void validateVirtualCpSuccess() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_VIRTUAL_LINK_WITH_VIRTUAL_CP_FILENAME),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);

        assertThatNoException().isThrownBy(() -> virtualCpValidationService.validate(virtualLinkData, extCpData));
    }

    @Test
    public void validateVirtualCpWithoutCpProtocolDataSuccess() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_VIRTUAL_LINK_WITHOUT_CP_PROTOCOL_DATA_FILENAME),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);

        assertThatNoException().isThrownBy(() -> virtualCpValidationService.validate(virtualLinkData, extCpData));
    }

    @Test
    public void validateVirtualCpWithWrongLayerProtocol() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_VIRTUAL_LINK_WITH_WRONG_LAYER_PROTOCOL_FILENAME),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);


        assertThatThrownBy(() -> virtualCpValidationService.validate(virtualLinkData, extCpData))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_WRONG_LAYER_PROTOCOL_ERROR_MESSAGE,
                                          extCpData.getCpdId(), CpProtocolData.LayerProtocolEnum.FOR_VIRTUAL_CP));
    }

    @Test
    public void validateVirtualCpWithMissedVirtualCpAddressField() throws Exception {
        ExtVirtualLinkData virtualLinkData = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_VIRTUAL_LINK_WITH_MISSED_VIRTUAL_CP_ADDRESS_FIELD),
                ExtVirtualLinkData.class);

        VnfExtCpData extCpData = virtualLinkData.getExtCps().get(0);


        assertThatThrownBy(() -> virtualCpValidationService.validate(virtualLinkData, extCpData))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_MISSED_PROTOCOL_DATA_FIELD,
                                          extCpData.getCpdId(), "virtualCpAddress"));
    }

    @Test
    public void testValidateExistedNadFailed() throws Exception {
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_INSTANTIATE_REQUEST_VIRTUAL_CP_NAD_JSON),
                InstantiateVnfRequest.class);
        ExtVirtualLinkData extVirtualLinks = instantiateVnfRequest.getExtVirtualLinks().get(0);
        VnfExtCpData extCp = extVirtualLinks.getExtCps().get(1);

        assertThatThrownBy(() -> virtualCpValidationService.validate(extVirtualLinks, extCp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NAD_PARAMETER_SHOULD_NOT_BE_PRESENT_FOR_VIRTUAL_CP);
    }

}