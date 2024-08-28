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

import static com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.TGZ;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.TOSCA_ARTIFACTS_FILE;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.ericsson.am.shared.vnfd.CommonUtility;
import com.ericsson.am.shared.vnfd.DataTypeUtility;
import com.ericsson.am.shared.vnfd.InterfaceTypeUtility;
import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.NodeTypeUtility;
import com.ericsson.am.shared.vnfd.TopologyTemplateUtility;
import com.ericsson.am.shared.vnfd.model.ArtifactsPropertiesDetail;
import com.ericsson.am.shared.vnfd.model.DataType;
import com.ericsson.am.shared.vnfd.model.InterfaceType;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeType;

public final class ReplicaDetailsUtility {

    private ReplicaDetailsUtility() {
    }

    public static Map<String, DataType> getTopologyTemplateInputs(final String descriptorModel) {
        var topologyTemplate = TopologyTemplateUtility.createTopologyTemplate(
                new JSONObject(descriptorModel), getNodeType(descriptorModel));
        return topologyTemplate.getInputs();
    }

    public static NodeType getNodeType(final String descriptorModel) {
        var vnfdJsonObject = new JSONObject(descriptorModel);
        return getNodeType(vnfdJsonObject);
    }

    public static NodeType getNodeType(JSONObject vnfd) {
        final Map<String, DataType> allDataType = DataTypeUtility.buildDataTypesFromVnfd(vnfd);
        final Map<String, InterfaceType> allInterfaceType = InterfaceTypeUtility.getInterfaceTypeFromVnfd(vnfd, allDataType);
        return NodeTypeUtility.buildNodeType(vnfd, allDataType, allInterfaceType);
    }

    public static NodeTemplate getNodeTemplate(final String descriptorModel) {
        JSONObject vnfd = new JSONObject(descriptorModel);
        NodeType nodeType = ReplicaDetailsUtility.getNodeType(descriptorModel);
        return NodeTemplateUtility.createNodeTemplate(nodeType, vnfd);
    }

    public static String getTargetChartName(final ScaleMapping scaleMapping, final String descriptorModel) {
        var mciopName = scaleMapping.getMciopName();
        var mapMciopNameToChartUrl = getMapMciopNameToChartUrl(descriptorModel);
        return StringUtils.substringAfterLast(mapMciopNameToChartUrl.get(mciopName), "/");
    }

    public static Map<String, String> getMapMciopNameToChartUrl(final String descriptorModel) {
        Map<String, String> mapMciopNameToChartUrl = new HashMap<>();
        JSONObject vnfd = new JSONObject(descriptorModel);
        if (vnfd.has(NODE_TYPES_KEY) && vnfd.getJSONObject(NODE_TYPES_KEY) != null) {
            JSONObject nodeType = vnfd.getJSONObject(NODE_TYPES_KEY);
            for (final ArtifactsPropertiesDetail artifact : CommonUtility.getArtifacts(nodeType)) {
                if (artifact.getType().equals(TOSCA_ARTIFACTS_FILE) && artifact.getFile().endsWith(TGZ)) {
                    mapMciopNameToChartUrl.put(artifact.getId(), artifact.getFile());
                }
            }
        }
        return mapMciopNameToChartUrl;
    }
}
