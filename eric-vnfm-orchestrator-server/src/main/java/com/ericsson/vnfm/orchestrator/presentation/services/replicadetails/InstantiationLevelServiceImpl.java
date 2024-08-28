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

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.LEVEL_ID_NOT_PRESENT_IN_VNFD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.DEFAULT_INSTANTIATION_LEVEL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevels;
import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevelsDataInfo;
import com.ericsson.am.shared.vnfd.model.policies.InstantiationLevelsProperties;
import com.ericsson.am.shared.vnfd.model.policies.InstantiationScaleLevels;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.VduInstantiationLevels;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.sync.SyncOperationValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstantiationLevelServiceImpl implements InstantiationLevelService {

    private static final String INSTANTIATION_LEVELS_TYPE = "tosca.policies.nfv.InstantiationLevels";

    private final ReplicaDetailsMapper replicaDetailsMapper;

    private final SyncOperationValidator syncOperationValidator;

    public void validateInstantiationLevelInPoliciesOfVnfInstance(final VnfInstance vnfInstance,
                                                                  final String level) {
        Map<String, InstantiationLevels> allInstantiationLevels = new HashMap<>();
        if (StringUtils.isNotEmpty(level)) {
            allInstantiationLevels = getInstantiationLevelsFromPolicies(vnfInstance);
        }
        if (MapUtils.isNotEmpty(allInstantiationLevels)) {
            verifyInstantiationLevelIdPresentInVNFD(level, allInstantiationLevels);
        }
    }

    public List<VduInstantiationLevels> getMatchingVduInstantiationLevels(Policies policies, String level) {
        return policies.getAllVduInstantiationLevels().values().stream()
                .filter(e -> e.getProperties().getInstantiationLevels().containsKey(level))
                .collect(Collectors.toList());
    }

    public Optional<InstantiationLevelsDataInfo> getMatchingInstantiationLevel(Policies policies, String level) {
        return policies.getAllInstantiationLevels().values().stream()
                .map(InstantiationLevels::getProperties)
                .map(InstantiationLevelsProperties::getInstantiationLevelsDataInfo)
                .flatMap(map -> map.entrySet().stream().filter(e -> e.getKey().equals(level))).findFirst().map(Map.Entry::getValue);
    }

    @Override
    public void setDefaultInstantiationLevelToVnfInstance(final VnfInstance vnfInstance) {
        var vnfInstancePolicies = replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance);
        if (MapUtils.isNotEmpty(vnfInstancePolicies.getAllInstantiationLevels())) {
            String defaultLevel = getDefaultInstantiationLevelFromPolicies(vnfInstancePolicies);
            vnfInstance.setInstantiationLevel(defaultLevel);
            setScaleLevelForInstantiationLevel(vnfInstance, defaultLevel, vnfInstancePolicies);
            LOGGER.info("Default instantiation level set to vnfInstance {}", defaultLevel);
        }
    }

    public String getDefaultInstantiationLevelFromPolicies(final Policies vnfInstancePolicies) {
        final Map<String, InstantiationLevels> allInstantiationLevels = vnfInstancePolicies.getAllInstantiationLevels();
        if (MapUtils.isEmpty(allInstantiationLevels)) {
            return null;
        }
        validateInstantiationLevels(allInstantiationLevels);
        return allInstantiationLevels
                .values()
                .iterator()
                .next()
                .getProperties()
                .getDefaultLevel();
    }

    @Override
    public void setScaleLevelForInstantiationLevel(final VnfInstance vnfInstance,
                                                   final String instantiationLevel,
                                                   final Policies vnfInstancePolicies) {

        Optional<InstantiationLevelsDataInfo> levelsDataInfo =
                getMatchingInstantiationLevel(vnfInstancePolicies, instantiationLevel);

        if (levelsDataInfo.isPresent()) {
            levelsDataInfo.map(InstantiationLevelsDataInfo::getScaleInfo).ifPresent(scaleInfo -> scaleInfo
                    .forEach((aspectNameAssociatedWithLevel, value) -> {
                        Integer scaleLevelForAspect = value.getScaleLevel();
                        setScaleLevelForSpecificAspect(vnfInstance, aspectNameAssociatedWithLevel, scaleLevelForAspect);
                    }));
        } else {
            throw new IllegalArgumentException("No Instantiation Level data present for default level");
        }
    }

    /**
     * Populates scaleLevel values for aspects from the "targetScaleLevelInfo" request param. If the scaleLevel value
     * is not present in the request, the default value will be set.
     *
     * @param vnfInstance Instance to update
     * @param targetScaleLevelInfo targetScaleLevelInfo from the request
     * @param vnfInstancePolicies Policies of vnfInstance
     */
    @Override
    public void setScaleLevelForTargetScaleLevelInfo(VnfInstance vnfInstance,
                                                     List<ScaleInfo> targetScaleLevelInfo,
                                                     Policies vnfInstancePolicies) {

        for (ScaleInfoEntity scaleInfoEntity : vnfInstance.getScaleInfoEntity()) {
            String aspectId = scaleInfoEntity.getAspectId();
            Optional<ScaleInfo> scaleInfoFromRequest = targetScaleLevelInfo.stream()
                    .filter(scaleInfo -> aspectId.equals(scaleInfo.getAspectId()))
                    .findFirst();

            if (scaleInfoFromRequest.isPresent()) {
                Integer scaleLevel = scaleInfoFromRequest.get().getScaleLevel();

                syncOperationValidator
                        .validateScaleLevelDoesNotExceedMaxLevel(vnfInstance, aspectId, aspectId, scaleLevel);
                scaleInfoEntity.setScaleLevel(scaleLevel);
            } else {
                scaleInfoEntity.setScaleLevel(getDefaultInstantiationLevelForAspect(aspectId, vnfInstancePolicies));
            }
        }
    }

    /**
     * Gets the default instantiation level for aspect by ID or 0 if there is no data present for this aspect in VNF.
     *
     * @param aspectId ID of aspect
     * @param vnfInstancePolicies VNF policies object
     * @return default instantiation level of 0 if not present
     */
    private Integer getDefaultInstantiationLevelForAspect(String aspectId, Policies vnfInstancePolicies) {
        return getMatchingInstantiationLevel(
                vnfInstancePolicies, getDefaultInstantiationLevelFromPolicies(vnfInstancePolicies))
                .map(instantiationLevelsDataInfo -> Optional.ofNullable(instantiationLevelsDataInfo.getScaleInfo().get(aspectId))
                        .map(InstantiationScaleLevels::getScaleLevel)
                        .orElse(DEFAULT_INSTANTIATION_LEVEL))
                .orElse(DEFAULT_INSTANTIATION_LEVEL);
    }

    private static void setScaleLevelForSpecificAspect(final VnfInstance vnfInstance,
                                                       final String aspectNameAssociatedWithLevel,
                                                       final Integer scaleLevelForAspect) {
        for (ScaleInfoEntity scaleInfoEntity : vnfInstance.getScaleInfoEntity()) {
            if (scaleInfoEntity.getAspectId().equals(aspectNameAssociatedWithLevel)) {
                scaleInfoEntity.setScaleLevel(scaleLevelForAspect);
            }
        }
    }

    private static void verifyInstantiationLevelIdPresentInVNFD(final String targetInstantiationLevelId,
                                                                final Map<String, InstantiationLevels> allInstantiationLevels) {
        allInstantiationLevels.values().stream()
                .filter(e -> INSTANTIATION_LEVELS_TYPE.equals(e.getType()))
                .map(InstantiationLevels::getProperties)
                .map(InstantiationLevelsProperties::getInstantiationLevelsDataInfo)
                .flatMap(map -> map.keySet().stream())
                .filter(targetInstantiationLevelId::equals)
                .findAny()
                .orElseThrow(() -> new InvalidInputException(String.format(LEVEL_ID_NOT_PRESENT_IN_VNFD, targetInstantiationLevelId))); // NOSONAR
    }

    public Map<String, InstantiationLevels> getInstantiationLevelsFromPolicies(final VnfInstance vnfInstance) {
        Map<String, InstantiationLevels> allInstantiationLevels = new HashMap<>();

        String vnfInstancePolicies = vnfInstance.getPolicies();
        if (StringUtils.isNotEmpty(vnfInstancePolicies)) {
            Policies policies = replicaDetailsMapper.getPoliciesFromVnfInstance(vnfInstance);
            allInstantiationLevels = policies.getAllInstantiationLevels();
        } else {
            LOGGER.warn("Instantiation levels not supported for instance id {}", vnfInstance.getVnfInstanceId());
        }

        return allInstantiationLevels;
    }

    private static void validateInstantiationLevels(final Map<String, InstantiationLevels> allInstantiationLevels) {
        if (allInstantiationLevels.size() > 1) {
            throw new IllegalArgumentException("More than one Instantiation level in VNFD");
        }
    }
}
