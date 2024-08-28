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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.dto.VduCpDto;
import com.ericsson.vnfm.orchestrator.model.entity.VduCpMappingFormat;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;

@Service
public class VduCpMapFormatHandler implements VduCpMappingFormatHandler {

    private static final String NAD_NAME = "name";
    private static final String NAD_NAMESPACE = "namespace";

    @Autowired
    private ValuesFileService valuesService;

    @Override
    public VduCpMappingFormat getType() {
        return VduCpMappingFormat.MAP;
    }

    @Override
    public void formatParameters(final VduCpDto vduCpDto, final Map<String, Object> valuesYamlMap) {
        Map<String, Object> formattedVduParametrs = new HashMap<>();
        Map<String, Map<String, String>> interfaceParams = new HashMap<>();

        for (VduCpDto.VduCp vduCp: vduCpDto.getVduCps()) {
            for (Triple<String, String, String> interfaceToNadParams: vduCp.getInterfaceToNadParams()) {
                interfaceParams.put(interfaceToNadParams.getLeft(), new HashMap<>(Map.of(NAD_NAME, interfaceToNadParams.getMiddle(),
                                                                           NAD_NAMESPACE, interfaceToNadParams.getRight())));
            }
        }

        formattedVduParametrs.put(vduCpDto.getPath(), interfaceParams);

        valuesService.valuesFileWithVduInterfaceParam(valuesYamlMap, formattedVduParametrs);
    }
}
