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
package com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.impl;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_DUPLICATED_NAMES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.EXT_CP_NOT_FOUND;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.DataTypeUtility;
import com.ericsson.am.shared.vnfd.InterfaceTypeUtility;
import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.NodeTypeUtility;
import com.ericsson.am.shared.vnfd.model.DataType;
import com.ericsson.am.shared.vnfd.model.InterfaceType;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeType;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VduCp;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.VirtualCp;
import com.ericsson.vnfm.orchestrator.model.ExtVirtualLinkData;
import com.ericsson.vnfm.orchestrator.model.VnfExtCpData;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.ExtCpValidationService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.ExtCpValidationServiceFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;

@Service
public class NetworkDataTypeValidationServiceImpl implements NetworkDataTypeValidationService {

    @Autowired
    private ExtCpValidationServiceFactory extCpValidationServiceFactory;

    @Override
    public void validate(final String descriptorModel, final List<ExtVirtualLinkData> extVirtualLinks) {
        NodeTemplate nodeTemplate = NodeTemplateUtility.createNodeTemplate(getNodeType(descriptorModel),
                                                                           new JSONObject(descriptorModel));

        validateExtCpDuplicates(extVirtualLinks);
        validateExtCps(extVirtualLinks, nodeTemplate);
    }

    private void validateExtCpDuplicates(final List<ExtVirtualLinkData> extVirtualLinks) {
        List<String> extCpNames = extVirtualLinks.stream().flatMap(extVirtualLink -> extVirtualLink.getExtCps().stream())
                .map(VnfExtCpData::getCpdId).collect(Collectors.toList());

        Set<String> duplicatedNames = findDuplicates(extCpNames);
        if (!CollectionUtils.isEmpty(duplicatedNames)) {
            throw new InvalidInputException(String.format(EXT_CP_DUPLICATED_NAMES, String.join(", ", duplicatedNames)));
        }
    }

    private Set<String> findDuplicates(final List<String> extCpNames) {
        Set<String> uniqueValues = new HashSet<>();
        Set<String> duplicateValues = new HashSet<>();
        extCpNames.forEach(extCpName -> {
            if (!uniqueValues.add(extCpName)) {
                duplicateValues.add(extCpName);
            }
        });
        return duplicateValues;
    }

    private void validateExtCps(final List<ExtVirtualLinkData> extVirtualLinks, final NodeTemplate nodeTemplate) {
        extVirtualLinks.forEach(extVirtualLink -> {
            List<VnfExtCpData> extCpsList = extVirtualLink.getExtCps();

            extCpsList.forEach(extCp -> {
                Object vnfdDataType = getVnfdDataType(extCp.getCpdId(), nodeTemplate);
                ExtCpValidationService<?> extCpValidationService = extCpValidationServiceFactory
                        .getValidationService(vnfdDataType.getClass());
                extCpValidationService.validate(extVirtualLink, extCp);
            });
        });
    }

    private static NodeType getNodeType(final String descriptorModel) {
        var vnfdJsonObject = new JSONObject(descriptorModel);
        final Map<String, DataType> allDataType = DataTypeUtility.buildDataTypesFromVnfd(vnfdJsonObject);
        final Map<String, InterfaceType> allInterfaceType = InterfaceTypeUtility.getInterfaceTypeFromVnfd(vnfdJsonObject, allDataType);
        return NodeTypeUtility.buildNodeType(vnfdJsonObject, allDataType, allInterfaceType);
    }

    private static Object getVnfdDataType(String extCpId, NodeTemplate nodeTemplate) {

        for (VduCp vduCp: nodeTemplate.getVduCps()) {
            if (vduCp.getName().equals(extCpId)) {
                return vduCp;
            }
        }

        for (VirtualCp virtualCp: nodeTemplate.getVirtualCps()) {
            if (virtualCp.getName().equals(extCpId)) {
                return virtualCp;
            }
        }

        throw new InvalidInputException(String.format(EXT_CP_NOT_FOUND, extCpId));
    }
}
