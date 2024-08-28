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
package com.ericsson.vnfm.orchestrator.presentation.helper;

import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.getAdditionalParamsYamlConversionResult;
import static com.ericsson.vnfm.orchestrator.utils.InstanceUtils.getCombinedValuesFileAsYaml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AdditionalAttributesHelper {

    public Map<String, Object> getCombinedAdditionalValuesMap(final VnfInstance instance) {
        String additionalParamsYamlAsString = getAdditionalParamsYamlConversionResult(instance);
        String combinedValuesFileYamlAsString = getCombinedValuesFileAsYaml(instance);
        ByteArrayResource combinedValuesResource = new ByteArrayResource(combinedValuesFileYamlAsString.getBytes(StandardCharsets.UTF_8));
        ByteArrayResource additionalParamsResource = new ByteArrayResource(additionalParamsYamlAsString.getBytes(StandardCharsets.UTF_8));
        YamlMapFactoryBean factoryBean = new YamlMapFactoryBean();
        factoryBean.setResolutionMethod(YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE);
        factoryBean.setSingleton(false);
        factoryBean.setResources(combinedValuesResource, additionalParamsResource);
        return factoryBean.getObject();
    }

}
