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
package com.ericsson.vnfm.orchestrator.presentation.controllers.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.IDEMPOTENCY_KEY_HEADER;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CLUSTER_CONFIG;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.REST_URL_VNFS;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ProcessingState;
import com.ericsson.vnfm.orchestrator.model.entity.RequestProcessingDetails;
import com.ericsson.vnfm.orchestrator.presentation.controllers.ClusterConfigController;
import com.ericsson.vnfm.orchestrator.presentation.controllers.VnfInstancesControllerImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.repositories.RequestProcessingDetailsRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;

@AutoConfigureMockMvc
@Slf4j
class IdempotencyFilterTest extends AbstractDbSetupTest {

    private static final String IDEMPOTENCY_HEADER_VALUE = "dummyKey";

    private static final String VNF_INSTANCES_PATH = REST_URL + REST_URL_VNFS;
    private static final String CLUSTER_PATH = REST_URL + CLUSTER_CONFIG;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestProcessingDetailsRepository requestProcessingDetailsRepository;

    @MockBean
    private VnfInstancesControllerImpl vnfInstancesController;

    @MockBean
    private ClusterConfigController clusterConfigController;

    @SpyBean
    private IdempotencyService idempotencyService;

    @AfterEach
    public void cleanup() {
        try {
            requestProcessingDetailsRepository.deleteById(IDEMPOTENCY_HEADER_VALUE);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("Request entry doesn't exist in the DB");
        }
    }

    @Test
    public void testIdempotencyFilterForNewRequest() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        String body = objectMapper.writeValueAsString(request);

        String hash = calculateRequestHash(VNF_INSTANCES_PATH, "POST", body);

        makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);

        Optional<RequestProcessingDetails> detailsOptional = requestProcessingDetailsRepository.findById(IDEMPOTENCY_HEADER_VALUE);

        verify(vnfInstancesController, times(1))
                .createVnfInstance(any(), any(), any(), any());
        assertThat(detailsOptional.isPresent()).isTrue();
        RequestProcessingDetails details = detailsOptional.get();

        assertThat(details.getId()).isEqualTo(IDEMPOTENCY_HEADER_VALUE);
        assertThat(details.getRequestHash()).isEqualTo(hash);
        assertThat(details.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(details.getRetryAfter()).isEqualTo(5);
        assertThat(details.getCreationTime()).isNotNull();
    }

    @Test
    public void testIdempotencyFilterForExistedRequest() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        String body = objectMapper.writeValueAsString(request);

        makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);
        verify(vnfInstancesController, times(1))
                .createVnfInstance(any(), any(), any(), any());

        MvcResult result = makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_EARLY.value());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }

    @Test
    public void testIdempotencyFilterForExistedMultipartRequest() throws Exception {
        String clusterConfigPath = "configs/cluster01.config";
        String description = "Dummy cluster config";

        MvcResult mvcResult = makeMultipartPostRequest(clusterConfigPath, description, CLUSTER_PATH);
        verify(clusterConfigController, times(1))
                .registerClusterConfigFile(anyString(), anyString(), anyString(), any(), any(), any(),
                                           any());

        MvcResult result = makeMultipartPostRequest(clusterConfigPath, description, CLUSTER_PATH);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_EARLY.value());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("3");
    }

    @Test
    public void testFinishedIdempotentRequest() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        String body = objectMapper.writeValueAsString(request);
        String hash = calculateRequestHash(VNF_INSTANCES_PATH, "POST", body);

        RequestProcessingDetails requestProcessingDetails = createDummyRequest(hash, ProcessingState.FINISHED, LocalDateTime.now());

        MvcResult result = makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);
        HttpServletResponse response = result.getResponse();

        verify(vnfInstancesController, times(0))
                .createVnfInstance(any(), any(), any(), any());
        assertThat(response.getStatus()).isEqualTo(requestProcessingDetails.getResponseCode());
        assertThat(response.getHeader("header")).isEqualTo("dummy");
    }

    @Test
    public void testExpiredIdempotentRequest() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        String body = objectMapper.writeValueAsString(request);

        String hash = calculateRequestHash(VNF_INSTANCES_PATH, "POST", body);
        createDummyRequest(hash, ProcessingState.STARTED, LocalDateTime.now().minusSeconds(11));

        makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);
        Optional<RequestProcessingDetails> detailsOptional = requestProcessingDetailsRepository.findById(IDEMPOTENCY_HEADER_VALUE);

        verify(vnfInstancesController, times(1))
                .createVnfInstance(any(), any(), any(), any());
        verify(idempotencyService, times(1))
                .updateProcessingRequestCreationTime(any());
        assertThat(detailsOptional.isPresent()).isTrue();
        RequestProcessingDetails details = detailsOptional.get();

        assertThat(details.getId()).isEqualTo(IDEMPOTENCY_HEADER_VALUE);
        assertThat(details.getRequestHash()).isEqualTo(hash);
        assertThat(details.getProcessingState()).isEqualTo(ProcessingState.STARTED);
        assertThat(details.getRetryAfter()).isEqualTo(5);

        long periodBetween = ChronoUnit.SECONDS.between(LocalDateTime.now(), details.getCreationTime());
        assertThat(periodBetween < 5).isTrue();
    }

    @Test
    public void testIdempotentRequestWithWrongHash() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");

        String body = objectMapper.writeValueAsString(request);

        createDummyRequest("wrongHashValue", ProcessingState.STARTED, LocalDateTime.now());

        MvcResult result = makePostRequest(VNF_INSTANCES_PATH, body, IDEMPOTENCY_HEADER_VALUE);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        verify(vnfInstancesController, times(0)).createVnfInstance(any(), any(), any(), any());
    }

    @Test
    public void testPostRequestWithoutIdempotencyHeader() throws Exception {
        final var request = new CreateVnfRequest();
        request.setVnfdId("vnfd-id");
        request.setVnfInstanceName("instance-name");
        String body = objectMapper.writeValueAsString(request);

        MvcResult result = makePostRequest(VNF_INSTANCES_PATH, body, null);
        HttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        verify(vnfInstancesController, times(0)).createVnfInstance(any(), any(), any(), any());
    }

    private RequestProcessingDetails createDummyRequest(String hash, ProcessingState state, LocalDateTime creationTime) throws Exception {
        RequestProcessingDetails requestProcessingDetails = new RequestProcessingDetails();
        requestProcessingDetails.setId(IDEMPOTENCY_HEADER_VALUE);
        requestProcessingDetails.setRequestHash(hash);
        requestProcessingDetails.setProcessingState(state);
        requestProcessingDetails.setCreationTime(creationTime);
        requestProcessingDetails.setRetryAfter(5);
        requestProcessingDetails.setResponseBody(objectMapper.writeValueAsString("{\"name\": \"test\"}"));
        requestProcessingDetails.setResponseHeaders(objectMapper.writeValueAsString(Map.of("header", List.of("dummy"))));
        requestProcessingDetails.setResponseCode(201);

        requestProcessingDetailsRepository.save(requestProcessingDetails);

        return requestProcessingDetails;
    }


    private MvcResult makePostRequest(final String requestUrl, final String body, String idempotencyKey) throws Exception {

        final var requestBuilder = post(requestUrl)
                .content(body)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        if (idempotencyKey != null) {
            requestBuilder.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }

        return mockMvc.perform(requestBuilder)
                .andReturn();
    }

    private MvcResult makeMultipartPostRequest(String filePath, String description, String requestUrl) throws Exception {
        Resource fileResource = new ClassPathResource(filePath);

        MockMultipartFile configFile = new MockMultipartFile(
                "clusterConfig", fileResource.getFilename(),
                MediaType.MULTIPART_FORM_DATA_VALUE,
                fileResource.getInputStream());

        return mockMvc.perform(MockMvcRequestBuilders
                                       .multipart(requestUrl)
                                       .file(configFile)
                                       .accept(MediaType.APPLICATION_JSON)
                                       .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_HEADER_VALUE)
                                       .param("description", description)
                                       .queryParam("isDefault", "true"))
                .andReturn();
    }

    private static String calculateRequestHash(String url, String method, String body) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(url.getBytes());
        messageDigest.update(method.getBytes());
        messageDigest.update(body.getBytes());
        return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
    }
}