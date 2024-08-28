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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationErrorMessageBuilder.setError;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE_BLANK;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationStageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.LcmOperationsConfig;
import com.ericsson.vnfm.orchestrator.model.OperationDetails;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.LocalDateMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ResourceOperationsMapperImpl;
import com.ericsson.vnfm.orchestrator.repositories.ChangedInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigFileRepository;
import com.ericsson.vnfm.orchestrator.repositories.ClusterConfigInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.repositories.TaskRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceNamespaceDetailsRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;
import com.ericsson.vnfm.orchestrator.repositories.impl.query.PartialSelectionQueryExecutor;
import com.ericsson.vnfm.orchestrator.utils.Utility;


@SpringBootTest(classes = {
        ResourceOperationsServiceImpl.class,
        ResourceOperationsMapperImpl.class,
        LocalDateMapper.class,
        DatabaseInteractionService.class,
        LcmOperationsConfig.class
})
@MockBean({
        VnfInstanceRepository.class,
        ClusterConfigFileRepository.class,
        ClusterConfigInstanceRepository.class,
        VnfInstanceNamespaceDetailsRepository.class,
        ScaleInfoRepository.class,
        OperationsInProgressRepository.class,
        ChangedInfoRepository.class,
        VnfResourceViewRepository.class,
        HelmChartRepository.class,
        PartialSelectionQueryExecutor.class,
        TaskRepository.class,
        LifecycleOperationStageRepository.class
})
public class ResourceOperationsServiceTest {
    @Autowired
    private ResourceOperationsServiceImpl resourceOperationsService;

    @MockBean
    private LifecycleOperationRepository lifecycleOperationRepository;

    private static final String RESOURCE_INSTANCE_NAME = "Test Instance";
    private static final String RESOURCE_ID = "12121";
    private static final LifecycleOperationState EVENT = LifecycleOperationState.FAILED;
    private static final LifecycleOperationType OPERATION = LifecycleOperationType.CHANGE_PACKAGE_INFO;
    private static final String PRODUCT_NAME = "vEPG";
    private static final String SOFTWARE_VERSION = "1.13.2";
    private static final String ERROR_OBJECT_STRING = "{ \"type\": \"about:blank\", \"title\": \"Onboarding Overloaded\", \"status\":"
                                                                            + " 503, \"detail\": \"Onboarding service not available\", "
                                                                            + "\"instance\": \"null\" }";
    private static VnfInstance vnfInstance = new VnfInstance();
    private static LifecycleOperation lifecycleOperationWithError = new LifecycleOperation();
    private static LifecycleOperation lifecycleOperationWithoutError = new LifecycleOperation();
    private static ProblemDetails problemDetails = new ProblemDetails();
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2012, 9, 17, 19, 47);

    @BeforeEach
    public void setup() {
        setUpVnfInstance();
        setUpLifecycleOperations();
        setUpProblemDetails();
    }

    @Test
    public void testGetAllOperationDetails() {
        List<LifecycleOperation> lifecycleOperationList = new ArrayList<>();
        lifecycleOperationList.add(lifecycleOperationWithError);
        lifecycleOperationList.add(lifecycleOperationWithoutError);

        when(lifecycleOperationRepository.findAll()).thenReturn(lifecycleOperationList);
        List<OperationDetails> allOperations = resourceOperationsService.getAllOperationDetails();

        assertThat(allOperations).isNotEmpty();
        assertThat(allOperations.get(0).getResourceInstanceName()).isEqualTo(RESOURCE_INSTANCE_NAME);
        assertThat(allOperations.get(0).getResourceID()).isEqualTo(RESOURCE_ID);
        assertThat(allOperations.get(0).getOperation()).isEqualTo(OPERATION.name());
        assertThat(allOperations.get(0).getEvent()).isEqualTo(EVENT.name());
        assertThat(allOperations.get(0).getVnfProductName()).isEqualTo(PRODUCT_NAME);
        assertThat(allOperations.get(0).getVnfSoftwareVersion()).isEqualTo(SOFTWARE_VERSION);
        assertThat(allOperations.get(0).getTimestamp()).isEqualTo(Utility.convertToDate(TIMESTAMP));
        assertThat(allOperations.get(0).getError().toString()).isEqualTo(problemDetails.toString());

        assertThat(allOperations.get(1).getResourceInstanceName()).isEqualTo(RESOURCE_INSTANCE_NAME);
        assertThat(allOperations.get(1).getResourceID()).isEqualTo(RESOURCE_ID);
        assertThat(allOperations.get(1).getOperation()).isEqualTo(OPERATION.name());
        assertThat(allOperations.get(1).getEvent()).isEqualTo(EVENT.name());
        assertThat(allOperations.get(1).getVnfProductName()).isEqualTo(PRODUCT_NAME);
        assertThat(allOperations.get(1).getVnfSoftwareVersion()).isEqualTo(SOFTWARE_VERSION);
        assertThat(allOperations.get(1).getTimestamp()).isEqualTo(Utility.convertToDate(TIMESTAMP));
        assertThat(allOperations.get(1).getError()).isNull();
    }

    private void setUpVnfInstance() {
        vnfInstance.setVnfInstanceId(RESOURCE_ID);
        vnfInstance.setVnfInstanceName(RESOURCE_INSTANCE_NAME);
        vnfInstance.setVnfProductName(PRODUCT_NAME);
        vnfInstance.setVnfSoftwareVersion(SOFTWARE_VERSION);
    }

    private void setUpLifecycleOperations() {
        lifecycleOperationWithError.setVnfInstance(vnfInstance);
        lifecycleOperationWithError.setOperationState(EVENT);
        lifecycleOperationWithError.setLifecycleOperationType(OPERATION);
        lifecycleOperationWithError.setStateEnteredTime(TIMESTAMP);
        lifecycleOperationWithError.setVnfProductName(PRODUCT_NAME);
        lifecycleOperationWithError.setVnfSoftwareVersion(SOFTWARE_VERSION);
        setError(ERROR_OBJECT_STRING, lifecycleOperationWithError);

        lifecycleOperationWithoutError.setVnfInstance(vnfInstance);
        lifecycleOperationWithoutError.setOperationState(EVENT);
        lifecycleOperationWithoutError.setLifecycleOperationType(OPERATION);
        lifecycleOperationWithoutError.setStateEnteredTime(TIMESTAMP);
        lifecycleOperationWithoutError.setVnfProductName(PRODUCT_NAME);
        lifecycleOperationWithoutError.setVnfSoftwareVersion(SOFTWARE_VERSION);
        setError(null, lifecycleOperationWithoutError);

    }

    private void setUpProblemDetails() {
        problemDetails.setInstance(URI.create(TYPE_BLANK));
        problemDetails.setTitle("Onboarding Overloaded");
        problemDetails.setType(URI.create(TYPE_BLANK));
        problemDetails.setDetail("Onboarding service not available");
        problemDetails.setStatus(503);
    }
}
