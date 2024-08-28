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
package com.ericsson.vnfm.orchestrator.presentation.services.replicadetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlFileIntoMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class ReplicaDetailsServiceTest extends ReplicaDetailsTestCommon {

    @Mock
    private DefaultReplicaDetailsCalculationService defaultReplicaDetailsCalculationService;

    @Mock
    private ScaleMappingReplicaDetailsCalculationService scaleMappingReplicaDetailsCalculationService;

    @Mock
    private MappingFileService mappingFileService;

    @Mock
    private ScaleService scaleService;

    @InjectMocks
    private ReplicaDetailsService replicaDetailsService = new ReplicaDetailsServiceImpl();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException {
        ReplicaDetailsMapper replicaDetailsMapper = new ReplicaDetailsMapper(objectMapper);
        ReflectionTestUtils.setField(replicaDetailsService, "replicaDetailsMapper", replicaDetailsMapper);
        super.setUp();
    }

    @Test
    public void updateAndSetReplicaDetailsToVnfInstanceScalingMapping() throws JsonProcessingException {
        Optional<String> scaleMappingPathFromVnfd = Optional.of("/Definitions/OtherTemplates/scaling_mapping.yaml");
        Map<String, ReplicaDetails> replicaDetails = buildExpectedReplicaDetails(true);
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(toValuesFile);

        Map<String, ReplicaDetails> expectedSpiderReplicaDetails = new HashMap<>(replicaDetails);
        ReplicaDetails ericPmBulkReporterReplicaDetails = expectedSpiderReplicaDetails.remove("eric-pm-bulk-reporter");
        Map<String, ReplicaDetails> expectedScaleReplicaDetails = Map.of("eric-pm-bulk-reporter", ericPmBulkReporterReplicaDetails);

        when(mappingFileService.getScaleMapFilePathFromDescriptorModel(vnfd))
                .thenReturn(scaleMappingPathFromVnfd);
        when(mappingFileService.getMappingFile(scaleMappingPathFromVnfd.get(), vnfInstanceRel4))
                .thenReturn(scaleMappingMap);
        when(scaleMappingReplicaDetailsCalculationService.calculate(vnfd, scaleMappingMap, vnfInstanceRel4, valuesYamlMap))
                .thenReturn(replicaDetails);


        replicaDetailsService.updateAndSetReplicaDetailsToVnfInstance(vnfd, vnfInstanceRel4, valuesYamlMap);

        Map<String, ReplicaDetails> spiderReplicaDetails = getAllReplicaDetailsFromHelmChart(List.of("1"));
        Map<String, ReplicaDetails> scaleReplicaDetails = getAllReplicaDetailsFromHelmChart(List.of("2"));

        assertEquals(expectedScaleReplicaDetails, scaleReplicaDetails);
        assertEquals(expectedSpiderReplicaDetails, spiderReplicaDetails);

        verify(scaleService, times(1)).setReplicaParameterForScaleInfo(vnfInstanceRel4);
    }

    @Test
    public void updateAndSetReplicaDetailsToVnfInstanceDefault() throws JsonProcessingException {
        Map<String, ReplicaDetails> replicaDetails = buildExpectedReplicaDetails(false);
        Map<String, Object> valuesYamlMap = convertYamlFileIntoMap(toValuesFile);

        when(mappingFileService.getScaleMapFilePathFromDescriptorModel(vnfd))
                .thenReturn(Optional.empty());
        when(defaultReplicaDetailsCalculationService.calculate(vnfInstanceRel4))
                .thenReturn(replicaDetails);

        replicaDetailsService.updateAndSetReplicaDetailsToVnfInstance(vnfd, vnfInstanceRel4, valuesYamlMap);

        Map<String, ReplicaDetails> allReplicaDetails = getAllReplicaDetailsFromHelmChart(List.of("1", "2"));

        assertEquals(replicaDetails, allReplicaDetails);

        verify(scaleService, times(1)).setReplicaParameterForScaleInfo(vnfInstanceRel4);
    }

    private Map<String, ReplicaDetails> getAllReplicaDetailsFromHelmChart(List<String> helmChartIds) throws JsonProcessingException {
        final String allReplicaDetailsAsString = vnfInstanceRel4.getHelmCharts()
                .stream()
                .filter(helmChart -> helmChartIds.contains(helmChart.getId()))
                .map(HelmChart::getReplicaDetails)
                .collect(Collectors.joining());
        return objectMapper.readValue(allReplicaDetailsAsString, new TypeReference<>() {});
    }
}
