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
package com.ericsson.vnfm.orchestrator.presentation.services.packageing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.ONBOARDING_LOCAL_PARAM_NAME;
import static com.ericsson.vnfm.orchestrator.TestUtils.ONBOARDING_PATH;
import static com.ericsson.vnfm.orchestrator.TestUtils.ONBOARDING_QUERY_VALUE;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.PACKAGE_NOT_FOUND_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.ericsson.vnfm.orchestrator.utils.OnboardingUtility.createOnboardingConfig;
import static com.ericsson.vnfm.orchestrator.utils.OnboardingUtility.createOnboardingRetryTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.amonboardingservice.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.messaging.OnboardingHealth;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.ericsson.vnfm.orchestrator.routing.onboarding.EvnfmOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.NfvoOnboardingRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClientImpl;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;


@SpringBootTest(classes = {
        PackageServiceImpl.class,
        NfvoConfig.class,
        OnboardingClientImpl.class,
        EvnfmOnboardingRoutingClient.class,
        NfvoOnboardingRoutingClient.class,
        OnboardingUriProvider.class,
        OnboardingConfig.class,
        RestTemplate.class,
        ObjectMapper.class,
        OkHttpClient.class })
@EnableConfigurationProperties
public class PackageServiceTest {
    private static final String DUMMY_DESCRIPTOR_ID = "test";
    private static final String NFVO_PATH = "/ecm_service/SOL003/vnfpkgm/v1/vnf_packages";
    private static final String NFVO_QUERY_VALUE = "vnfdId.eq=%s&softwareImages.containerFormat.eq=DOCKER&allTenants.eq=true";

    @Autowired
    private PackageService packageService;

    @Autowired
    private NfvoConfig nfvoConfig;

    @Autowired
    private OnboardingClient onboardingClient;

    @Autowired
    private OnboardingUriProvider onboardingUriProvider;

    @Autowired
    @Qualifier("evnfmOnboardingRoutingClient")
    private OnboardingRoutingClient evnfmOnboardingRoutingClient;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OnboardingHealth onboardingHealth;

    @MockBean
    @Qualifier("nfvoRetryTemplate")
    private RetryTemplate retryTemplate;

    private MockWebServer mockWebServer;

    private String onboardingHost;
    private String packageDetails;
    private String packageDetailsNotUnique;
    private String vnfdDetails;
    private String scalingMappingDetails;
    private String packageArtifacts;
    private Map<String, String> chartArtifactKeysToUrls;

    @BeforeEach
    public void init() throws URISyntaxException, JsonProcessingException {
        mockRetryTemplateResult();

        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, "nfvoToken", "token");

        mockWebServer = new MockWebServer();
        onboardingHost = String.valueOf(mockWebServer.url("/"));

        packageDetails = getFile("packages-response-from-nfvo.json");
        packageDetailsNotUnique = getFile("packages-response-not-unique-from-nfvo.json");
        vnfdDetails = getFile("vnfd-response-from-nfvo.json");
        scalingMappingDetails = getFile("scaling-mapping-file-response.json");
        packageArtifacts = getFile("package-artifacts-response.yaml");

        chartArtifactKeysToUrls = Map.of(
                "helm_package_1",
                "https://helm-repository.evnfm01.eccd01.eccd:443/onboarded/charts/spider-app-label-verification-2.193.100.tgz",
                "helm_package_2",
                "https://helm-repository.evnfm01.eccd01.eccd:443/onboarded/charts/spider-app-label-verification-3.193.100.tgz");
    }

    @Test
    public void testGetPackageInfoSmallStackSuccess() throws InterruptedException {
        String expectedPackageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedRequestCount = 1;

        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageDetails));

        PackageResponse actualPackage = packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(expectedRequestCount, mockWebServer.getRequestCount());
        assertThat(recordedRequest.getPath()).doesNotStartWith(NFVO_PATH);
        assertThat(recordedRequest.getPath()).startsWith(ONBOARDING_PATH);

        assertEquals(expectedPackageId, actualPackage.getId());
    }

    @Test
    public void testGetPackageInfoFullstackSuccess() throws InterruptedException {
        String expectedPackageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedRequestCount = 2;

        mockFullStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageDetails));
        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, vnfdDetails));

        PackageResponse packageInfo = packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(expectedRequestCount);
        assertThat(recordedRequest.getPath()).doesNotStartWith(ONBOARDING_PATH);
        assertThat(recordedRequest.getPath()).startsWith(NFVO_PATH);

        assertThat(packageInfo.getId()).isEqualTo(expectedPackageId);
        packageInfo.getHelmPackageUrls().forEach(helmPackage ->
                                                         assertThat(chartArtifactKeysToUrls).containsEntry(helmPackage.getChartArtifactKey(),
                                                                                                           helmPackage.getChartUrl()));
    }

    @Test
    public void testGetPackageInfoSmallStackFailedPackageNotUnique() {
        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageDetailsNotUnique));

        assertThatThrownBy(() -> packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID))
                .isInstanceOf(UnprocessablePackageException.class)
                .hasMessageContaining(String.format("Only one package is expected to have vnfdId %s", DUMMY_DESCRIPTOR_ID));
    }

    @Test
    public void testGetPackageInfoSmallStackFailedPackagesEmpty() {
        String packageDetailsEmpty = "[]";
        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageDetailsEmpty));

        assertThatThrownBy(() -> packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID))
                .isInstanceOf(UnprocessablePackageException.class)
                .hasMessageContaining(String.format("Package not found with VNFD ID %s", DUMMY_DESCRIPTOR_ID));
    }

    @Test
    public void testGetScalingMappingSmallStackSuccess() {
        String vnfPackageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedScalingMappingSize = 6;
        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, scalingMappingDetails));

        Map<String, ScaleMapping> scalingMapping = packageService.getScalingMapping(vnfPackageId,
                                                                                          "/Definitions/OtherTemplates/scaling_mapping.yaml");

        assertThat(scalingMapping).isNotNull()
                .hasSize(expectedScalingMappingSize);
    }

    @Test
    public void testGetPackageInfoFullstackWithRetrySuccess() {
        String expectedPackageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedRequestCount = 3;

        mockFullStackConfig();

        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageDetails));
        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, vnfdDetails));

        PackageResponse packageInfo = packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(expectedRequestCount);

        assertThat(packageInfo.getId()).isEqualTo(expectedPackageId);
        packageInfo.getHelmPackageUrls().forEach(helmPackage ->
                                                         assertThat(chartArtifactKeysToUrls).containsEntry(helmPackage.getChartArtifactKey(),
                                                                                                           helmPackage.getChartUrl()));
    }

    @Test
    public void testGetPackageInfoFullstackWithRetryFailedNoResponse() {
        Assertions.assertThrows(InternalRuntimeException.class, () -> {
            mockFullStackConfig();
            int requestCount = 3;

            // enqueue 3 requests without response
            IntStream.range(0, requestCount)
                    .forEach(i -> mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)));

            packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);
        });
    }

    @Test
    public void testGetPackageInfoSmallStackWithRetryFailedInternalServerError() {

        Assertions.assertThrows(InternalRuntimeException.class, () -> {
            int requestCount = 4;

            mockSmallStackConfig();

            // enqueue 4 requests with internal error as response from Onboarding
            IntStream.range(0, requestCount)
                    .forEach(i -> mockWebServer.enqueue(createMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, StringUtils.EMPTY)));
            when(onboardingHealth.isUp()).thenReturn(true);

            packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);

            fail("Should have thrown an InternalRuntimeException");
        });
    }

    @Test
    public void testGetPackageInfoSmallStackFailedNotFound() {

        Assertions.assertThrows(PackageDetailsNotFoundException.class, () -> {
            mockSmallStackConfig();

            mockWebServer.enqueue(createMockResponse(HttpStatus.NOT_FOUND, getPackageNotFoundErrorMessage()));
            when(onboardingHealth.isUp()).thenReturn(true);

            packageService.getPackageInfo(DUMMY_DESCRIPTOR_ID);

            fail("Should have thrown a PackageDetailsNotFoundException");
        });
    }

    @Test
    public void testGetSupportedOperationsFullstackSuccess() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedSupportedOperationsSize = 9;

        mockFullStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, vnfdDetails));

        List<OperationDetail> actualSupportedOperations = packageService.getSupportedOperations(packageId);

        assertNotNull(actualSupportedOperations);
        assertThat(actualSupportedOperations).hasSize(expectedSupportedOperationsSize);
    }

    @Test
    public void testGetSupportedOperationsSmallstackSuccess() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        int expectedSupportedOperationsSize = 9;

        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, vnfdDetails));

        List<OperationDetail> actualSupportedOperations = packageService.getSupportedOperations(packageId);

        assertNotNull(actualSupportedOperations);
        assertThat(actualSupportedOperations).hasSize(expectedSupportedOperationsSize);
    }

    @Test
    public void testUpdatePackageStateSmallstackSuccess() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";

        mockSmallStackConfig();

        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, StringUtils.EMPTY));

        packageService.updateUsageState(packageId, "43bf1225-81e1-46b4-ae10-cadea4432941", true);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    public void testUpdatePackageStateFullstackSuccessNotExecuted() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";

        mockFullStackConfig();

        packageService.updateUsageState(packageId, "43bf1225-81e1-46b4-ae10-cadea4432941", true);

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    public void testGetPackageArtifactsSmallStackSuccess() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        String artifactsPath = "Definitions/OtherTemplates/package-artifacts-response.yaml";

        mockSmallStackConfig();
        mockWebServer.enqueue(createMockResponse(HttpStatus.OK, packageArtifacts));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .setBody(packageArtifacts));

        Optional<String> actual = packageService.getPackageArtifacts(packageId, artifactsPath);
        assertThat(actual).isPresent();
    }

    @Test
    public void testGetPackageArtifactsSmallStackEmptyResponse() {
        String packageId = "43bf1225-81e1-46b4-ae10-cadea4432939";
        String artifactsPath = "Definitions/OtherTemplates/package-artifacts-response.yaml";

        mockSmallStackConfig();
        when(onboardingHealth.isUp()).thenReturn(true);

        var expectedProblemDetails = new ProblemDetails();
        expectedProblemDetails.setTitle("ArtifactPath not found");
        expectedProblemDetails.setType(URI.create(TYPE_BLANK));
        expectedProblemDetails.setStatus(HttpStatus.NOT_FOUND.value());

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .setBody(expectedProblemDetails.toString()));

        Optional<String> actual = packageService.getPackageArtifacts(packageId, artifactsPath);
        assertThat(actual).isEmpty();
    }

    private void mockRetryTemplateResult() {
        RetryTemplate retryTemplate = createOnboardingRetryTemplate();

        ReflectionTestUtils.setField(evnfmOnboardingRoutingClient, "nfvoRetryTemplate", retryTemplate);
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, "nfvoRetryTemplate", retryTemplate);
    }

    private void mockSmallStackConfig() {
        nfvoConfig.setEnabled(false);
        ReflectionTestUtils.setField(packageService, "nfvoConfig", nfvoConfig);
        OnboardingConfig onboardingConfig = createOnboardingConfig(onboardingHost, ONBOARDING_PATH,
                                                                   ONBOARDING_QUERY_VALUE);
        ReflectionTestUtils.setField(onboardingUriProvider, ONBOARDING_LOCAL_PARAM_NAME, onboardingConfig);
    }

    private void mockFullStackConfig() {
        nfvoConfig.setEnabled(true);
        ReflectionTestUtils.setField(packageService, "nfvoConfig", nfvoConfig);
        OnboardingConfig onboardingConfig = createOnboardingConfig(onboardingHost, NFVO_PATH,
                                                                   NFVO_QUERY_VALUE);
        ReflectionTestUtils.setField(onboardingUriProvider, ONBOARDING_LOCAL_PARAM_NAME, onboardingConfig);
    }

    private String getPackageNotFoundErrorMessage() {
        return "{\"message\": \"" + String.format(PACKAGE_NOT_FOUND_ERROR_MESSAGE,
                                                  PackageServiceTest.DUMMY_DESCRIPTOR_ID) + "\"}";
    }

    private static MockResponse createMockResponse(final HttpStatus httpStatus, String body) {
        return new MockResponse()
                .setResponseCode(httpStatus.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private String getFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }
}
