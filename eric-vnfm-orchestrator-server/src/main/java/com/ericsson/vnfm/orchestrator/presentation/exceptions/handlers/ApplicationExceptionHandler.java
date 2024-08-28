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
package com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PRECONDITION_FAILED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.presentation.exceptions.handlers.ExceptionHandlersUtils.getInstanceUri;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ericsson.am.shared.vnfd.exception.ScalingInfoValidationException;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AlreadyInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AuthenticationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ClusterConfigFileNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ConnectionFailureException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeInfoInstanceIdNotPresentException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradePackageDeletedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DuplicateCombinationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.FileExecutionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.GrantingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpResponseParsingException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpServiceInaccessibleException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.IllegalOperationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidFileException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidOperationStateException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidPaginationQueryException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LastOperationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.LifecycleInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingLicensePermissionException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingMandatoryParameterException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceDeletionInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceValidationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotAddedToOssException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotAuthorizedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationAlreadyInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PodStatusException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ReleaseNameInUseException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RetrieveDataException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.RunningLcmOperationsAmountExceededException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.SSLCertificateException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ServiceUnavailableException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ValidationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfModificationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfdNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler implements AsyncUncaughtExceptionHandler {
    private static final String SCALE_TYPE = "type";
    private static final String TERMINATION_TYPE = "terminationType";
    private static final String DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE = "Downgrade not supported";
    private static final String MALFORMED_REQUEST_ERROR_MESSAGE = "Malformed Request";

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        logThrowable(ex);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        final BindingResult bindingResult = ex.getBindingResult();
        final String errors = "terminateVnfRequest".equalsIgnoreCase(bindingResult.getObjectName()) ?
                collectAllErrors(bindingResult.getAllErrors(), TERMINATION_TYPE, "FORCEFUL, GRACEFUL") :
                collectAllErrors(bindingResult.getAllErrors(), SCALE_TYPE, "SCALE_OUT, SCALE_IN");
        ProblemDetails problemDetails = new ProblemDetails();
        if (bindingResult.hasErrors()) {
            problemDetails.setTitle(BAD_REQUEST.getReasonPhrase());
            problemDetails.setType(URI.create(TYPE_BLANK));
            problemDetails.setStatus(BAD_REQUEST.value());
            problemDetails.setInstance(getInstanceUri(request));
            String type = "scaleVnfRequest".equalsIgnoreCase(bindingResult.getObjectName()) ? SCALE_TYPE : TERMINATION_TYPE;
            problemDetails.setDetail(createErrorMessage(bindingResult.getAllErrors(), errors, type));
        }
        LOGGER.error("Method argument not valid, exceptions occurred: {}", problemDetails.getDetail(), ex);
        return new ResponseEntity<>(problemDetails, BAD_REQUEST);
    }

    private static String collectAllErrors(List<ObjectError> allErrors, String type, String values) {
        return allErrors.stream()
                .filter(e -> type.equalsIgnoreCase(((FieldError) e).getField()))
                .map(e -> values)
                .collect(Collectors.joining(", ", "value not supported - Supported values : ", ""));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders headers,
                                                                     HttpStatusCode status, WebRequest request) {
        ResponseEntity<ProblemDetails> problemDetails = createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
        return new ResponseEntity<>(problemDetails.getBody(), problemDetails.getHeaders(), problemDetails.getStatusCode());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, WebRequest request) {
        ResponseEntity<ProblemDetails> problemDetails = createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
        return new ResponseEntity<>(problemDetails.getBody(), problemDetails.getStatusCode());
    }

    private static String createErrorMessage(List<ObjectError> allError, String error, String type) {
        StringBuilder errorMessage = new StringBuilder();
        for (int i = 0; i < allError.size(); i++) {
            String field = ((FieldError) allError.get(i)).getField();
            errorMessage.append(field).append(" ").
                    append(type.equalsIgnoreCase(field) && error != null ? error :
                                   allError.get(i).getDefaultMessage());
            if (i != (allError.size() - 1)) {
                errorMessage.append(", ");
            }
        }
        return errorMessage.toString();
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ResponseEntity<ProblemDetails> problemDetails = createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
        return new ResponseEntity<>(problemDetails.getBody(), problemDetails.getStatusCode());
    }

    @ExceptionHandler(SSLCertificateException.class)
    public ResponseEntity<ProblemDetails> handleSSLCertificateException(SSLCertificateException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid or missing SSL Certification", BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers,
                                                                     HttpStatusCode status, WebRequest request) {
        ResponseEntity<ProblemDetails> problemDetails = createProblemDetails(request, ex, "Content-Type not supported", NOT_ACCEPTABLE);
        return new ResponseEntity<>(problemDetails.getBody(), problemDetails.getStatusCode());
    }

    @ExceptionHandler(DowngradeNotSupportedException.class)
    public ResponseEntity<ProblemDetails> handleDowngradeNotSupportedException(DowngradeNotSupportedException ex, WebRequest request) {
        return createProblemDetails(request, ex, DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(DowngradeInfoInstanceIdNotPresentException.class)
    public ResponseEntity<ProblemDetails> handleDowngradeInfoInstanceIdNotPresentException(
            DowngradeInfoInstanceIdNotPresentException ex, WebRequest request) {
        return createProblemDetails(request, ex, DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, NOT_FOUND);
    }

    @ExceptionHandler(DowngradePackageDeletedException.class)
    public ResponseEntity<ProblemDetails> handleDowngradePackageDeletedException(DowngradePackageDeletedException ex, WebRequest request) {
        return createProblemDetails(request, ex, DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, PRECONDITION_FAILED);
    }

    @ExceptionHandler(UnprocessablePackageException.class)
    public ResponseEntity<ProblemDetails> handleUnprocessablePackageException(UnprocessablePackageException ex, WebRequest request) {
        return createProblemDetails(request, ex, "VNF package is unprocessable with provided VNFD ID", UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(PackageDetailsNotFoundException.class)
    public ResponseEntity<ProblemDetails> handlePackageDetailsNotFoundException(PackageDetailsNotFoundException ex, WebRequest request) {
        return createProblemDetails(request, ex, "VNF Package not uploaded for the provided VNFD ID", PRECONDITION_FAILED);
    }

    @ExceptionHandler(VnfdNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleVnfdNotFoundException(VnfdNotFoundException ex, WebRequest request) {
        return createProblemDetails(request, ex, "VNFD is not found for provided VNF Package", NOT_FOUND);
    }

    @ExceptionHandler(ScalingInfoValidationException.class)
    public ResponseEntity<ProblemDetails> handleScalingInfoValidationException(ScalingInfoValidationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Scaling Info", PRECONDITION_FAILED);
    }

    @ExceptionHandler(LifecycleInProgressException.class)
    public ResponseEntity<ProblemDetails> handleLifecycleInProgressException(LifecycleInProgressException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Lifecycle operation is in progress for the provided instance id", PRECONDITION_FAILED);
    }

    @ExceptionHandler(InternalRuntimeException.class)
    public ResponseEntity<ProblemDetails> handleInternalRuntimeException(InternalRuntimeException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Bad request", BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetails> handleNotFoundException(final NotFoundException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Not Found Exception", NOT_FOUND);
    }

    @ExceptionHandler(AlreadyInstantiatedException.class)
    public ResponseEntity<ProblemDetails> handleAlreadyInstantiatedException(final AlreadyInstantiatedException ex, WebRequest request) {
        return createProblemDetails(request, ex, "The resource is already in the INSTANTIATED state", CONFLICT);
    }

    @ExceptionHandler(OperationAlreadyInProgressException.class)
    public ResponseEntity<ProblemDetails> handleOperationAlreadyInProgressException(final OperationAlreadyInProgressException ex,
                                                                                    WebRequest request) {
        return createProblemDetails(request, ex, "VNF instance is already being processed", CONFLICT);
    }

    @ExceptionHandler(NamespaceDeletionInProgressException.class)
    public ResponseEntity<ProblemDetails> handleNamespaceDeletionInProgressException(final NamespaceDeletionInProgressException ex,
                                                                                     WebRequest request) {
        return createProblemDetails(request, ex, "This namespace is restricted as it is marked for deletion", CONFLICT);
    }

    @ExceptionHandler(NotInstantiatedException.class)
    public ResponseEntity<ProblemDetails> handleNotInstantiatedException(final NotInstantiatedException ex, WebRequest request) {
        return createProblemDetails(request, ex, "This resource is not in the INSTANTIATED state", CONFLICT);
    }

    @ExceptionHandler(PodStatusException.class)
    public ResponseEntity<ProblemDetails> handlePodStatusException(final PodStatusException ex, WebRequest request) {
        return createProblemDetails(request, ex, "The Pod Status operation request failed", ex.getStatus());
    }

    @ExceptionHandler(NamespaceValidationException.class)
    public ResponseEntity<ProblemDetails> handleNamespaceValidation(final NamespaceValidationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Namespace Validation request failed", ex.getStatus());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ProblemDetails> handleUnsupportedOperationException(final UnsupportedOperationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Unsupported Operation Exception", BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetails> handleIllegalStateException(final IllegalStateException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Illegal State Exception", CONFLICT);
    }

    @ExceptionHandler(MissingMandatoryParameterException.class)
    public ResponseEntity<ProblemDetails> handleIllegalStateException(final MissingMandatoryParameterException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Mandatory parameter missing", UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetails> handleIllegalArgumentException(final IllegalArgumentException ex, WebRequest request) {
        return createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ProblemDetails> handleInvalidInputException(final InvalidInputException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Input Exception", BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateCombinationException.class)
    public ResponseEntity<ProblemDetails> handleDuplicateCombinationException(final DuplicateCombinationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Duplicate releaseName with same target cluster server and namespace found", CONFLICT);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ProblemDetails> handleInvalidFileException(final InvalidFileException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Values File", BAD_REQUEST);
    }

    @ExceptionHandler(FileExecutionException.class)
    public ResponseEntity<ProblemDetails> handleInvalidFileException(final FileExecutionException ex, WebRequest request) {
        return createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
    }

    @ExceptionHandler(NotAddedToOssException.class)
    public ResponseEntity<ProblemDetails> handleNotAddedToOssException(final NotAddedToOssException ex, WebRequest request) {
        return createProblemDetails(request, ex, "VNF Instance has not been added to OSS", CONFLICT);
    }

    @ExceptionHandler(ReleaseNameInUseException.class)
    public ResponseEntity<ProblemDetails> handleReleaseNameInUseException(final ReleaseNameInUseException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Clean up of Resources cannot be performed", BAD_REQUEST);
    }

    @ExceptionHandler(LastOperationException.class)
    public ResponseEntity<ProblemDetails> handleReleaseNameInUseException(final LastOperationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Clean up of Resources cannot be performed", BAD_REQUEST);
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<ProblemDetails> handleIllegalOperationException(final IllegalOperationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Operation not present in VNFD", PRECONDITION_FAILED);
    }

    @ExceptionHandler(ConnectionFailureException.class)
    public ResponseEntity<ProblemDetails> handleConnectionFailureException(final ConnectionFailureException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Connection to ENM has failed", BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetails> handleAuthenticationException(final AuthenticationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Connection to ENM has failed", UNAUTHORIZED);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(ValidationException ex, WebRequest request) {
        return createProblemDetails(request, ex, ex.getTitle(), ex.getResponseHttpStatus());
    }

    @ExceptionHandler(HttpServiceInaccessibleException.class)
    public ResponseEntity<ProblemDetails> handleHttpServiceInaccessibleException(final HttpServiceInaccessibleException ex, WebRequest request) {
        return createProblemDetails(request, ex, SERVICE_UNAVAILABLE.getReasonPhrase(), SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(HttpResponseParsingException.class)
    public ResponseEntity<ProblemDetails> handleHttpResponseParsingException(final HttpResponseParsingException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Bad request", BAD_REQUEST);
    }

    @ExceptionHandler(HttpClientException.class)
    public ResponseEntity<ProblemDetails> handleHttpClientException(final HttpClientException ex, WebRequest request) {
        return createProblemDetails(request, ex, MALFORMED_REQUEST_ERROR_MESSAGE, BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOperationStateException.class)
    public ResponseEntity<ProblemDetails> handleInvalidOperationStateException(final InvalidOperationStateException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Operation State", UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<ProblemDetails> handleCryptoException(final CryptoException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Crypto Service call failed", BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetails> handleDataAccessExceptionException(final DataAccessException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Internal Database error occurred", BAD_REQUEST);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ProblemDetails> handleServiceUnavailableException(final ServiceUnavailableException ex, WebRequest request) {
        return createProblemDetails(request, ex, String.format("%s Service call failed", ex.getService()), BAD_REQUEST);
    }

    @ExceptionHandler(InvalidHealRequestException.class)
    public ResponseEntity<ProblemDetails> handleInvalidInputException(final InvalidHealRequestException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Heal Request Exception", BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPaginationQueryException.class)
    public ResponseEntity<ProblemDetails> handleInvalidPaginationQueryException(final InvalidPaginationQueryException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Pagination Query Parameter Exception", BAD_REQUEST);
    }

    @ExceptionHandler(VnfModificationException.class)
    public ResponseEntity<ProblemDetails> handleVnfModificationException(final VnfModificationException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Invalid Vnf Modification  Exception", BAD_REQUEST);
    }

    @ExceptionHandler(GrantingException.class)
    public ResponseEntity<ProblemDetails> handleGrantingException(final GrantingException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Granting Request wasn't confirmed by NFVO", BAD_REQUEST);
    }

    @ExceptionHandler(MissingLicensePermissionException.class)
    public ResponseEntity<ProblemDetails> handleMissingLicensePermissionException(final MissingLicensePermissionException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Missing License Permission Exception", METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ProblemDetails> handleNotAuthorizedException(final NotAuthorizedException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Not Authorized Exception", UNAUTHORIZED);
    }

    @ExceptionHandler(ClusterConfigFileNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleClusterConfigFileNotFoundException(ClusterConfigFileNotFoundException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Cluster Config File Not Found Exception", PRECONDITION_FAILED);
    }

    @ExceptionHandler(RetrieveDataException.class)
    public ResponseEntity<ProblemDetails> handleRetrieveDataException(final RetrieveDataException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Failed during data retrieving", BAD_REQUEST);
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    public ResponseEntity<ProblemDetails> handleOperationNotSupportedException(final OperationNotSupportedException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Operation Not Supported Exception", BAD_REQUEST);
    }

    @ExceptionHandler(RunningLcmOperationsAmountExceededException.class)
    public ResponseEntity<ProblemDetails> handleRunningLcmOperationsAmountExceededException(
            final RunningLcmOperationsAmountExceededException ex, WebRequest request) {
        return createProblemDetails(request, ex, "Limit of concurrent LCM operations is reached", TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException ex, HttpServletRequest request) {
        LOGGER.warn("ClientAbortException generated by request {} from remote address {}",
                     request.getRequestURL(), request.getRemoteAddr());
    }

    private static ResponseEntity<ProblemDetails> createProblemDetails(final WebRequest request,
                                                                       final Throwable throwable,
                                                                       final String title,
                                                                       final HttpStatus httpStatus) {
        logThrowable(throwable);

        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setTitle(title);
        problemDetails.setType(URI.create(TYPE_BLANK));
        problemDetails.setStatus(httpStatus.value());
        problemDetails.setInstance(getInstanceUri(request));
        problemDetails.setDetail(throwable.getMessage());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(problemDetails, responseHeaders, httpStatus);
    }

    private static void logThrowable(final Throwable throwable) {
        LOGGER.error("Error occurred: {}", throwable.getMessage(), throwable);
    }
}
