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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.license;

import static com.ericsson.vnfm.orchestrator.model.license.Permission.ENM_INTEGRATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ALLOWED_NUMBER_OF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.ENM_INTEGRATION_LICENSE_TYPE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.INSTANTIATE_VNF_REQUEST_PARAM;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.utils.Utility.checkAndCastObjectToMap;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A component that checks ENM integration license permission for Instantiate operation if it requires interaction with ENM.
 * <p>
 * It analyzes {@link InstantiateVnfRequest} passed to:
 * <ul>
 * <li>{@link VnfInstancesControllerImpl#instantiateVnfInstance(String, String, String, String, InstantiateVnfRequest)} as body</li>
 * <li>{@link VnfInstancesControllerImpl#instantiateVnfInstance(String, String, String, String, MultipartFile, InstantiateVnfRequest)}
 * as multipart request part.</li>
 * </ul>
 * <p>
 * If additional parameters has {@link com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request#ADD_NODE_TO_OSS}=true
 * then instantiate requires ENM
 * integration.
 */
@ControllerAdvice
@Slf4j
public class EnmIntegrationLicenseInstantiateRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final ObjectMapper mapper;

    private final LicenseConsumerService licenseConsumerService;

    private final VnfInstanceRepository vnfInstanceRepository;

    private final boolean restrictedMode;

    public EnmIntegrationLicenseInstantiateRequestBodyAdvice(final ObjectMapper mapper,
                                                             final LicenseConsumerService licenseConsumerService,
                                                             @Value("${orchestrator.restrictedMode:true}") final boolean restrictedMode,
                                                             final VnfInstanceRepository vnfInstanceRepository) {
        this.mapper = mapper;
        this.licenseConsumerService = licenseConsumerService;
        this.restrictedMode = restrictedMode;
        this.vnfInstanceRepository = vnfInstanceRepository;
    }

    @Override
    public boolean supports(final MethodParameter methodParameter,
                            final Type targetType,
                            final Class<? extends HttpMessageConverter<?>> converterType) {

        return isVnfInstanceController(methodParameter) && isInstantiateVnfRequestParameter(methodParameter, targetType);
    }

    @Override
    public Object afterBodyRead(final Object body,
                                final HttpInputMessage inputMessage,
                                final MethodParameter parameter,
                                final Type targetType,
                                final Class<? extends HttpMessageConverter<?>> converterType) {

        checkEnmIntegrationPermissionIfNecessary(body);

        return body;
    }

    private static boolean isVnfInstanceController(final MethodParameter methodParameter) {
        return methodParameter.getContainingClass() == VnfInstancesControllerImpl.class;
    }

    private static boolean isInstantiateVnfRequestParameter(final MethodParameter methodParameter, final Type targetType) {
        return isRequestBodyParameter(targetType) || isRequestPartParameter(methodParameter);
    }

    private static boolean isRequestBodyParameter(final Type targetType) {
        return targetType.getTypeName().equals(InstantiateVnfRequest.class.getTypeName());
    }

    private static boolean isRequestPartParameter(final MethodParameter methodParameter) {
        final var requestPartAnnotation = methodParameter.getParameterAnnotation(RequestPart.class);
        if (requestPartAnnotation == null) {
            return false;
        }

        return Objects.equals(requestPartAnnotation.value(), INSTANTIATE_VNF_REQUEST_PARAM);
    }

    private void checkEnmIntegrationPermissionIfNecessary(final Object body) {
        if (restrictedMode && isEnmIntegrationRequest(body)
                && !licenseConsumerService.getPermissions().contains(ENM_INTEGRATION)) {
            int numberOfPackages = (int) vnfInstanceRepository.count();

            if (numberOfPackages > ALLOWED_NUMBER_OF_INSTANCES) {
                throw new MissingLicensePermissionException(String.format(ILLEGAL_NUMBER_OF_INSTANCES_ERROR_MESSAGE, ENM_INTEGRATION_LICENSE_TYPE));
            }
        }
    }

    private boolean isEnmIntegrationRequest(final Object body) {
        return getInstantiateVnfRequest(body)
                .map(request -> checkAndCastObjectToMap(request.getAdditionalParams()))
                .map(additionalParams -> additionalParams.get(ADD_NODE_TO_OSS))
                .map(addNodeToOss -> BooleanUtils.toBoolean(addNodeToOss.toString()))
                .orElse(false);
    }

    private Optional<InstantiateVnfRequest> getInstantiateVnfRequest(final Object body) {
        if (body instanceof InstantiateVnfRequest) {
            return Optional.of((InstantiateVnfRequest) body);
        } else if (body instanceof String) {
            return deserializeInstantiateRequest((String) body);
        }

        LOGGER.warn("Unknown InstantiateVnfRequest type {}", body.getClass().getTypeName());

        return Optional.empty();
    }

    private Optional<InstantiateVnfRequest> deserializeInstantiateRequest(final String body) {
        try {
            return Optional.of(mapper.readValue(body, InstantiateVnfRequest.class));
        } catch (final JsonProcessingException e) {
            LOGGER.error("Error occurred while deserializing Instantiate request", e);

            return Optional.empty();
        }
    }
}
