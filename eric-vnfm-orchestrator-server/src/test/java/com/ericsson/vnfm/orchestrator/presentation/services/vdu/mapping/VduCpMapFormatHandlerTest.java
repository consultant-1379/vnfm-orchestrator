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
package com.ericsson.vnfm.orchestrator.presentation.services.vdu.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.VALUES_FILE_PREFIX;
import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.dto.VduCpDto;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaDetailsService;
import com.ericsson.vnfm.orchestrator.utils.FileMerger;
import com.ericsson.vnfm.orchestrator.utils.YamlFileMerger;


@SpringBootTest(classes = {
        ValuesFileService.class,
        ValuesFileComposer.class,
        FileMerger.class,
        YamlFileMerger.class,
        VduCpMapFormatHandler.class
})
@MockBean(classes = {
        ReplicaDetailsService.class,
        ReplicaDetailsMapper.class,
        AdditionalAttributesHelper.class,
        PackageService.class
})
public class VduCpMapFormatHandlerTest {

    private static final Logger LOGGER = getLogger(VduCpMapFormatHandlerTest.class);

    @Autowired
    private VduCpMapFormatHandler vduCpMapFormatHandler;

    @Test
    public void formatParametersToMapTest() {
        Path toValuesFile = createValuesFile();
        Map<String, Object> vduParams = convertYamlFileIntoMap(toValuesFile);

        final VduCpDto vduCpDto = createVduCpDto();

        vduCpMapFormatHandler.formatParameters(vduCpDto, vduParams);


        assertThat(vduParams).containsKey("vnfc1");

        Map<String, Map<String, Map<String, String>>> interfaceParams = (Map<String, Map<String, Map<String, String>>>) vduParams.get("vnfc1");

        assertThat(interfaceParams).containsKey("test-cnf");
        assertThat(interfaceParams.get("test-cnf")).hasSize(3);
        assertThat(interfaceParams.get("test-cnf")).containsKey("macvlan1");
        assertThat(interfaceParams.get("test-cnf").get("macvlan1").get("name")).isEqualTo("macvlan-1");
        assertThat(interfaceParams.get("test-cnf").get("macvlan1").get("namespace")).isEqualTo("nfvoNamespace1");
        assertThat(interfaceParams.get("test-cnf")).containsKey("net0");
        assertThat(interfaceParams.get("test-cnf").get("net0").get("name")).isEqualTo("macvlan-2");
        assertThat(interfaceParams.get("test-cnf").get("net0").get("namespace")).isEqualTo("nfvoNamespace2");
        assertThat(interfaceParams.get("test-cnf")).containsKey("net2");
        assertThat(interfaceParams.get("test-cnf").get("net2").get("name")).isEqualTo("macvlan-3");
        assertThat(interfaceParams.get("test-cnf").get("net2").get("namespace")).isEqualTo("nfvoNamespace3");

        deleteFile(toValuesFile);
    }

    private Path createValuesFile() {
        Path toValuesFile;
        try {
            toValuesFile = Files.createTempFile(VALUES_FILE_PREFIX, ".yaml");
        } catch (IOException ioe) {
            LOGGER.error("Error occurred while creating values file {}", ioe.getMessage());
            throw new InternalRuntimeException("Unable to create the values file due to " + ioe.getMessage());
        }

        return toValuesFile;
    }

    private VduCpDto createVduCpDto() {
        VduCpDto vduCpDto = new VduCpDto();
        vduCpDto.setFormat("map");
        vduCpDto.setPath("vnfc1.test-cnf");

        List<VduCpDto.VduCp> vduCps = new ArrayList<>();
        vduCps.add(createFirstVduCp());
        vduCps.add(createSecondVduCp());

        vduCpDto.setVduCps(vduCps);
        return vduCpDto;
    }

    private VduCpDto.VduCp createFirstVduCp() {
        VduCpDto.VduCp vduCp = new VduCpDto.VduCp();

        vduCp.setCpId("test-cnf_vdu_cp_macvlan");
        vduCp.setOrder(1);

        List<Triple<String, String, String>> interfaceToNadParams = new ArrayList<>();
        interfaceToNadParams.add(Triple.of("macvlan1", "macvlan-1", "nfvoNamespace1"));
        interfaceToNadParams.add(Triple.of("net0", "macvlan-2", "nfvoNamespace2"));

        vduCp.setInterfaceToNadParams(interfaceToNadParams);

        return vduCp;
    }

    private VduCpDto.VduCp createSecondVduCp() {
        VduCpDto.VduCp vduCp = new VduCpDto.VduCp();

        vduCp.setCpId("test-cnf_vdu_cp_normal");
        vduCp.setOrder(2);

        List<Triple<String, String, String>> interfaceToNadParams = new ArrayList<>();
        interfaceToNadParams.add(Triple.of("net2", "macvlan-3", "nfvoNamespace3"));

        vduCp.setInterfaceToNadParams(interfaceToNadParams);

        return vduCp;
    }
}
