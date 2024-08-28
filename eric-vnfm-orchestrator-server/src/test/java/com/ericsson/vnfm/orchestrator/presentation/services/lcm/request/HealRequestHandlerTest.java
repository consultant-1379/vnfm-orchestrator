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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.createNotSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.createNotSupportedOperationsWithError;
import static com.ericsson.vnfm.orchestrator.TestUtils.createSupportedOperations;
import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_FILE_REFERENCE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_BACKUP_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Heal.RESTORE_PASSWORD;

import java.util.List;
import java.util.Map;

import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.DefaultLcmOpErrorProcessor;
import com.ericsson.vnfm.orchestrator.presentation.services.VnfInstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorProcessorFactory;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ExtensionsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.oss.topology.CMPEnrollmentHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.HealVnfRequest;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidHealRequestException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NamespaceDeletionInProgressException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.OperationNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.services.ClusterConfigServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.presentation.services.LifeCycleManagementHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileComposer;
import com.ericsson.vnfm.orchestrator.presentation.services.WorkflowServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.calculation.UsernameCalculationService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.GrantingService;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.MappingFileService;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.Day0ConfigurationService;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.evnfm.WorkflowRoutingServicePassThrough;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
        ObjectMapper.class,
        HealRequestService.class,
        HealRequestHandler.class,
        CNFHealRequestHandler.class,
        CNAHealRequestHandler.class,
        Day0ConfigurationService.class,
        HealRequestServiceFactory.class,
        LcmOpErrorManagementServiceImpl.class,
        LcmOpErrorProcessorFactory.class,
        DefaultLcmOpErrorProcessor.class
})
@MockBean({
        NfvoConfig.class,
        OssNodeService.class,
        GrantingService.class,
        ValuesFileComposer.class,
        MappingFileService.class,
        WorkflowServiceImpl.class,
        ReplicaDetailsMapper.class,
        ExtensionsMapper.class,
        LifeCycleRequestFactory.class,
        ClusterConfigServiceImpl.class,
        LifeCycleManagementHelper.class,
        DatabaseInteractionService.class,
        UsernameCalculationService.class,
        GrantingNotificationsConfig.class,
        WorkflowRoutingServicePassThrough.class,
        GrantingResourceDefinitionCalculation.class,
        HelmChartHelper.class,
        LifecycleOperationHelper.class,
        LcmOpSearchService.class,
        VnfInstanceService.class,
})
public class HealRequestHandlerTest {

    @Autowired
    private HealRequestHandler healRequestHandler;

    @MockBean
    private InstanceService instanceService;

    @MockBean
    private PackageService packageService;

    @MockBean
    private TerminateRequestHandler terminateRequestHandler;

    @MockBean
    private UrlValidator validator;

    @MockBean
    private CMPEnrollmentHelper cmpEnrollmentHelper;

    @Test
    public void validCauseType() {
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                                                    createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatNoException().isThrownBy(() -> healRequestHandler.validateHealConfiguration(vnfInstance, "Full Restore"));
    }

    @Test
    public void invalidCauseType() {
        VnfInstance instance = createVnfInstance("already-instantiated",
                                                 createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatThrownBy(() -> healRequestHandler.validateHealConfiguration(instance, "Partial Restore"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Cause type [Partial Restore] is not supported");
    }

    @Test
    public void noCauseType() {
        VnfInstance instance = createVnfInstance("already-instantiated",
                                                 createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatThrownBy(() -> healRequestHandler.validateHealConfiguration(instance, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Cause type [null] is not supported");
    }

    @Test
    public void validLcmOperationsConfigurations() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        when(packageService.getVnfd(any())).thenReturn(jsonObject);
        VnfInstance instance = createVnfInstance("already-instantiated",
                                                 createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatNoException().isThrownBy(() -> healRequestHandler.validateHealConfiguration(instance, "full restore"));
    }

    @Test
    public void invalidLcmOperationsConfigurations() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-invalid-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        when(packageService.getVnfd(any())).thenReturn(jsonObject);
        VnfInstance instance = createVnfInstance("already-instantiated",
                                                 createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatThrownBy(() -> healRequestHandler.validateHealConfiguration(instance, "full restore"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect lcm_operations_configuration for Heal Interface");
    }

    @Test
    public void missingLcmOperationsConfigurations() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-missing-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        when(packageService.getVnfd(any())).thenReturn(jsonObject);
        VnfInstance instance = createVnfInstance("already-instantiated",
                                                 createSupportedOperations(LCMOperationsEnum.HEAL));
        assertThatThrownBy(() -> healRequestHandler.validateHealConfiguration(instance, "full restore"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lcm_operations_configuration is mandatory for a Heal Interface");
    }

    @Test
    public void testExceptionThrownWhenbackupFileReferenceAndbackupNamePresent() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createSupportedOperations(LCMOperationsEnum.HEAL));

        when(packageService.getVnfd(any())).thenReturn(jsonObject);

        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                RESTORE_BACKUP_FILE_REFERENCE, "sftp://users@14BCP04/my-backup",
                RESTORE_PASSWORD, "",
                RESTORE_BACKUP_NAME, ""
        ));

        assertThatThrownBy(() -> {
            healRequestHandler.specificValidation(vnfInstance, vnfRequest);
        })
                .isInstanceOf(InvalidHealRequestException.class)
                .hasMessageContaining(String.format("%s and %s can not be present in the same HEAL Request",
                                                    RESTORE_BACKUP_NAME, RESTORE_BACKUP_FILE_REFERENCE));
    }

    @Test
    public void testCNFValidationIsPerformedWhenbackupFileReferencePresent() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createSupportedOperations(LCMOperationsEnum.HEAL));

        when(packageService.getVnfd(any())).thenReturn(jsonObject);

        when(validator.isValid("sftp://users@14BCP04/my-backup")).thenReturn(true);

        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                "restore.backupFileReference", "sftp://users@14BCP04/my-backup",
                "restore.password", ""
        ));

        assertThatThrownBy(() -> {
            healRequestHandler.specificValidation(vnfInstance, vnfRequest);
        })
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Password cannot be null or empty");
    }

    @Test
    public void testNegativeCNAValidationIsPerformedWhenBackupNameIsPresent() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createSupportedOperations(LCMOperationsEnum.HEAL));

        when(packageService.getVnfd(any())).thenReturn(jsonObject);

        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                "restore.backupName", "backUp",
                "restore.scope", ""
        ));

        assertThatThrownBy(() -> {
            healRequestHandler.specificValidation(vnfInstance, vnfRequest);
        })
                .isInstanceOf(InvalidHealRequestException.class)
                .hasMessageContaining("Invalid CNA Restore request. restore.scope is not present or an invalid value.");
    }

    @Test
    public void testFailHealSpecificValidation() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createNotSupportedOperations(LCMOperationsEnum.HEAL));

        when(packageService.getVnfd(any())).thenReturn(jsonObject);

        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                "restore.backupName", "backUp",
                "restore.scope", ""
        ));

        assertThatThrownBy(() -> healRequestHandler.specificValidation(vnfInstance, vnfRequest))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessageContaining("Operation heal is not supported for package test");
    }

    @Test
    public void testFailHealSpecificValidationWithErrorMessage() {
        String descriptorModel = getFile("heal/descriptor-model-heal-with-lcm-operations.json");
        JSONObject jsonObject = new JSONObject(descriptorModel);
        VnfInstance vnfInstance = createVnfInstance("already-instantiated",
                createNotSupportedOperationsWithError(
                        Map.of(LCMOperationsEnum.HEAL, "Heal validation error message")));

        when(packageService.getVnfd(any())).thenReturn(jsonObject);

        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                "restore.backupName", "backUp",
                "restore.scope", ""
        ));
        assertThatThrownBy(() -> healRequestHandler.specificValidation(vnfInstance, vnfRequest))
                .isInstanceOf(OperationNotSupportedException.class)
                .hasMessage("Operation heal is not supported for package test due to cause: Heal validation error message");
    }

    @Test
    public void testSendRequestWhenNamespaceIsRestrictedFail() {
        HealVnfRequest vnfRequest = getHealVnfRequest(Map.of(
                "restore.backupName", "backUp",
                "restore.scope", ""
        ));

        LifecycleOperation lifecycleOperation = new LifecycleOperation();
        lifecycleOperation.setOperationState(LifecycleOperationState.STARTING);
        VnfInstance vnfInstance = new VnfInstance();

        when(terminateRequestHandler.getType()).thenReturn(LifecycleOperationType.TERMINATE);
        doThrow(new NamespaceDeletionInProgressException("testMessage"))
                .when(terminateRequestHandler).setCleanUpResources(any(), any());

        assertThatThrownBy(() -> healRequestHandler
                .sendRequest(vnfInstance, lifecycleOperation, vnfRequest, null))
                .isInstanceOf(NamespaceDeletionInProgressException.class)
                .hasMessage("testMessage");

        assertThat(lifecycleOperation.getOperationState()).isEqualTo(LifecycleOperationState.FAILED);
    }

    private HealVnfRequest getHealVnfRequest(Map<String, String> additionalParameters) {
        HealVnfRequest vnfRequest = new HealVnfRequest();
        vnfRequest.setCause("full restore");
        vnfRequest.setAdditionalParams(additionalParameters);
        return vnfRequest;
    }

    private static VnfInstance createVnfInstance(String instanceId, List<OperationDetail> operationDetails) {
        final VnfInstance value = new VnfInstance();
        value.setInstantiationState(InstantiationState.INSTANTIATED);
        value.setVnfInstanceId(instanceId);
        value.setVnfPackageId("test");
        value.setSupportedOperations(operationDetails);
        return value;
    }

    private String getFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }
}
