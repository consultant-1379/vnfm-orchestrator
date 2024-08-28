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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.bro.BroHttpRoutingClient;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetBackupsPositiveBase extends ContractTestRunner {

    @MockBean
    private BroHttpRoutingClient broHttpRoutingClient;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @BeforeEach
    public void setUp() {
        when(broHttpRoutingClient.getBackupsByScope(anyString(), anyString())).thenReturn(getAllBackups());

        List<BackupsResponseDto> backupsResponseDtos = new ArrayList<>();
        when(broHttpRoutingClient.getBackupsByScope(contains("noBackups"), anyString())).thenReturn(backupsResponseDtos);
        when(broHttpRoutingClient.getAllScopes(anyString())).thenReturn(List.of("EVNFM"));

        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId("1234");
        vnfInstance.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstance.setBroEndpointUrl("http://eric-bro-ctl.test:7001");

        VnfInstance vnfInstanceNoBackups = new VnfInstance();
        vnfInstanceNoBackups.setVnfInstanceId("34");
        vnfInstanceNoBackups.setInstantiationState(InstantiationState.INSTANTIATED);
        vnfInstanceNoBackups.setBroEndpointUrl("http://noBackups:7001");

        when(vnfInstanceRepository.findById(anyString())).thenReturn(Optional.of(vnfInstance));
        when(vnfInstanceRepository.findById(contains("-no-backups"))).thenReturn(Optional.of(vnfInstanceNoBackups));
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private List<BackupsResponseDto> getAllBackups() {
        List<BackupsResponseDto> backupsResponses = new ArrayList<>();
        BackupsResponseDto backupsResponse1 = new BackupsResponseDto();
        backupsResponse1.setId("12345");
        backupsResponse1.setName("cnf-backup_3.2.0_20210120155030");
        LocalDateTime creationDate1 = LocalDateTime.of(2020, Month.JANUARY, 20, 15,
                                                       52, 31, 831);
        backupsResponse1.setCreationTime(Date.from(creationDate1.atZone(ZoneId.systemDefault()).toInstant()));
        backupsResponse1.setStatus(BackupsResponseDto.StatusEnum.COMPLETE);
        backupsResponse1.setScope("EVNFM");

        BackupsResponseDto backupsResponse2 = new BackupsResponseDto();
        backupsResponse2.setId("12346");
        backupsResponse2.setName("cnf-backup_3.2.0_20210120165030");
        LocalDateTime creationDate2 = LocalDateTime.of(2020, Month.JANUARY, 20, 16,
                                                       52, 31, 831);
        backupsResponse2.setCreationTime(Date.from(creationDate2.atZone(ZoneId.systemDefault()).toInstant()));
        backupsResponse2.setStatus(BackupsResponseDto.StatusEnum.INCOMPLETE);
        backupsResponse2.setScope("EVNFM");

        BackupsResponseDto backupsResponse3 = new BackupsResponseDto();
        backupsResponse3.setId("12347");
        backupsResponse3.setName("cnf-backup_3.2.0_20210120175030");
        LocalDateTime creationDate3 = LocalDateTime.of(2020, Month.JANUARY, 20, 17,
                                                       52, 31, 831);
        backupsResponse3.setCreationTime(Date.from(creationDate3.atZone(ZoneId.systemDefault()).toInstant()));
        backupsResponse3.setStatus(BackupsResponseDto.StatusEnum.CORRUPTED);
        backupsResponse3.setScope("EVNFM");

        backupsResponses.add(backupsResponse1);
        backupsResponses.add(backupsResponse2);
        backupsResponses.add(backupsResponse3);
        return backupsResponses;
    }
}
