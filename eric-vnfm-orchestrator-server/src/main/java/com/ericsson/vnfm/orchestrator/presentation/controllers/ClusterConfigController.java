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
package com.ericsson.vnfm.orchestrator.presentation.controllers;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.ClusterConfigs.NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.ClusterConfigs.SORT_COLUMNS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.OPERATION_WITHOUT_LICENSE_ATTRIBUTE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_IS_FINISHED_TEXT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Messages.OPERATION_PERFORMED_TEXT;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildLinks;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildPaginationInfo;
import static com.ericsson.vnfm.orchestrator.utils.UrlUtils.PAGE;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoJson;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.vnfm.orchestrator.api.CismClusterDataApi;
import com.ericsson.vnfm.orchestrator.api.ClusterConfigsApi;
import com.ericsson.vnfm.orchestrator.model.ClusterConfigData;
import com.ericsson.vnfm.orchestrator.model.ClusterConfigPatchRequest;
import com.ericsson.vnfm.orchestrator.model.ClusterConfigResponse;
import com.ericsson.vnfm.orchestrator.model.PagedCismClusterConfigsResponse;
import com.ericsson.vnfm.orchestrator.model.PagedClusterConfigsResponse;
import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ClusterConfigFile;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.idempotency.IdempotencyService;
import com.ericsson.vnfm.orchestrator.presentation.services.license.OrchestratorLimitsCalculator;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.CismClusterConfigMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ClusterConfigMapper;
import com.ericsson.vnfm.orchestrator.utils.PaginationUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/vnflcm/v1")
public class ClusterConfigController implements ClusterConfigsApi, CismClusterDataApi {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private UsernameCalculationService usernameCalculationService;

    @Autowired
    private ClusterConfigMapper clusterConfigMapper;

    @Autowired
    private CismClusterConfigMapper cismClusterConfigMapper;

    @Autowired
    private OrchestratorLimitsCalculator orchestratorLimitsCalculator;

    @Autowired
    private IdempotencyService idempotencyService;

    private final ClusterConfigService configService;

    public ClusterConfigController(ClusterConfigService configService) {
        this.configService = configService;
    }

    @Override
    public ResponseEntity<ClusterConfigResponse> registerClusterConfigFile(String accept,
                                                                           String contentType,
                                                                           String idempotencyKey,
                                                                           MultipartFile clusterConfig,
                                                                           String description,
                                                                           String crdNamespace,
                                                                           Boolean isDefault) {

        Supplier<ResponseEntity<ClusterConfigResponse>> registerClusterSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(OPERATION_PERFORMED_TEXT, "Registering cluster", "name", clusterConfig.getOriginalFilename(), requestUsername);

            String header = httpServletRequest.getHeader("wfs-clusterconfig");
            ClusterConfigFile newClusterConfigFile = configService.prepareRegistrationClusterConfig(clusterConfig,
                                                                                                    description,
                                                                                                    crdNamespace,
                                                                                                    isDefault);
            orchestratorLimitsCalculator.checkOrchestratorLimitsForClusters(httpServletRequest.getAttribute(OPERATION_WITHOUT_LICENSE_ATTRIBUTE));
            configService.registerClusterConfig(newClusterConfigFile);
            ClusterConfigResponse responseDto = clusterConfigMapper.toInternalModel(newClusterConfigFile);

            LOGGER.info(OPERATION_IS_FINISHED_TEXT, "Registering cluster", "name", clusterConfig.getOriginalFilename());

            if (header == null) {
                return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>(HttpStatus.CREATED);
            }
        };

        return idempotencyService.executeTransactionalIdempotentCall(registerClusterSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<Void> deregisterClusterConfigByName(String clusterConfigName, String accept, String idempotencyKey) {

        Supplier<ResponseEntity<Void>> deregisterClusterSupplier = () -> {
            String requestUsername = usernameCalculationService.calculateUsername();
            LOGGER.info(OPERATION_PERFORMED_TEXT, "De-registering cluster", "name", clusterConfigName, requestUsername);

            configService.deregisterClusterConfig(clusterConfigName);

            LOGGER.info(OPERATION_IS_FINISHED_TEXT, "De-registering cluster", "name", clusterConfigName);

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        };

        return idempotencyService.executeTransactionalIdempotentCall(deregisterClusterSupplier, idempotencyKey);
    }

    @Override
    public ResponseEntity<PagedClusterConfigsResponse> getAllClusterConfigs(final String accept,
                                                                            final String filter,
                                                                            final Boolean getAllConfigs,
                                                                            final Integer page,
                                                                            final Integer size,
                                                                            final List<String> sort) {
        Pageable pageable;
        if (getAllConfigs) {
            pageable = Pageable.unpaged();
        } else {
            pageable = new PaginationUtils.PageableBuilder()
                    .defaults(NAME)
                    .page(page).size(size).sort(sort, SORT_COLUMNS)
                    .build();
        }
        Page<ClusterConfigFile> resultsPage = configService.getClusterConfigs(filter, pageable);
        PaginationInfo paginationInfo = buildPaginationInfo(resultsPage);
        PagedClusterConfigsResponse response = new PagedClusterConfigsResponse()
                .page(paginationInfo).links(buildLinks(paginationInfo, PAGE))
                .items(resultsPage.map(clusterConfigMapper::toInternalModel).getContent());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ClusterConfigResponse> updateClusterConfigByName(String clusterConfigName,
                                                                           String accept,
                                                                           String contentType,
                                                                           MultipartFile clusterConfig,
                                                                           Boolean skipSameClusterVerification,
                                                                           String description,
                                                                           Boolean isDefault) {
        String requestUsername = usernameCalculationService.calculateUsername();
        LOGGER.info(OPERATION_PERFORMED_TEXT, "Updating cluster", "name", clusterConfigName, requestUsername);

        ClusterConfigFile clusterConfigFile = configService.updateClusterConfig(clusterConfigName,
                                                                                clusterConfig,
                                                                                description,
                                                                                Boolean.TRUE.equals(skipSameClusterVerification),
                                                                                Boolean.TRUE.equals(isDefault));
        ClusterConfigResponse clusterConfigResponse = clusterConfigMapper.toInternalModel(clusterConfigFile);

        LOGGER.info(OPERATION_IS_FINISHED_TEXT, "Updating cluster", "name", clusterConfigName);

        return new ResponseEntity<>(clusterConfigResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ClusterConfigResponse> updateClusterConfigPartiallyByName(String clusterConfigName,
                                                                                    String accept,
                                                                                    String contentType,
                                                                                    ClusterConfigPatchRequest clusterConfigRequest,
                                                                                    Boolean isSkipSameClusterVerification) {
        String requestUsername = usernameCalculationService.calculateUsername();
        LOGGER.info(OPERATION_PERFORMED_TEXT, "Partially updating cluster", "name", clusterConfigName, requestUsername);
        ClusterConfigFile updatedClusterConfigFile = configService.modifyClusterConfig(clusterConfigName,
                                                                                       clusterConfigRequest,
                                                                                       Boolean.TRUE.equals(isSkipSameClusterVerification));

        ClusterConfigResponse clusterConfigResponse = clusterConfigMapper.toInternalModel(updatedClusterConfigFile);

        return new ResponseEntity<>(clusterConfigResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedCismClusterConfigsResponse> getCismClusterConfigsData(Integer page, Integer size) {
        Pageable pageable = new PaginationUtils.PageableBuilder()
                .page(page)
                .size(size)
                .build();
        Page<ClusterConfigFile> resultsPage = configService.getCismClusterConfigs(pageable);
        resultsPage.forEach(clusterConfigData -> clusterConfigData.setContent(convertYamlStringIntoJson(clusterConfigData.getContent()).toString()));

        PaginationInfo paginationInfo = buildPaginationInfo(resultsPage);
        PagedCismClusterConfigsResponse response = new PagedCismClusterConfigsResponse()
                .page(paginationInfo).links(buildLinks(paginationInfo, PAGE))
                .items(resultsPage.map(f -> getToInternalModel(f)).getContent());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private ClusterConfigData getToInternalModel(ClusterConfigFile clusterConfigFile) {
        return cismClusterConfigMapper.toInternalModel(clusterConfigFile);
    }
}