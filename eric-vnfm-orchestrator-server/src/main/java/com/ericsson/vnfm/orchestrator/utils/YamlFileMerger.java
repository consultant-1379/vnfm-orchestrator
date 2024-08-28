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
package com.ericsson.vnfm.orchestrator.utils;

import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertMapToYamlFormat;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class YamlFileMerger implements FileMerger {

    /**
     * Take multiple Yaml documents in string format and merge them.
     * Note: latter file always override if there are same properties in the yaml file
     * ex: yaml1 has "test: a", yaml2 has "test: b", yaml3 has "test: c"
     * mergeYamls(yaml1, yaml2, yaml3) -> result: "test: c"
     *
     * Note: due to fault in spring library this method is unable to override
     * complex value with empty map
     * example nodeSelector: /n snmp-sender: eric-ccrc merged with nodeSelector: {}
     * return nodeSelector: /n snmp-sender: eric-ccrc
     *
     * @param fileContent set of YAML file as strings
     * @return merged YAML files
     */
    @Override
    public String merge(final String... fileContent) {
        ByteArrayResource[] valuesYamlFromFile = Arrays.stream(fileContent)
                .filter(Objects::nonNull)
                .map(String::getBytes)
                .map(ByteArrayResource::new)
                .toArray(ByteArrayResource[]::new);
        YamlMapFactoryBean factoryBean = new YamlMapFactoryBean();
        factoryBean.setResolutionMethod(YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE);
        factoryBean.setSingleton(false);
        factoryBean.setResources(valuesYamlFromFile);
        Map<String, Object> mergedValues = factoryBean.getObject();
        return convertMapToYamlFormat(mergedValues);
    }
}
