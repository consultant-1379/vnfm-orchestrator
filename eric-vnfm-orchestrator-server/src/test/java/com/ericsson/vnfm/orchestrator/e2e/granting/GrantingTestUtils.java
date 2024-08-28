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
package com.ericsson.vnfm.orchestrator.e2e.granting;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.JSON_FILE_ENDING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;

import lombok.experimental.UtilityClass;
import okhttp3.Credentials;

@UtilityClass
public class GrantingTestUtils {
    static final String CACHE_CONTROL_HEADER = "no-cache";
    static final String TENANT_ID_HEADER = "tenantId";
    static final String APPLICATION_JSON_HEADER = MediaType.APPLICATION_JSON.toString();
    static final String APPLICATION_JSON_HEADER_UTF8 = "application/json; charset=UTF-8";
    static final String SCALING_MAPPING_PATH = "Definitions/OtherTemplates/scaling_mapping.yaml";
    static final String SCALING_MAPPING_PATH_DEPLOYABLE_MODULES = "Definitions/OtherTemplates/43bf1225-81e1-46b4-rel41-cadsourcedm1.yaml";
    static final String GRANTING_URL = "/ecm_service/grant/v1/grants";
    static final String AUTH_HEADER = "AuthToken";
    static final String NFVO_TOKEN_PARAM = "nfvoToken";
    static final String NFVO_TOKEN = "token";

    void stubHealthCheck(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathEqualTo("/actuator/health")).willReturn(aResponse().withStatus(HttpStatus.OK.value())));
    }

    void verifyNotFoundInOperationsInProgressTable(OperationsInProgressRepository repository, String vnfId) {
        assertThat(repository.findByVnfId(vnfId)).isEmpty();
    }

    void verifyFoundInOperationsInProgressTable(OperationsInProgressRepository repository, String vnfId) {
        assertThat(repository.findByVnfId(vnfId)).isPresent();
    }

    MappingBuilder prepareGrantingRequest(String grantRequestBody) {
        return post(urlPathEqualTo(GRANTING_URL))
                .withHeader(AUTH_HEADER, equalTo(NFVO_TOKEN))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(APPLICATION_JSON_HEADER_UTF8))
                .withHeader(HttpHeaders.ACCEPT, equalTo(APPLICATION_JSON_HEADER))
                .withHeader(HttpHeaders.CACHE_CONTROL, equalTo(CACHE_CONTROL_HEADER))
                .withHeader(TENANT_ID_HEADER, equalTo(TENANT_ID_HEADER))
                .withRequestBody(equalToJson(grantRequestBody, true, true));
    }

    MappingBuilder prepareAnyGrantingRequest() {
        return post(urlPathEqualTo(GRANTING_URL))
                .withHeader(AUTH_HEADER, equalTo(NFVO_TOKEN))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(APPLICATION_JSON_HEADER_UTF8))
                .withHeader(HttpHeaders.ACCEPT, equalTo(APPLICATION_JSON_HEADER))
                .withHeader(HttpHeaders.CACHE_CONTROL, equalTo(CACHE_CONTROL_HEADER))
                .withHeader(TENANT_ID_HEADER, equalTo(TENANT_ID_HEADER));
    }

    String getGrantRequestBodyForInstantiate(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"operation\":\"INSTANTIATE\",\"addResources\":[{\"id\":\"${json-unit.any-string}\","
                        + "\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_storage\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container\", \"bulk_reporter_container2\"]}],"
                        + "\"isAutomaticInvocation\":false,\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForInstantiateWithDefaultDeployableModules(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfLcmOpOccId\":\"${json-unit.any-string}\",\"vnfdId\":\"%2$s\",\"operation\":\"INSTANTIATE\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\","
                        + "\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_container1_1\", "
                        + "\"bulk_reporter_container1_2\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_storage1_1\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container2_1\", \"bulk_reporter_container2_2\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage2_1\"]}],\"removeResources\": [],"
                        + "\"isAutomaticInvocation\":false,\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequest(String instanceId, String vnfdId) throws IOException, URISyntaxException {
        String grantRequestBody = TestUtils.readDataFromFile(String.format("granting/grantRequest/%s.json", instanceId));
        return format(grantRequestBody, instanceId, vnfdId);
    }

    String getGrantRequestBodyForTerminateWithDefaultDeployableModules(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfLcmOpOccId\":\"${json-unit.any-string}\",\"vnfdId\":\"%2$s\",\"operation\":\"TERMINATE\","
                        + "\"removeResources\":[{\"id\":\"${json-unit.any-string}\","
                        + "\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_container1_1\", "
                        + "\"bulk_reporter_container1_2\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_storage1_1\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container2_1\", \"bulk_reporter_container2_2\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage2_1\"]}],"
                        + "\"isAutomaticInvocation\":false,\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForInstantiateWithUpdatedDeployableModules(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfLcmOpOccId\":\"${json-unit.any-string}\",\"vnfdId\":\"%2$s\",\"operation\":\"INSTANTIATE\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\","
                        + "\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_container1_1\", "
                        + "\"bulk_reporter_container1_2\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter1\",\"resourceTemplateId\":[\"bulk_reporter_storage1_1\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container2_1\", \"bulk_reporter_container2_2\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter2\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage2_1\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter3\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container3_1\", \"bulk_reporter_container3_2\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter3\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage3_1\"]}],\"removeResources\": [],"
                        + "\"isAutomaticInvocation\":false,\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForTerminate(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"operation\":\"TERMINATE\",\"removeResources\":[{\"id\":\"${json-unit"
                        + ".any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container\", \"bulk_reporter_container2\"]}],"
                        + "\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForScale(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfLcmOpOccId\":\"${json-unit.any-string}\"," +
                        "\"vnfdId\":\"%2$s\",\"operation\":\"SCALE\"," +
                        "\"addResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\"," +
                        "\"resourceTemplateId\":[\"bulk_reporter_container\",\"bulk_reporter_container2\"]},{\"id\":\"${json-unit.any-string}\"," +
                        "\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_storage\"]}]," +
                        "\"removeResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\"," +
                        "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container\",\"bulk_reporter_container2\"]}," +
                        "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\"," +
                        "\"resourceTemplateId\":[\"bulk_reporter_storage\"]}]," +
                        "\"isAutomaticInvocation\":false,\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"}," +
                        "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForChangePackage(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"flavourId\":\"default\",\"operation\":\"CHANGE_VNFPKG\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_storage\"]}],"
                        + "\"removeResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container2\", \"bulk_reporter_container\"]}],"
                        + "\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForChangePackageRel4ToRel3(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"flavourId\":\"default\",\"operation\":\"CHANGE_VNFPKG\","
                        + "\"removeResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container2\", \"bulk_reporter_container\"]}],"
                        + "\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForChangePackageRel3ToRel4(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"flavourId\":\"default\",\"operation\":\"CHANGE_VNFPKG\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_storage\"]}],"
                        + "\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForChangePackageAfterScale(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"flavourId\":\"default\",\"operation\":\"CHANGE_VNFPKG\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_storage\"]}],"
                        + "\"removeResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container\", \"bulk_reporter_container2\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_container\", \"bulk_reporter_container2\"]}],"
                        + "\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    String getGrantRequestBodyForDowngrade(String instanceId, String vnfdId) {
        return format(
                "{\"vnfInstanceId\":\"%1$s\",\"vnfdId\":\"%2$s\",\"flavourId\":\"default\",\"operation\":\"CHANGE_VNFPKG\","
                        + "\"addResources\":[{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]},{\"id\":\"${json-unit.any-string}\",\"type\":\"OSCONTAINER\","
                        + "\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container2\", \"bulk_reporter_container\"]}],"
                        + "\"removeResources\":[{\"id\":\"${json-unit.any-string}\","
                        + "\"type\":\"OSCONTAINER\",\"vduId\":\"eric-pm-bulk-reporter\",\"resourceTemplateId\":[\"bulk_reporter_container\"]},"
                        + "{\"id\":\"${json-unit.any-string}\",\"type\":\"STORAGE\",\"vduId\":\"eric-pm-bulk-reporter\","
                        + "\"resourceTemplateId\":[\"bulk_reporter_storage\"]}],\"isAutomaticInvocation\":false,"
                        + "\"_links\":{\"vnfLcmOpOcc\":{\"href\":\"${json-unit.any-string}\"},"
                        + "\"vnfInstance\":{\"href\":\"https://localhost/vnflcm/v1/vnf_instances/%1$s\"}}}",
                instanceId, vnfdId);
    }

    void stubGettingCurrentVnfd(WireMockServer wireMockServer, String currentPackageId) throws Exception {
        String upgradeRel4Vnfd = TestUtils.readDataFromFile("granting/vnfd/rel4_vnfd.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", currentPackageId)))
                                       .willReturn(aResponse().withBody(upgradeRel4Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingVnfdWithDeployableModules(WireMockServer wireMockServer, String currentPackageId) throws Exception {
        String upgradeRel4Vnfd = TestUtils.readDataFromFile("granting/vnfd/d3def1ce-4cf4-477c-aab3-pkgId4e6a390.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", currentPackageId)))
                                       .willReturn(aResponse().withBody(upgradeRel4Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingVnfd(WireMockServer wireMockServer, String packageId) throws Exception {
        String upgradeRel4Vnfd = TestUtils.readDataFromFile(format("granting/vnfd/%s.yaml", packageId));
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", packageId)))
                                       .willReturn(aResponse().withBody(upgradeRel4Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingNewVnfd(WireMockServer wireMockServer, String newPackageId) throws Exception {
        String upgradeRel4Vnfd = TestUtils.readDataFromFile("granting/vnfd/rel4_vnfd_upgrade.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", newPackageId)))
                                       .willReturn(aResponse().withBody(upgradeRel4Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel3Vnfd(WireMockServer wireMockServer, String currentPackageId) throws Exception {
        String rel3Vnfd = TestUtils.readDataFromFile("granting/vnfd/rel3_vnfd.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", currentPackageId)))
                                       .willReturn(aResponse().withBody(rel3Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel3UpgradeVnfd(WireMockServer wireMockServer, String currentPackageId) throws Exception {
        String rel3Vnfd = TestUtils.readDataFromFile("granting/vnfd/rel3_vnfd_upgrade.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", currentPackageId)))
                                       .willReturn(aResponse().withBody(rel3Vnfd)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel3VnfdWithMixedScalableVdus(WireMockServer wireMockServer, String packageId) throws Exception {
        String rel4VnfdNonScalable = TestUtils.readDataFromFile("granting/vnfd/rel3_vnfd_mixed_vdus.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", packageId)))
                                       .willReturn(aResponse().withBody(rel4VnfdNonScalable)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel3VnfdWithNonScalableVdus(WireMockServer wireMockServer, String packageId) throws Exception {
        String rel4VnfdNonScalable = TestUtils.readDataFromFile("granting/vnfd/rel3_vnfd_non_scalable_vdus.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", packageId)))
                                       .willReturn(aResponse().withBody(rel4VnfdNonScalable)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel4VnfdWithMixedScalableVdus(WireMockServer wireMockServer, String packageId) throws Exception {
        String rel4VnfdNonScalable = TestUtils.readDataFromFile("granting/vnfd/rel4_vnfd_mixed_vdus.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", packageId)))
                                       .willReturn(aResponse().withBody(rel4VnfdNonScalable)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingRel4VnfdWithNonScalableVdus(WireMockServer wireMockServer, String packageId) throws Exception {
        String rel4VnfdNonScalable = TestUtils.readDataFromFile("granting/vnfd/rel4_vnfd_non_scalable_vdus.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/vnfd", packageId)))
                                       .willReturn(aResponse().withBody(rel4VnfdNonScalable)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingScalingMappingFile(WireMockServer wireMockServer, String packageId) throws IOException,
            URISyntaxException {
        String scalingMapping = TestUtils.readDataFromFile("scaling_mapping.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/artifacts/%s", packageId,
                                                         GrantingTestUtils.SCALING_MAPPING_PATH)))
                                       .willReturn(aResponse().withBody(scalingMapping)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingScalingMappingFileForPackage(WireMockServer wireMockServer, String packageId) throws IOException,
            URISyntaxException {
        String scalingMapping = TestUtils.readDataFromFile(format("granting/scalingMapping/%s.yaml", packageId));
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/artifacts/%s", packageId,
                                                         GrantingTestUtils.SCALING_MAPPING_PATH_DEPLOYABLE_MODULES)))
                                       .willReturn(aResponse().withBody(scalingMapping)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingScalingMappingFileWithDeployableModules(WireMockServer wireMockServer, String packageId) throws IOException,
            URISyntaxException {
        String scalingMapping = TestUtils.readDataFromFile("granting/scalingMapping/scaling_mapping_dm.yaml");
        wireMockServer.stubFor(get(urlPathEqualTo(format("/api/vnfpkgm/v1/vnf_packages/%s/artifacts/%s", packageId,
                                                         GrantingTestUtils.SCALING_MAPPING_PATH_DEPLOYABLE_MODULES)))
                                       .willReturn(aResponse().withBody(scalingMapping)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingPackageResponseByVnfd(WireMockServer wireMockServer, String vnfd) throws Exception {
        String packageResponse = TestUtils.readDataFromFile(format("granting/packageResponseFromNfvo/%1$s%2$s", vnfd, JSON_FILE_ENDING));
        wireMockServer.stubFor(get(urlPathEqualTo("/api/vnfpkgm/v1/vnf_packages"))
                                       .withQueryParam(format("(eq,vnfdId,%s)", vnfd), equalTo(""))
                                       .withHeader(HttpHeaders.ACCEPT, equalTo(APPLICATION_JSON_HEADER))
                                       .withHeader(HttpHeaders.CACHE_CONTROL, equalTo(CACHE_CONTROL_HEADER))
                                       .withHeader(AUTH_HEADER, equalTo(NFVO_TOKEN))
                                       .withHeader(TENANT_ID_HEADER, equalTo(TENANT_ID_HEADER))
                                       .willReturn(aResponse().withBody(packageResponse)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    void stubGettingPackageResponseByVnfdWithDeployableModules(WireMockServer wireMockServer, String vnfd) throws Exception {
        String packageResponse = TestUtils.readDataFromFile(format("granting/packageResponseFromNfvo/deployableModules/%1$s%2$s", vnfd,
                                                                   JSON_FILE_ENDING));
        wireMockServer.stubFor(get(urlPathEqualTo("/api/vnfpkgm/v1/vnf_packages"))
                                       .withQueryParam(format("(eq,vnfdId,%s)", vnfd), equalTo(""))
                                       .withHeader(HttpHeaders.ACCEPT, equalTo(APPLICATION_JSON_HEADER))
                                       .withHeader(HttpHeaders.CACHE_CONTROL, equalTo(CACHE_CONTROL_HEADER))
                                       .withHeader(AUTH_HEADER, equalTo(NFVO_TOKEN))
                                       .withHeader(TENANT_ID_HEADER, equalTo(TENANT_ID_HEADER))
                                       .willReturn(aResponse().withBody(packageResponse)
                                                           .withStatus(HttpStatus.OK.value())));
    }

    ProblemDetails getProblemDetailsForForbiddenResponse(String vnf_id, String pkgId) {
        return new ProblemDetails()
                .type(URI.create(TYPE_BLANK))
                .title("Granting Request wasn't confirmed by NFVO")
                .status(500)
                .detail(format("NFVO rejected a Granting request for VNF instance %1$s , Package ID %2$s based on policies and available capacity",
                               vnf_id, pkgId)
                )
                .instance(URI.create(vnf_id));
    }

    ProblemDetails getProblemDetailsForServiceUnavailableResponse(String vnf_id) {
        return new ProblemDetails()
                .type(URI.create(TYPE_BLANK))
                .title("NFVO Service call failed")
                .status(500)
                .detail(format("Error occurs due to failed Granting request for VNF %s", vnf_id))
                .instance(URI.create(vnf_id));
    }
}
