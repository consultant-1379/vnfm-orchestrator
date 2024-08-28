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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.MANUAL_CONTROLLED;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.VNF_CONTROLLED_SCALING;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.policies.InitialDelta;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDataType;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectProperties;
import com.ericsson.am.shared.vnfd.model.policies.ScalingAspects;
import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.OperationInProgress;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.presentation.helper.VnfdHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigService;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@TestPropertySource(properties = { "spring.flyway.locations = classpath:db/migration" })
public class NegativeUpgradeExtensionsBase extends AbstractDbSetupTest {

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private ClusterConfigService clusterConfigService;

    @SpyBean
    private DatabaseInteractionService databaseInteractionService;

    @MockBean
    private OperationsInProgressRepository operationsInProgressRepository;

    @MockBean
    private VnfdHelper vnfdHelper;

    @MockBean
    private ReplicaDetailsMapper replicaDetailsMapper;

    @SpyBean
    private ExtensionsService extensionsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        ReflectionTestUtils.setField(instanceService, "extensionsService", extensionsService);
        ReflectionTestUtils.setField(extensionsService, "mapper", objectMapper);

        Policies policies = getPolicies();
        final VnfInstance value = new VnfInstance();
        value.setVnfInstanceId("invalid-extensions-upgrade");
        value.setVnfInstanceName("InvalidExtensionsUpgradeInstanceName");
        value.setInstantiationState(InstantiationState.INSTANTIATED);
        Map<String, Object> extensions = new HashMap<>();
        Map<String, Object> vnfControlledScaling = new HashMap<>();
        vnfControlledScaling.put("Aspect1", MANUAL_CONTROLLED);
        extensions.put(VNF_CONTROLLED_SCALING, vnfControlledScaling);
        value.setVnfInfoModifiableAttributesExtensions(Utility.convertObjToJsonString(extensions));
        value.setTempInstance(Utility.convertObjToJsonString(value));
        value.setPolicies(objectMapper.writeValueAsString(policies));
        value.setSupportedOperations(TestUtils.createSupportedOperations(LCMOperationsEnum.values()));
        value.setVnfDescriptorId("testVnfDescriptorId");
        final var packageResponse = new PackageResponse();
        packageResponse.setDescriptorModel(readDataFromFile("granting/vnfd.json"));

        doReturn(value).when(databaseInteractionService).getVnfInstance(anyString());
        doReturn(value).when(instanceService).createTempInstanceForUpgradeOperation(any(), any(), any());
        doCallRealMethod().when(instanceService).setExtensions(any(), anyString(), any(), any());
        doReturn(new LifecycleOperation()).when(databaseInteractionService).persistLifecycleOperation(any());
        when(vnfdHelper.getVnfdScalingInformation(any())).thenReturn(Optional.of(policies));
        when(replicaDetailsMapper.getPoliciesFromVnfInstance(any())).thenReturn(policies);
        given(vnfInstanceRepository.findById("invalid-extensions-upgrade")).willReturn(Optional.of(value));
        given(packageService.getPackageInfoWithDescriptorModel(any())).willReturn(packageResponse);
        given(operationsInProgressRepository.save(any(OperationInProgress.class))).willReturn(new OperationInProgress());
        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private Policies getPolicies() {
        Map<String, ScalingAspectDataType> scalingAspectDataType = new HashMap<>();
        ScalingAspectDataType aspect1 = new ScalingAspectDataType();
        aspect1.setName("Aspect1");
        aspect1.setDescription("Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale "
                                       + "step");
        aspect1.setMaxScaleLevel(10);
        scalingAspectDataType.put("Aspect1", aspect1);
        ScalingAspectProperties scalingAspectProperties = new ScalingAspectProperties();
        scalingAspectProperties.setAllAspects(scalingAspectDataType);
        ScalingAspects aspects = new ScalingAspects();
        aspects.setProperties(scalingAspectProperties);
        aspects.setType("tosca.policies.nfv.ScalingAspects");
        Map<String, ScalingAspects> sclaingAspects = new HashMap<>();
        sclaingAspects.put("ScalingAspects", aspects);
        Policies policies = new Policies();
        policies.setAllScalingAspects(sclaingAspects);

        policies.setAllInitialDelta(Map.of("eric-pm-bulk-reporter", new InitialDelta()));
        return policies;
    }
}
