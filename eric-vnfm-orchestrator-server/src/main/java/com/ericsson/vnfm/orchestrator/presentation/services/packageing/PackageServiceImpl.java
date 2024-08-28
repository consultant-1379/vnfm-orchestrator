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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import static com.ericsson.am.shared.vnfd.utils.Constants.NODE_TYPES_KEY;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.ericsson.am.shared.vnfd.CommonUtility;
import com.ericsson.am.shared.vnfd.ScalingMapUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.ArtifactsPropertiesDetail;
import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.ScaleMapping;
import com.ericsson.am.shared.vnfd.validation.ToscaSupportedOperationValidator;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.UsageStateRequest;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmPackage;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfdNotFoundException;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PackageServiceImpl implements PackageService {

    private static final String PACKAGE_NOT_FOUND_WITH_ID = "Package not found with VNFD ID ";

    @Autowired
    private OnboardingClient onboardingClient;

    @Autowired
    private OnboardingUriProvider onboardingUriProvider;

    @Autowired
    private NfvoConfig nfvoConfig;

    public JSONObject getVnfd(final String vnfPkgId) {
        URI getVnfdUri = onboardingUriProvider.getVnfdQueryUri(vnfPkgId);
        LOGGER.info("Started getting VNFD for VNF package with ID: {} by URI: {}", vnfPkgId, getVnfdUri);
        String vnfd = onboardingClient.get(getVnfdUri, MediaType.TEXT_PLAIN_VALUE, String.class)
                .orElseThrow(() -> new VnfdNotFoundException(String.format("VNFD of package with ID: %s is not found", vnfPkgId)));
        return VnfdUtility.validateYamlAndConvertToJsonObject(vnfd);
    }

    public Map<String, ScaleMapping> getScalingMapping(final String vnfPkgId, final String scaleMappingFilePath) {
        URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfPkgId, scaleMappingFilePath);
        LOGGER.info("Started getting scaling mapping file for VNF package with ID: {} by URI: {}", vnfPkgId, getArtifactUri);
        Optional<String> mappingFileResponse = onboardingClient.get(getArtifactUri, MediaType.TEXT_PLAIN_VALUE, String.class);
        if (mappingFileResponse.isEmpty() || isEmpty(mappingFileResponse.get())) {
            LOGGER.info("Completed getting scaling mapping file for VNF package with ID: {} by URI: {}. File is empty or not present", vnfPkgId,
                        getArtifactUri);
            return Collections.emptyMap();
        }
        String mappingFileBody = mappingFileResponse.get();
        LOGGER.info("Completed getting scaling mapping file for VNF package with ID: {} by URI: {}. Response: {}", vnfPkgId, getArtifactUri,
                    mappingFileBody);
        return ScalingMapUtility.getScalingMap(mappingFileBody);
    }

    public PackageResponse getPackageInfo(final String vnfdId) {
        final URI packageQueryUri = onboardingUriProvider.getOnboardingPackagesQueryUri(vnfdId);
        LOGGER.info("Started getting package info for VNF package with VNFD ID: {} by URI: {}", vnfdId, packageQueryUri);
        final PackageResponse[] packageResponses = onboardingClient.get(packageQueryUri,
                                                                        MediaType.APPLICATION_JSON_VALUE,
                                                                        PackageResponse[].class)
                .orElseThrow(() -> new PackageDetailsNotFoundException(PACKAGE_NOT_FOUND_WITH_ID + vnfdId));
        LOGGER.debug("Completed getting package info for for VNF package with VNFD ID: {} by URI: {}. Response: {}", vnfdId, packageQueryUri,
                     packageResponses);
        validatePackageResponse(packageResponses, vnfdId);
        final PackageResponse packageResponse = packageResponses[0];
        if (nfvoConfig.isEnabled()) {
            updatePackageResponseWithChartArtifactKeys(packageResponse);
        }
        return packageResponse;
    }

    @Override
    public PackageResponse getPackageInfoWithDescriptorModel(final String vnfdId) {
        PackageResponse packageInfo = getPackageInfo(vnfdId);
        packageInfo.setDescriptorModel(getVnfd(packageInfo.getId()).toString());
        return packageInfo;
    }

    @Override
    public Optional<String> getPackageArtifacts(String vnfPkgId, String artifactPath) {
        final URI getArtifactUri = onboardingUriProvider.getArtifactUri(vnfPkgId, artifactPath);
        final Optional<String> packageArtifactsOptional = onboardingClient.get(getArtifactUri, MediaType.TEXT_PLAIN_VALUE, String.class);
        if (packageArtifactsOptional.isEmpty()) {
            LOGGER.info("Artifacts not found for artifactPath: {}", artifactPath);
        } else {
            LOGGER.info("Completed getting package artifacts for VNFD ID: {}, ArtifactPath: {} by URI: {}. Response: {}",
                    vnfPkgId, artifactPath, getArtifactUri, packageArtifactsOptional.get());
        }
        return packageArtifactsOptional;
    }

    @Override
    public List<OperationDetail> getSupportedOperations(String vnfPackageId) {
        LOGGER.info("Started getting supported operations for VNF package with ID: {}", vnfPackageId);
        String vnfd = getVnfd(vnfPackageId).toString();
        return ToscaSupportedOperationValidator.getVnfdSupportedOperations(vnfd);
    }

    @Override
    public void updateUsageState(final String vnfPkgId, final String vnfInstanceId, final boolean isInUse) {
        if (nfvoConfig.isEnabled()) {
            LOGGER.info("Skipping updates to usage state as nfvoConfig.isEnabled() returns {}", nfvoConfig.isEnabled() + "");
            return;
        }
        if (StringUtils.isBlank(vnfPkgId) || StringUtils.isBlank(vnfInstanceId)) {
            throw new InternalRuntimeException("Package Id and Instance Id are mandatory fields");
        }

        UsageStateRequest usageStateRequest = buildUsageStateRequest(vnfInstanceId, isInUse);
        URI usageStateUri = onboardingUriProvider.getUpdateUsageStateUri(vnfPkgId);
        LOGGER.info("Updating package {} with instance {} to inUse state : {}", vnfPkgId, vnfInstanceId, isInUse);

        try {
            onboardingClient.put(usageStateUri, usageStateRequest);
        } catch (PackageDetailsNotFoundException e) {
            LOGGER.warn("There is no package with id {}", vnfPkgId, e);
        } catch (HttpServerErrorException.InternalServerError e) {
            throw new InternalRuntimeException(String.format(
                    "Unable to update usage state for package %s; Failed due to unavailable Onboarding Service", vnfPkgId), e);
        } catch (InternalRuntimeException e) {
            throw new InternalRuntimeException(String.format(
                    "Update usage state for package %s and VNF Instance %s to isUsageState %s failed",
                    vnfPkgId, vnfInstanceId, isInUse), e);
        }
    }

    private UsageStateRequest buildUsageStateRequest(final String instanceId, final boolean isInUse) {
        UsageStateRequest usageStateRequest = new UsageStateRequest();
        usageStateRequest.setVnfId(instanceId);
        usageStateRequest.setInUse(isInUse);
        return usageStateRequest;
    }

    private void updatePackageResponseWithChartArtifactKeys(final PackageResponse packageResponse) {
        JSONObject vnfd = getVnfd(packageResponse.getId());
        JSONObject nodeType = vnfd.getJSONObject(NODE_TYPES_KEY);
        for (final ArtifactsPropertiesDetail artifact : CommonUtility.getArtifacts(nodeType)) {
            String artifactFileName = StringUtils.substringAfterLast(artifact.getFile(), File.separator);
            for (final HelmPackage helmPackage : packageResponse.getHelmPackageUrls()) {
                String fileNameInUrl = StringUtils.substringAfterLast(helmPackage.getChartUrl(), File.separator);
                if (fileNameInUrl.equals(artifactFileName)) {
                    helmPackage.setChartArtifactKey(artifact.getId());
                }
            }
        }
    }

    private void validatePackageResponse(PackageResponse[] packageResponses, String vnfdId) {
        int numberOfPackages = packageResponses.length;
        if (numberOfPackages == 0) {
            throw new UnprocessablePackageException(PACKAGE_NOT_FOUND_WITH_ID + vnfdId);
        }
        if (numberOfPackages != 1) {
            String errorMessage = String.format("Only one package is expected to have vnfdId %s but %s were found in the response "
                                                        + "from the Onboarding service", vnfdId, numberOfPackages);
            throw new UnprocessablePackageException(errorMessage);
        }
    }
}
