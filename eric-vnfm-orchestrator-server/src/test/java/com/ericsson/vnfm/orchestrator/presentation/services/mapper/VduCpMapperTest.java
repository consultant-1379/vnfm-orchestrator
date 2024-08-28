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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.dto.VduCpDto;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        VduCpMapperImpl.class,
        VnfdHelper.class
})
public class VduCpMapperTest {
    private static final String DESCRIPTOR_MODEL_FILENAME = "descriptorModel.json";
    private static final String VALID_INSTANTIATE_REQUEST_FILENAME = "validInstantiateRequest.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VduCpMapper vduCpMapper;


    @Test
    public void testMapToDto() throws IOException {
        String descriptorModel = readDataFromFile(getClass(), DESCRIPTOR_MODEL_FILENAME);
        InstantiateVnfRequest instantiateVnfRequest = objectMapper.readValue(
                readDataFromFile(getClass(), VALID_INSTANTIATE_REQUEST_FILENAME),
                InstantiateVnfRequest.class);

        final List<VduCpDto> vduCpDtos = vduCpMapper.mapToDto(instantiateVnfRequest, new JSONObject(descriptorModel));
        Optional<VduCpDto> mapVduCp = vduCpDtos.stream()
                .filter(vduCpDto -> vduCpDto.getFormat().equals("map"))
                .findFirst();

        Optional<VduCpDto> listVduCp = vduCpDtos.stream()
                .filter(vduCpDto -> vduCpDto.getFormat().equals("list"))
                .findFirst();

        assertThat(vduCpDtos.size()).isEqualTo(3);
        assertThat(mapVduCp).isPresent()
                .isNotEmpty();
        assertThat(mapVduCp.get().getPath()).isEqualTo("vnfc1.test-cnf");
        assertThat(mapVduCp.get().getParam()).isEqualTo("networks");
        assertThat(mapVduCp.get().getVduCps().size()).isEqualTo(2);
        assertThat(mapVduCp.get().getVduCps().get(0).getInterfaceToNadParams().size()).isEqualTo(2);
        assertThat(mapVduCp.get().getVduCps().get(0).getCpId()).isNotEmpty();
        assertThat(mapVduCp.get().getVduCps().get(0).getOrder()).isNotNull();


        assertThat(listVduCp).isPresent()
                .isNotEmpty();
        assertThat(listVduCp.get().getPath()).isEqualTo("vnfc1");
        assertThat(listVduCp.get().getParam()).isEqualTo("networks");
        assertThat(listVduCp.get().getMultusInterface()).isEqualTo("net0");
        assertThat(listVduCp.get().getSet()).contains("interfaces=[]");
        assertThat(listVduCp.get().getVduCps().size()).isEqualTo(1);
        assertThat(listVduCp.get().getVduCps().get(0).getInterfaceToNadParams().size()).isEqualTo(1);
        assertThat(listVduCp.get().getVduCps().get(0).getCpId()).isNotEmpty();
        assertThat(listVduCp.get().getVduCps().get(0).getOrder()).isNotNull();
    }
}
