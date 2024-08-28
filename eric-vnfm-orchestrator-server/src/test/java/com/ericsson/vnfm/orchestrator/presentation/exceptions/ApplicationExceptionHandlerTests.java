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
package com.ericsson.vnfm.orchestrator.presentation.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.am.shared.vnfd.exception.ScalingInfoValidationException;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.license.Permission;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.controllers.filter.IdempotencyFilter;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.NotificationService;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfLcmOperationService;
import com.ericsson.vnfm.orchestrator.presentation.services.backups.BackupsService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.JwtDecoder;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.drac.DracService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.LicenseConsumerService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.EnmTopologyService;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@WebMvcTest(controllers = VnfInstancesControllerImpl.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class,
            excludeFilters = @ComponentScan.Filter(
                    value = { IdempotencyFilter.class },
                    type = FilterType.ASSIGNABLE_TYPE))
@MockBean(classes = {
        InstanceService.class,
        VnfInstanceMapper.class,
        LifeCycleManagementService.class,
        EnmTopologyService.class,
        OssNodeService.class,
        VnfLcmOperationService.class,
        BackupsService.class,
        DatabaseInteractionService.class,
        UsernameCalculationService.class,
        NotificationService.class,
        JwtDecoder.class,
        VnfInstanceRepository.class,
        ClusterConfigFileRepository.class})
public class ApplicationExceptionHandlerTests {

    private static final String DB_VNF_VNFDID = "d3def1ce-4cf4-477c-aab3-21cb04e6a378";
    private static final String SAMPLE_VNF_DESCRIPTION = "create-vnf-instance-test-description";
    private static final String SAMPLE_VNF_NAME = "create-vnf-instance-test";
    private static final String CREATE_VNF_INSTANCE_URL = REST_URL + REST_URL_VNFS;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private VnfInstancesControllerImpl vnfInstancesController;

    @MockBean
    private LicenseConsumerService licenseConsumerService;

    @MockBean
    private DracService dracService;

    @MockBean
    private PackageService packageService;

    @BeforeEach
    public void setUp() {
        when(licenseConsumerService.getPermissions()).thenReturn(EnumSet.allOf(Permission.class));
        when(dracService.isEnabled()).thenReturn(false);
    }

    @Test
    public void shouldHandleSSLCertificateException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new SSLCertificateException("test")).when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Invalid or missing SSL Certification");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleHttpMessageNotReadable() throws Exception {
        // when
        final var mvcResult = postMvcResult("{");

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Malformed Request");
        assertThat(problemDetails.getDetail()).contains("JSON parse error");
    }

    @Test
    public void shouldHandleHttpMediaTypeNotSupported() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        // when
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(CREATE_VNF_INSTANCE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_ATOM_XML)
                .content(mapper.writeValueAsString(createVnfRequest));

        final var mvcResult = mockMvc
                .perform(requestBuilder)
                .andReturn();

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(406);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Content-Type not supported");
        assertThat(problemDetails.getDetail()).contains("Content-Type 'application/atom+xml' is not supported");
    }

    @Test
    public void shouldHandleInternalRuntimeException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new InternalRuntimeException("test")).when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Bad request");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleUnsupportedOperationException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new UnsupportedOperationException("test")).when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Unsupported Operation Exception");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleDuplicateCombinationException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new DuplicateCombinationException("Duplicate combination of resource instance name, target cluster server and namespace."))
                .when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(409);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Duplicate releaseName with same target cluster server and namespace found");
        assertThat(problemDetails.getDetail()).contains("Duplicate combination of resource instance name, target cluster server and namespace.");
    }

    @Test
    public void shouldHandleConnectionFailureException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new ConnectionFailureException("test")).when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Connection to ENM has failed");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleAuthenticationException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new AuthenticationException("test")).when(vnfInstancesController).createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(401);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Connection to ENM has failed");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleHttpServiceInaccessibleException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new HttpServiceInaccessibleException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleHttpResponseParsingException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new HttpResponseParsingException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Bad request");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleHttpClientException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new HttpClientException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Malformed Request");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleCryptoException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new CryptoException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Crypto Service call failed");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandlePackageDetailsNotFoundException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new PackageDetailsNotFoundException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(412);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("VNF Package not uploaded for the provided VNFD ID");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleVnfdNotFoundException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new VnfdNotFoundException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(404);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("VNFD is not found for provided VNF Package");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleScalingInfoValidationException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new ScalingInfoValidationException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(412);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Invalid Scaling Info");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleNamespaceValidationException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new NamespaceValidationException("test", new RuntimeException(), HttpStatus.BAD_REQUEST)).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Namespace Validation request failed");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleDataAccessException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new JpaSystemException(new RuntimeException("test"))).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Internal Database error occurred");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleServiceUnavailableException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new ServiceUnavailableException("nfvo", "test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("nfvo Service call failed");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleGrantingException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new GrantingException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Granting Request wasn't confirmed by NFVO");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleClusterConfigFileNotFoundException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new ClusterConfigFileNotFoundException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(412);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Cluster Config File Not Found Exception");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleRetrieveDataException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new RetrieveDataException("test")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Failed during data retrieving");
        assertThat(problemDetails.getDetail()).contains("test");
    }

    @Test
    public void shouldHandleOperationNotSupportedException() throws Exception {
        // given
        final var createVnfRequest = createCreateVnfRequest();

        doThrow(new OperationNotSupportedException("new-operation", "package-id", "not supported yet")).when(vnfInstancesController)
                .createVnfInstance(anyString(), anyString(), anyString(), any());

        // when
        final var mvcResult = postMvcResult(mapper.writeValueAsString(createVnfRequest));

        // then
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);

        final var problemDetails = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProblemDetails.class);
        assertThat(problemDetails.getTitle()).contains("Operation Not Supported Exception");
        assertThat(problemDetails.getDetail())
                .contains("Operation new-operation is not supported for package package-id due to cause: not supported yet");
    }

    private static CreateVnfRequest createCreateVnfRequest() {
        CreateVnfRequest createVnfRequest = new CreateVnfRequest();
        createVnfRequest.setVnfdId(DB_VNF_VNFDID);
        createVnfRequest.setVnfInstanceName(SAMPLE_VNF_NAME);
        createVnfRequest.setVnfInstanceDescription(SAMPLE_VNF_DESCRIPTION);
        return createVnfRequest;
    }

    private MvcResult postMvcResult(String stringToPost) throws Exception {
        final RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(CREATE_VNF_INSTANCE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, "dummyKey")
                .content(stringToPost);

        return mockMvc
                .perform(requestBuilder)
                .andReturn();
    }
}
