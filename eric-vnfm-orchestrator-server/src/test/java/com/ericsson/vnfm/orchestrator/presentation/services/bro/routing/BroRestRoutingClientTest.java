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
package com.ericsson.vnfm.orchestrator.presentation.services.bro.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionRequest;
import com.ericsson.vnfm.orchestrator.model.backup.BroActionResponse;
import com.ericsson.vnfm.orchestrator.model.dto.BackupManagersResponseDtoWrapper;
import com.ericsson.vnfm.orchestrator.presentation.services.bro.BroServicesTestConfig;
import com.ericsson.vnfm.orchestrator.routing.HttpRoutingClient;
import com.ericsson.vnfm.orchestrator.routing.bro.BroHttpRoutingClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
    BroHttpRoutingClientImpl.class,
    BroServicesTestConfig.class,
    ObjectMapper.class
})
public class BroRestRoutingClientTest {

    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private HttpRoutingClient routingClient;
    @Autowired
    private BroHttpRoutingClientImpl broHttpRoutingClient;
    @Captor
    private ArgumentCaptor<String> hostCaptor;

    private static final String httpHost = "http://localhost";
    private static final String sftpHost = "sftp://user@localhost";
    private static final String broUrl = "test-url";

    @Test
    public void testGetAllScopesPositive() throws Exception {
        String getBackupsResponseString = TestUtils.parseJsonFile("contracts/api/getAllBackupScopes/positive/AllBackupScopesBroResponse.json");
        BackupManagersResponseDtoWrapper getBackupsResponse = map(getBackupsResponseString, BackupManagersResponseDtoWrapper.class);
        given(routingClient.executeHttpRequest(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
            , ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(getBackupsResponse);
        final List<String> allScopes = broHttpRoutingClient.getAllScopes(broUrl);
        assertEquals(2, allScopes.size());
        assertTrue(allScopes.containsAll(Stream.of("EVNFM", "DEFAULT").collect(Collectors.toList())));
    }

    @Test
    public void testExportBackupWithPassword() {
        BroActionResponse broResponse = getBroActionResponse();
        given(routingClient.executeHttpRequest(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
            , ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(broResponse);
        final BroActionResponse response = broHttpRoutingClient.exportBackup(broUrl, "default", "test-snapshot",
                                                                             sftpHost, "testPassword");
        assertThat(response.getId()).isEqualTo("54321");
    }

    @Test
    public void testExportBackupWithoutPassword() {
        BroActionResponse broResponse = getBroActionResponse();
        given(routingClient.executeHttpRequest(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
            , ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(broResponse);
        final BroActionResponse response = broHttpRoutingClient.exportBackup(broUrl, "default", null,
                                                                             httpHost, null);
        assertThat(response.getId()).isEqualTo("54321");
    }

    @Test
    public void testExportBackupRequestForHttpHost() throws Exception {
        BroActionResponse broResponse = getBroActionResponse();
        given(routingClient.executeHttpRequest(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
            , ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(broResponse);
        broHttpRoutingClient.exportBackup("url", "DEFAULT", "test", httpHost, "pass");
        Mockito.verify(routingClient).executeHttpRequest(Mockito.any(), Mockito.any(), Mockito.any(), hostCaptor.capture(), Mockito.any());
        final BroActionRequest requestWhichWasSent = map(hostCaptor.getValue(), BroActionRequest.class);
        assertThat(requestWhichWasSent.getPayload().getUri()).startsWith("http");
        assertThat(requestWhichWasSent.getPayload().getPassword()).isNull();
    }

    @Test
    public void testExportBackupForSftpHost() throws Exception {
        BroActionResponse broResponse = getBroActionResponse();
        given(routingClient.executeHttpRequest(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
            , ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(broResponse);
        broHttpRoutingClient.exportBackup("url", "DEFAULT", "test", sftpHost, "pass");
        Mockito.verify(routingClient).executeHttpRequest(Mockito.any(), Mockito.any(), Mockito.any(), hostCaptor.capture(), Mockito.any());
        final BroActionRequest originalRequest = map(hostCaptor.getValue(), BroActionRequest.class);
        assertThat(originalRequest.getPayload().getUri()).startsWith("sftp");
        assertThat(originalRequest.getPayload().getPassword()).isNotNull();
    }

    private BroActionResponse getBroActionResponse() {
        final BroActionResponse response = new BroActionResponse();
        response.setId("54321");
        return response;
    }

    private <T> T map(String requestString, Class<T> mappingClass) throws Exception {
        return mapper.readValue(requestString, mappingClass);
    }
}