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
package com.ericsson.vnfm.orchestrator.presentation.interceptors.drac;

import static java.lang.String.format;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.GET_PACKAGE_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.GET_PACKAGE_INFO_CONTENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.DRAC_DISABLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Messages.EMPTY_NODE_TYPE;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.drac.DracService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.utils.Utility;

import lombok.extern.slf4j.Slf4j;

/**
 * A component that checks if user is authorized to perform change VNF package operation taking into account Domain Role Access Control
 * configuration.
 * <p>
 * It analyzes {@link ChangeCurrentVnfPkgRequest} passed
 * to {@link VnfInstancesControllerImpl#changeVnfPkgForVnfInstanceById(String, String, String, String, ChangeCurrentVnfPkgRequest)}
 * or {@link VnfInstancesControllerImpl#changeVnfPkgForVnfInstanceById(String, String, String, String, MultipartFile, ChangeCurrentVnfPkgRequest)}
 * as body.
 */
@ControllerAdvice
@Slf4j
public class DracChangeVnfPkgRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final String CHANGE_VNF_PKG_METHOD_NAME = "changeVnfPkgForVnfInstanceById";

    private final PackageService packageService;
    private final DracService dracService;

    public DracChangeVnfPkgRequestBodyAdvice(final PackageService packageService,
                                             final DracService dracService) {
        this.packageService = packageService;
        this.dracService = dracService;
    }

    @Override
    public boolean supports(final MethodParameter methodParameter,
                            final Type targetType,
                            final Class<? extends HttpMessageConverter<?>> converterType) {

        return isVnfInstanceController(methodParameter) && isChangeVnfPackageMethodName(methodParameter);
    }

    @Override
    public Object afterBodyRead(final Object body,
                                final HttpInputMessage inputMessage,
                                final MethodParameter parameter,
                                final Type targetType,
                                final Class<? extends HttpMessageConverter<?>> converterType) {

        checkUserRoleIfNecessary(body);

        return body;
    }

    private static boolean isVnfInstanceController(final MethodParameter methodParameter) {
        return methodParameter.getContainingClass() == VnfInstancesControllerImpl.class;
    }

    private static boolean isChangeVnfPackageMethodName(final MethodParameter methodParameter) {
        final Method method = methodParameter.getMethod();
        final String methodName = method != null ? method.getName() : null;

        return Objects.equals(methodName, CHANGE_VNF_PKG_METHOD_NAME);
    }

    private void checkUserRoleIfNecessary(final Object body) {
        if (!dracService.isEnabled()) {
            LOGGER.debug(DRAC_DISABLED);
            return;
        }

        ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest;

        if (body instanceof ChangeCurrentVnfPkgRequest) {
            changeCurrentVnfPkgRequest = (ChangeCurrentVnfPkgRequest) body;
        } else {
            changeCurrentVnfPkgRequest = Utility.parseJsonIgnoreUnknown(body.toString(), ChangeCurrentVnfPkgRequest.class);
        }

        final var nodeType = lookupNodeType(changeCurrentVnfPkgRequest);
        if (nodeType.isEmpty()) {
            LOGGER.debug(EMPTY_NODE_TYPE);
            return;
        }

        dracService.checkPermissionForNodeType(nodeType.get());
    }

    private Optional<String> lookupNodeType(final ChangeCurrentVnfPkgRequest changeCurrentVnfPkgRequest) {
        try {
            final PackageResponse packageInfo = packageService.getPackageInfo(changeCurrentVnfPkgRequest.getVnfdId());
            LOGGER.debug(format(GET_PACKAGE_INFO_CONTENT, packageInfo.toString()));
            return Optional.ofNullable(packageInfo.getVnfProductName());
        } catch (final Exception e) {
            LOGGER.warn(format(GET_PACKAGE_ERROR_MESSAGE, changeCurrentVnfPkgRequest.getVnfdId()), e);
            return Optional.empty();
        }
    }
}
