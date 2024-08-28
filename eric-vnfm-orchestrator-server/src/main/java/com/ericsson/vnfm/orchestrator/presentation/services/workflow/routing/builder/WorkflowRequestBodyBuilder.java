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
package com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.builder;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ericsson.vnfm.orchestrator.model.HttpFileResource;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.vnfm.orchestrator.utils.YamlUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WorkflowRequestBodyBuilder {

    public static final String JSON_REQUEST_PARAMETER_NAME = "json";
    public static final String VALUES_REQUEST_PARAMETER_NAME = "values";
    public static final String SECOND_VALUES_REQUEST_PARAMETER_NAME = "additionalValues";
    public static final String CONFIG_FILE_REQUEST_PARAMETER_NAME = "clusterConfig";

    private final ClusterConfigService clusterConfigService;

    public WorkflowRequestBodyBuilder(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    public <T> MultiValueMap<String, Object> buildRequestBody(T jsonRequestParams, String clusterName, Path valuesFilePath) {
        MultiValueMap<String, Object> resultBody = new LinkedMultiValueMap<>();
        LOGGER.info("Building wfs request with parameters: clusterName={}, valuesFilePath={}", clusterName, valuesFilePath);

        Object nonNullJsonRequestPart = ObjectUtils.defaultIfNull(jsonRequestParams, Collections.emptyMap());
        resultBody.add(JSON_REQUEST_PARAMETER_NAME, convertObjToJsonString(nonNullJsonRequestPart));

        if (valuesFilePath != null && !Utility.isEmptyFile(valuesFilePath)) {
            resultBody.add(VALUES_REQUEST_PARAMETER_NAME, new FileSystemResource(valuesFilePath.toFile()));
        }

        HttpFileResource clusterConfigFile = getClusterConfigParameter(clusterName);
        resultBody.add(CONFIG_FILE_REQUEST_PARAMETER_NAME, clusterConfigFile);
        return resultBody;
    }

    public <T> MultiValueMap<String, Object> buildRequestBody(T jsonRequestParams, String clusterName, Path valuesFilePath,
                                                              Path secondValuesPath) {
        MultiValueMap<String, Object> resultBody = buildRequestBody(jsonRequestParams, clusterName, valuesFilePath);

        if (secondValuesPath != null && !Utility.isEmptyFile(secondValuesPath)) {
            resultBody.add(SECOND_VALUES_REQUEST_PARAMETER_NAME, new FileSystemResource(secondValuesPath.toFile()));
        }

        return resultBody;
    }

    public Path buildValuesPathFile(Map<String, Object> valuesParameter) {
        if (!CollectionUtils.isEmpty(valuesParameter)) {
            return YamlUtility.writeMapToValuesFile(valuesParameter);
        }
        return null;
    }

    private HttpFileResource getClusterConfigParameter(String clusterName) {
        ClusterConfigFile configFileByName = clusterConfigService.getOrDefaultConfigFileByName(clusterName);
        return new HttpFileResource(configFileByName.getContent().getBytes(StandardCharsets.UTF_8), configFileByName.getName());
    }
}
