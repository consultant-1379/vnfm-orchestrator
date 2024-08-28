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
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_DUPLICATED_NAMES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl.NetworkDataTypeValidationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl.VduCpValidationServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl.VirtualCpValidationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        NetworkDataTypeValidationServiceImpl.class,
        ExtCpValidationServiceFactory.class,
        VduCpValidationServiceImpl.class,
        VirtualCpValidationServiceImpl.class
})
public class NetworkDataTypeValidationServiceImplTest {

    private static final String DESCRIPTOR_MODEL_FILENAME = "descriptorModel.json";
    private static final String VALID_INSTANTIATE_REQUEST_FILENAME = "validInstantiateRequest.json";
    private static final String INVALID_INSTANTIATE_REQUEST_DUPLICATED_EXT_CP_FILENAME = "invalidInstantiateRequestDuplicatedExtCps.json";
    private static final String INVALID_INSTANTIATE_REQUEST_NOT_DEFINED_EXT_CP_FILENAME = "invalidInstantiateRequestNotDefinedExtCp.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private NetworkDataTypeValidationService networkDataTypeValidationService;

    @Test
    public void testValidateNetworkDataTypeSuccess() throws Exception{
        String descriptorModel = readDataFromFile(getClass(), DESCRIPTOR_MODEL_FILENAME);
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_INSTANTIATE_REQUEST_FILENAME),
                InstantiateVnfRequest.class);

        assertThatNoException().isThrownBy(() ->
                                                   networkDataTypeValidationService.
                                                           validate(descriptorModel,
                                                                    instantiateVnfRequest.getExtVirtualLinks()));
    }

    @Test
    public void testValidateNetworkDataTypesWithDuplicatedExtCps() throws Exception {
        String descriptorModel = readDataFromFile(getClass(), DESCRIPTOR_MODEL_FILENAME);
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_INSTANTIATE_REQUEST_DUPLICATED_EXT_CP_FILENAME),
                InstantiateVnfRequest.class);

        assertThatThrownBy(() -> networkDataTypeValidationService.validate(descriptorModel,
                                                                           instantiateVnfRequest.getExtVirtualLinks()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_DUPLICATED_NAMES, "test-cnf_vdu_cp_macvlan, eric-pm-bulk-reporter_virtual_cp"));
    }

    @Test
    public void testValidateNetworkDataTypeWithNotExistedExtCp() throws Exception {
        String descriptorModel = readDataFromFile(getClass(), DESCRIPTOR_MODEL_FILENAME);
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), INVALID_INSTANTIATE_REQUEST_NOT_DEFINED_EXT_CP_FILENAME),
                InstantiateVnfRequest.class);

        assertThatThrownBy(() -> networkDataTypeValidationService.validate(descriptorModel,
                                                                           instantiateVnfRequest.getExtVirtualLinks()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(String.format(EXT_CP_NOT_FOUND, "not-defined-ext-cp"));
    }

}