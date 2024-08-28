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
package com.ericsson.vnfm.orchestrator.presentation.services.backups;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import static com.ericsson.vnfm.orchestrator.TestUtils.BRO_ENDPOINT_URL;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_DESCRIPTOR_ID;
import static com.ericsson.vnfm.orchestrator.TestUtils.DUMMY_INSTANCE_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.MISSING_VNF_INSTANCE_PARAMS_MESSAGE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.MissingMandatoryParameterException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.routing.RestTemplateHttpRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.bro.BroHttpRoutingClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    BackupsServiceImpl.class,
    ObjectMapper.class,
    BroHttpRoutingClientImpl.class,
    RestTemplateHttpRoutingClient.class
})
public class BackupsServiceImplTest {
    @Autowired
    private BackupsServiceImpl backupsService;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private DatabaseInteractionService databaseInteractionService;
    @Test
    public void testGetAllBackupsSuccess() {
        ResponseEntity<String> getAllBackupsByScopeResponse = new ResponseEntity<>(TestUtils.parseJsonFile(
                "snapshots/getAllBackupsFromBroResponse.json"),
                HttpStatus.OK);
        ResponseEntity<String> getScopesResponse = new ResponseEntity<>(TestUtils
                .parseJsonFile("contracts/api/getAllBackupScopes/positive/AllBackupScopesBroResponse.json"), HttpStatus.OK);
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(getTestVnfInstance());
        given(restTemplate.exchange(ArgumentMatchers.endsWith("backup"), any(HttpMethod.class), any(HttpEntity.class),
                ArgumentMatchers.<Class<String>>any())).willReturn(getAllBackupsByScopeResponse);
        given(restTemplate.exchange(ArgumentMatchers.endsWith("backup-manager"), any(HttpMethod.class), any(HttpEntity.class),
                ArgumentMatchers.<Class<String>>any())).willReturn(getScopesResponse);

        final List<BackupsResponseDto> backups = backupsService.getAllBackups(DUMMY_INSTANCE_ID);
        assertEquals(4, backups.size());
        assertEquals(4, backups.stream().map(BackupsResponseDto::getId).filter(s -> s.equals("test1") || s.equals("test2")).count());
    }

    @Test
    public void testGetAllBackupsFailsNoBroUrlProvided() {
        given(databaseInteractionService.getVnfInstance(DUMMY_INSTANCE_ID + "-no-bro-url")).willReturn(getTestNoBroUrlVnfInstance());
        assertThatThrownBy(() -> backupsService.getAllBackups(DUMMY_INSTANCE_ID + "-no-bro-url"))
                .isInstanceOf(MissingMandatoryParameterException.class)
                .hasMessageStartingWith(MISSING_VNF_INSTANCE_PARAMS_MESSAGE);
    }

    @Test
    public void testGetAllBackupsFailsVnfNotInstantiated() {
        given(databaseInteractionService.getVnfInstance(DUMMY_INSTANCE_ID + "-not-instantiated-state")).willReturn(getTestNotInstantiatedVnfInstance());
        assertThatThrownBy(() -> backupsService.getAllBackups(DUMMY_INSTANCE_ID + "-not-instantiated-state"))
                .isInstanceOf(NotInstantiatedException.class);
    }

    @Test
    public void testGetAllBackupsShouldReturnEmptyListIfNoBackupsFound() {
        given(databaseInteractionService.getVnfInstance(anyString())).willReturn(getTestVnfInstance());
        given(restTemplate.exchange(ArgumentMatchers.endsWith("backup-manager"), any(HttpMethod.class), any(HttpEntity.class),
                                    ArgumentMatchers.<Class<String>>any())).willReturn(ResponseEntity.ok("{\"backupManagers\":[{\"backupType"
                                                                                                                         + "\":\"myType\",\"backupDomain\":\"myDomain\",\"id\":\"DEFAULT\"}]}"));
        given(restTemplate.exchange(ArgumentMatchers.endsWith("backup"), any(HttpMethod.class), any(HttpEntity.class),
                                    ArgumentMatchers.<Class<String>>any())).willReturn(ResponseEntity.ok("{\"backups\":[]}"));
        final List<BackupsResponseDto> snapshots = backupsService.getAllBackups(DUMMY_INSTANCE_ID);
        assertEquals(0, snapshots.size());
    }

    private VnfInstance getTestVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(DUMMY_INSTANCE_ID);
        vnfInstance.setVnfDescriptorId(DUMMY_DESCRIPTOR_ID);
        vnfInstance.setBroEndpointUrl(BRO_ENDPOINT_URL);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        return vnfInstance;
    }

    private VnfInstance getTestNotInstantiatedVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(DUMMY_INSTANCE_ID + "-not-instantiated-state");
        vnfInstance.setVnfDescriptorId(DUMMY_DESCRIPTOR_ID);
        vnfInstance.setBroEndpointUrl(BRO_ENDPOINT_URL);
        vnfInstance.setInstantiationState(InstantiationState.NOT_INSTANTIATED);
        return vnfInstance;
    }

    private VnfInstance getTestNoBroUrlVnfInstance() {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(DUMMY_INSTANCE_ID + "-no-bro-url");
        vnfInstance.setVnfDescriptorId(DUMMY_DESCRIPTOR_ID);
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        return vnfInstance;
    }
}