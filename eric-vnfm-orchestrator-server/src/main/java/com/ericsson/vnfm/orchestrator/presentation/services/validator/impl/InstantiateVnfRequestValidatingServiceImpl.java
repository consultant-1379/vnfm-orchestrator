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
package com.ericsson.vnfm.orchestrator.presentation.services.validator.impl;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Errors.NAMESPACE_VALIDATION_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.AT_LEAST_ONE_ALPHABET_CHARACTER_REGEX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE_NAME_MAX_LENGTH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE_NAME_MIN_LENGTH;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE_VALUE_REGEX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.InstantiateVnfRequestValidatingService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.networkdatatypes.NetworkDataTypeValidationService;
import com.ericsson.vnfm.orchestrator.utils.BooleanUtils;

@Service
public class InstantiateVnfRequestValidatingServiceImpl implements InstantiateVnfRequestValidatingService {
    private static final Pattern NAMESPACE_VALUE_VALIDATION_PATTERN = Pattern.compile(NAMESPACE_VALUE_REGEX);
    private static final Pattern AT_LEAST_ONE_ALPHABET_CHARACTER_PATTERN = Pattern.compile(AT_LEAST_ONE_ALPHABET_CHARACTER_REGEX);

    @Autowired
    private NetworkDataTypeValidationService networkDataTypeValidationService;

    /**
     * The following rule is used for validation:
     * "The target size for VNF instantiation may be specified in either instantiationLevelId or targetScaleLevelInfo,
     * but not both. If none of the two attributes (instantiationLevelId or targetScaleLevelInfo) are present,
     * the default instantiation level as declared in the VNFD shall be used"
     *
     * @param instantiateVnfRequest InstantiateVnfRequest object
     */
    public void validateScaleLevelInfo(InstantiateVnfRequest instantiateVnfRequest) {
        String instantiationLevelId = instantiateVnfRequest.getInstantiationLevelId();
        List<ScaleInfo> targetScaleLevelInfo = instantiateVnfRequest.getTargetScaleLevelInfo();

        if (StringUtils.isNotEmpty(instantiationLevelId) && CollectionUtils.isNotEmpty(targetScaleLevelInfo)) {
            throw new InvalidInputException("Instantiate scale level must be specified either " +
                    "by \"instantiationLevelId\" param or by \"targetScaleLevelInfo\". " +
                    "You cannot use both at the same time.");
        }
    }

    public void validateTimeouts(final Map<?, ?> additionalParams) {
        if (MapUtils.isNotEmpty(additionalParams)) {
            List<String> timeouts = List.of(APPLICATION_TIME_OUT);
            List<String> timeOutErrors = verifyTimeouts(additionalParams, timeouts);

            if (CollectionUtils.isNotEmpty(timeOutErrors)) {
                throw new InvalidInputException(
                        String.format("Invalid timeout value specified for %s", timeOutErrors));
            }
        }
    }

    @Override
    public void validateNetworkDataTypes(final String vnfd, final InstantiateVnfRequest instantiateVnfRequest) {
        networkDataTypeValidationService.validate(vnfd, instantiateVnfRequest.getExtVirtualLinks());
    }

    @Override
    public void validateNamespace(final Map<?, ?> additionalParams) {
        if (MapUtils.isEmpty(additionalParams) || additionalParams.get(NAMESPACE) == null
                || StringUtils.isEmpty(additionalParams.get(NAMESPACE).toString())) {
            return;
        }

        String namespace = additionalParams.get(NAMESPACE).toString();

        List<String> validationErrors = new ArrayList<>();

        if (StringUtils.length(namespace) > NAMESPACE_NAME_MAX_LENGTH) {
            validationErrors.add(String.format("Namespace value must not be longer than %s characters", NAMESPACE_NAME_MAX_LENGTH));
        }
        if (StringUtils.length(namespace) < NAMESPACE_NAME_MIN_LENGTH) {
            validationErrors.add(String.format("Namespace value must contain at least %s characters", NAMESPACE_NAME_MIN_LENGTH));
        }
        if (!NAMESPACE_VALUE_VALIDATION_PATTERN.matcher(namespace).matches()) {
            validationErrors.add(NAMESPACE_VALIDATION_ERROR_MESSAGE);
        }
        if (!AT_LEAST_ONE_ALPHABET_CHARACTER_PATTERN.matcher(namespace).matches()) {
            validationErrors.add("Namespace value must consist of at least one alphabet character");
        }

        if (!validationErrors.isEmpty()) {
            throw new InvalidInputException(String.join(";", validationErrors));
        }
    }

    @Override
    public void validateSkipVerification(final Map<?, ?> additionalParams) {
        if (MapUtils.isEmpty(additionalParams)
            || additionalParams.get(SKIP_VERIFICATION) == null
            || additionalParams.get(ADD_NODE_TO_OSS) == null
            || additionalParams.get(CMP_V2_ENROLLMENT) == null) {
            return;
        }

        boolean isSkipVerificationEnabled = BooleanUtils.getBooleanValue(additionalParams.get(SKIP_VERIFICATION));
        boolean isAddNodeToOssEnabled = BooleanUtils.getBooleanValue(additionalParams.get(ADD_NODE_TO_OSS));
        boolean isCMPv2EnrollmentEnabled = BooleanUtils.getBooleanValue(additionalParams.get(CMP_V2_ENROLLMENT));

        if (isAddNodeToOssEnabled && isSkipVerificationEnabled && isCMPv2EnrollmentEnabled) {
            throw new InvalidInputException(
                    "Both parameters 'skipVerification' and 'CMPv2Enrollment' cannot be set true when 'addNodeToOss' is set true");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> verifyTimeouts(final Map<?, ?> additionalParams, final List<String> timeouts) {
        return timeouts.stream()
                .filter(timeout -> {
                    Optional<String> additionalParamsTimeout = (Optional<String>) additionalParams.keySet().stream()
                            .filter(key -> key.toString().equalsIgnoreCase(timeout))
                            .findAny();
                    return additionalParamsTimeout
                            .filter(s -> !StringUtils.isNumeric(String.valueOf(additionalParams.get(s))))
                            .isPresent();
                })
                .collect(Collectors.toList());
    }
}
