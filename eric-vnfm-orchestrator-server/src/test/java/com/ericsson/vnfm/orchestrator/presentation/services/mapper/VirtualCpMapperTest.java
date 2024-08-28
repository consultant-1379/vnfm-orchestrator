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

import static org.assertj.core.api.Assertions.assertThatNoException;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = { VirtualCpMapperImpl.class })
public class VirtualCpMapperTest {
    private static final String DESCRIPTOR_MODEL_FILENAME = "descriptorModel.json";
    private static final String VALID_INSTANTIATE_REQUEST_FILENAME = "validInstantiateRequest.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VirualCpMapper virualCpMapper;

    @Test
    public void testMapToDtoSuccess() throws Exception {
        String descriptorModel = readDataFromFile(getClass(), DESCRIPTOR_MODEL_FILENAME);
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_INSTANTIATE_REQUEST_FILENAME),
                InstantiateVnfRequest.class);

        assertThatNoException().isThrownBy(() ->
                                                   virualCpMapper.maptoDto(instantiateVnfRequest, new JSONObject(descriptorModel)));
    }
}
