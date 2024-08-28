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
package com.ericsson.vnfm.orchestrator.routing.bro;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.BACKUP_ACTION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.BRO_GET_BACKUPS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.BRO_GET_BACKUP_MANAGERS;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionRequest;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionResponse;
import com.ericsson.vnfm.orchestrator.model.backup.BroActions;
import com.ericsson.vnfm.orchestrator.model.dto.BackupManagerDto;
import com.ericsson.vnfm.orchestrator.model.dto.BackupManagersResponseDtoWrapper;
import com.ericsson.vnfm.orchestrator.model.dto.BackupResponseDtoWrapper;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.HttpClientException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.routing.HttpRoutingClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BroHttpRoutingClientImpl implements BroHttpRoutingClient {

    private static final String UNABLE_TO_PARSE_REQUEST = "Unable to parse the request json due to %s";
    private static final String REMOTE_HOST_NOT_VALID = "Remote host for backup can not be empty.";

    @Autowired
    private HttpRoutingClient routingClient;

    @Autowired
    private ObjectMapper objectMapper;


    public List<BackupsResponseDto> getBackupsByScope(final String broUrl, final String scope) {
        final BackupResponseDtoWrapper backupResponseDtoWrapper = executeGetRequest(String.format(BRO_GET_BACKUPS,
                                                                                                  broUrl,
                                                                                                  scope),
                                                                                    MediaType.APPLICATION_JSON,
                                                                                    BackupResponseDtoWrapper.class);
        return backupResponseDtoWrapper
                .getBackups().stream().peek(s -> s.setScope(scope)).collect(Collectors.toList());
    }

    @Override
    public BroActionResponse exportBackup(String broUrl, String scope, String backupName, String remoteHost, String password) {
        final BroActionRequest broActionRequest = createExportRequest(backupName, remoteHost, password);
        String jsonRequest = convertToJson(broActionRequest);
        return routingClient.executeHttpRequest(createHeaders(MediaType.APPLICATION_JSON),
                String.format(BACKUP_ACTION,
                        broUrl,
                        scope), HttpMethod.POST, jsonRequest, BroActionResponse.class);
    }

    @Override
    public BroActionResponse createBackup(String broUrl, String scope, String backupName) {
        final BroActionRequest broActionRequest = new BroActionRequest
                .BroActionRequestBuilder(BroActions.CREATE_BACKUP.toString())
                .withBackupName(backupName)
                .buildRequest();
        String jsonRequest = convertToJson(broActionRequest);
        return routingClient.executeHttpRequest(createHeaders(MediaType.APPLICATION_JSON),
                String.format(BACKUP_ACTION, broUrl, scope),
                HttpMethod.POST,
                jsonRequest,
                BroActionResponse.class);
    }

    public List<String> getAllScopes(final String broUrl) {
        final BackupManagersResponseDtoWrapper response = executeGetRequest(String.format(BRO_GET_BACKUP_MANAGERS, broUrl),
                                                                              MediaType.APPLICATION_JSON, BackupManagersResponseDtoWrapper.class);
        return response.getBackupManagers().stream().map(BackupManagerDto::getId).collect(Collectors.toList());
    }

    @Override
    public BroActionResponse deleteBackup(String broUrl, String scope, String backupName) {
        final BroActionRequest broActionRequest = new BroActionRequest
                .BroActionRequestBuilder(BroActions.DELETE_BACKUP.toString())
                .withBackupName(backupName)
                .buildRequest();
        String jsonRequest = convertToJson(broActionRequest);
        return routingClient.executeHttpRequest(createHeaders(MediaType.APPLICATION_JSON),
                                                String.format(BACKUP_ACTION, broUrl, scope),
                                                HttpMethod.POST,
                                                jsonRequest,
                                                BroActionResponse.class);
    }

    private <R> R executeGetRequest(String broUrl, MediaType contentType, Class<R> responseType) {
        return routingClient.executeHttpRequest(createHeaders(contentType),
                                                broUrl,
                                                HttpMethod.GET,
                                                null,
                                                responseType);
    }

    private static HttpHeaders createHeaders(final MediaType contentType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType.toString());
        return headers;
    }

    private static BroActionRequest createExportRequest(String backupName, String remoteHost, String password) {
        if (Strings.isNullOrEmpty(remoteHost)) {
            throw new HttpClientException(REMOTE_HOST_NOT_VALID);
        }
        return remoteHost.startsWith("http")
                ? createHttpExportRequest(remoteHost, backupName)
                : createSftpExportRequest(backupName, remoteHost, password);
    }

    private static BroActionRequest createHttpExportRequest(String remoteHost, String backupName) {
        return new BroActionRequest
                .BroActionRequestBuilder(BroActions.EXPORT.toString())
                .withBackupName(backupName)
                .withUri(remoteHost)
                .buildRequest();
    }

    private static BroActionRequest createSftpExportRequest(String backupName, String remoteHost, String password) {
        return new BroActionRequest
                .BroActionRequestBuilder(BroActions.EXPORT.toString())
                .withBackupName(backupName)
                .withPassword(password)
                .withUri(remoteHost)
                .buildRequest();
    }

    private String convertToJson(BroActionRequest broActionRequest) {
        try {
            return objectMapper.writeValueAsString(broActionRequest);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException(String.format(UNABLE_TO_PARSE_REQUEST, e.getMessage()), e);
        }
    }
}
