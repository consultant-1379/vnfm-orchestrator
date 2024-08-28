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

import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import static com.ericsson.vnfm.orchestrator.model.TaskName.DELETE_VNF_INSTANCE;
import static com.ericsson.vnfm.orchestrator.model.TaskName.SEND_NOTIFICATION;
import static com.ericsson.vnfm.orchestrator.model.TaskName.UPDATE_PACKAGE_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.DEFAULT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.SKIP_IMAGE_UPLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Errors.DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Instantiate.SITEBASIC_XML;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.EVNFM_PARAMS_FOR_WFS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode.TENANT_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENTITY_PROFILE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.OTP_VALIDITY_PERIOD_IN_MINUTES;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartHelper.getVnfLcmPrioritizedHelmPackageMap;
import static com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartPriorityHelper.getHelmChartsWithReversedTerminate;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.HelmChartUtils.toHelmChart;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logAdditionalParameters;
import static com.ericsson.vnfm.orchestrator.utils.LoggingUtils.logVnfInstance;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.createSitebasicFileFromOSSParams;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.dontLogPasswords;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.extendOssTopologySpecificParameters;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopology;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologyAsMap;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologyManagedElementId;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.getOssTopologySpecificParameters;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.mergeMaps;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.removeOssTopologyFromKey;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.removeOssTopologyFromKeyWithAdditionalAttributes;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.transformModelToFtlDataTypes;
import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.validateOtpValidityPeriod;
import static com.ericsson.vnfm.orchestrator.utils.RollbackPatternUtility.getDefaultDowngradePattern;
import static com.ericsson.vnfm.orchestrator.utils.ScalingUtils.getCommentedScaleInfo;
import static com.ericsson.vnfm.orchestrator.utils.SupportedOperationUtils.isOperationSupported;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertStringToJSONObj;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertXMLToJson;
import static com.ericsson.vnfm.orchestrator.utils.Utility.parseJson;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.getInstantiateName;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.getVnfdDowngradeParams;
import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.isDowngradeSupported;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.mergeJsonObjectAndStringAndWriteToValuesFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.ChangeVnfPackagePatternUtility;
import com.ericsson.am.shared.vnfd.NodeTemplateUtility;
import com.ericsson.am.shared.vnfd.VnfdUtility;
import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.model.Property;
import com.ericsson.am.shared.vnfd.model.lcmoperation.LCMOperationsEnum;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.HelmPackage;
import com.ericsson.am.shared.vnfd.model.nestedvnfd.NodeTemplate;
import com.ericsson.am.shared.vnfd.model.policies.Policies;
import com.ericsson.am.shared.vnfd.validation.ToscaSupportedOperationValidator;
import com.ericsson.vnfm.orchestrator.filters.VnfInstanceQuery;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.NfvoConfig;
import com.ericsson.vnfm.orchestrator.model.AddNodeToVnfInstanceByIdRequest;
import com.ericsson.vnfm.orchestrator.model.CreateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.DowngradeInfo;
import com.ericsson.vnfm.orchestrator.model.DowngradePackageInfo;
import com.ericsson.vnfm.orchestrator.model.RollbackInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChartBaseEntity;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.model.onboarding.PackageResponse;
import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeInfoInstanceIdNotPresentException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradeNotSupportedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.DowngradePackageDeletedException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.PackageDetailsNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UnprocessablePackageException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.UsageUpdateException;
import com.ericsson.vnfm.orchestrator.presentation.helper.AdditionalAttributesHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.HelmChartPriorityHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ExtensionsService;
import com.ericsson.vnfm.orchestrator.presentation.services.replicadetails.ReplicaCountCalculationService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.HelmChartRepository;
import com.ericsson.vnfm.orchestrator.repositories.ScaleInfoRepository;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.ericsson.vnfm.orchestrator.utils.ReplicaDetailsUtility;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

/**
 * An implementation to get the package details from Onboarding service and create vnf
 * instance model and persist it in database
 */
@Slf4j
@Service
public class InstanceService {

    private static final String DOWNGRADE_NOT_SUPPORTED_AS_DOWNGRADE_PACKAGE_IS_ERROR_MESSAGE = "Downgrade not " +
            "supported for instance id %s as the target downgrade package is no longer available";
    private static final String ALARM_SUPERVISION_ERROR_MESSAGE = "Setting Alarm Supervision to %s from ENM failure, please set the alarm "
            + "supervision from ENM manually:: %s";

    @Autowired
    private NfvoConfig nfvoConfig;

    @Value("${smallstack.application}")
    private String smallStackApplication;

    @Value("${orchestrator.suffixFirstCnfReleaseSchema}")
    private boolean suffixFirstCnfReleaseSchema;

    @Autowired
    private VnfInstanceQuery vnfInstanceQuery;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private HelmChartRepository helmChartRepository;

    @Autowired
    private ScaleInfoRepository scaleInfoRepository;

    @Autowired
    private ReplicaCountCalculationService replicaCountCalculationService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ExtensionsService extensionsService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdditionalAttributesHelper additionalAttributesHelper;

    @Autowired
    private VnfInstanceMapper vnfInstanceMapper;

    @Autowired
    private LifeCycleManagementHelper lifeCycleManagementHelper;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private ChangePackageOperationDetailsService changePackageOperationDetailsService;

    @Autowired
    private ChangeVnfPackageService changeVnfPackageService;

    private void updateVnfInstanceWithPackageInfo(final VnfInstance vnfInstance, final PackageResponse packageInfo) {
        vnfInstance.setVnfPackageId(packageInfo.getId());
        vnfInstance.setVnfdVersion(packageInfo.getVnfdVersion());
        vnfInstance.setVnfSoftwareVersion(packageInfo.getVnfSoftwareVersion());
        vnfInstance.setVnfProductName(packageInfo.getVnfProductName());
        vnfInstance.setVnfProviderName(packageInfo.getVnfProvider());
        vnfInstance.setVnfDescriptorId(packageInfo.getVnfdId());
        vnfInstance.setOverrideGlobalRegistry(!getSkipImageUploadValue(packageInfo));
        final List<OperationDetail> supportedOperations =
                ToscaSupportedOperationValidator.getVnfdSupportedOperations(packageInfo.getDescriptorModel());
        vnfInstance.setSupportedOperations(supportedOperations);
        vnfInstance.setHealSupported(isOperationSupported(vnfInstance, LCMOperationsEnum.HEAL.getOperation()));
        setIsDeployableModulesSupported(vnfInstance, packageInfo);
    }

    public void removeHelmChartEntriesFromInstance(final VnfInstance vnfInstance) {
        LOGGER.info("Removing helm charts from instance");
        for (final HelmChart helmChart : helmChartRepository.findByVnfInstance(vnfInstance)) {
            vnfInstance.getHelmCharts().remove(helmChart);
            LOGGER.info("Deleting {}", helmChart);
            helmChartRepository.delete(helmChart);
        }
    }

    @Transactional
    public void removeScaleEntriesFromInstance(final VnfInstance vnfInstance) {
        LOGGER.info("Removing scale entries from instance");
        for (final ScaleInfoEntity scaleInfoEntity : scaleInfoRepository.findAllByVnfInstance(vnfInstance)) {
            scaleInfoRepository.deleteById(scaleInfoEntity.getScaleInfoId());
        }
        vnfInstance.setScaleInfoEntity(null);
        databaseInteractionService.saveVnfInstanceToDB(vnfInstance);
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    protected static boolean getSkipImageUploadValue(PackageResponse packageInfo) {
        Map<String, Object> userDefinedData = (Map) packageInfo.getUserDefinedData();
        if (!CollectionUtils.isEmpty(userDefinedData)) {
            for (Map.Entry<String, Object> entry : userDefinedData.entrySet()) {
                if (SKIP_IMAGE_UPLOAD.equalsIgnoreCase(entry.getKey())) {
                    return toBooleanDefaultIfNull(Boolean.valueOf(entry.getValue().toString()), false);
                }
            }
        }
        return false;
    }

    /**
     * Persist the information about the instance
     *
     * @param packageInfo      general package information about the package
     * @param createVnfRequest the request from the user to create the identifier
     * @param policies         An Optional which may contain the policies section from the VNFD
     * @return the persisted instance
     */
    public VnfInstance createVnfInstanceEntity(final PackageResponse packageInfo,
                                               final CreateVnfRequest createVnfRequest,
                                               final Optional<Policies> policies) {
        try {
            final var vnfInstance = new VnfInstance();

            vnfInstance.setVnfInstanceName(createVnfRequest.getVnfInstanceName());
            vnfInstance.setVnfInstanceDescription(createVnfRequest.getVnfInstanceDescription());

            LOGGER.info("Updating VNF Instance with package info");
            updateVnfInstanceWithPackageInfo(vnfInstance, packageInfo);

            final var helmReleaseNameGenerator =
                    HelmReleaseNameGenerator.forInstantiate(vnfInstance, packageInfo, suffixFirstCnfReleaseSchema);
            vnfInstance.setHelmCharts(createHelmChartsFromPackageInfo(packageInfo, vnfInstance, helmReleaseNameGenerator));

            policies.ifPresent(setPoliciesInVnfInstance(vnfInstance));

            LOGGER.info("Vnf Instance to be persisted is {}", logVnfInstance(vnfInstance));

            setMetadataInVnfInstance(createVnfRequest, vnfInstance);

            setOssTopology(vnfInstance, packageInfo);
            setExtensions(vnfInstance, packageInfo.getDescriptorModel(), null, null);
            setSupportedOperations(vnfInstance, packageInfo.getId());
            setIsSol003Release4VersionParam(vnfInstance, packageInfo.getDescriptorModel());

            return vnfInstance;
        } catch (final Exception e) {
            throw new InternalRuntimeException(String.format("Unable to create vnf instance in db due to %s", e.getMessage()), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setMetadataInVnfInstance(final CreateVnfRequest createVnfRequest, VnfInstance vnfInstance) {
        try {
            Map<String, String> metadata;
            if (createVnfRequest.getMetadata() == null) {
                metadata = new HashMap<>();
                metadata.put(TENANT_NAME, "ECM");
            } else {
                metadata = (Map<String, String>) createVnfRequest.getMetadata();
                if (Strings.isNullOrEmpty(metadata.get(TENANT_NAME))) {
                    metadata.put(TENANT_NAME, "ECM");
                }
            }
            vnfInstance.setMetadata(mapper.writeValueAsString(metadata));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to create vnfInstance due to invalid metadata only string " +
                                                       "key value pair is supported", ex);
        }
    }

    private Consumer<Policies> setPoliciesInVnfInstance(final VnfInstance vnfInstance) {
        return policies -> {
            try {
                vnfInstance.setPolicies(mapper.writeValueAsString(policies));
                if (!CollectionUtils.isEmpty(policies.getAllScalingAspects())) {
                    List<ScaleInfoEntity> scaleInfoList =
                            policies.getAllScalingAspects()
                                    .entrySet()
                                    .stream()
                                    .flatMap(ScalingUtils::toAspectIds)
                                    .map(ScalingUtils::buildScaleInfoWithDefaultScaleLevel)
                                    .collect(toList());
                    scaleInfoList.forEach(scaleInfoEntity -> scaleInfoEntity.setVnfInstance(vnfInstance));
                    vnfInstance.setScaleInfoEntity(scaleInfoList);
                }
            } catch (JsonProcessingException e) {
                throw new InternalRuntimeException("Failed to write policies to database", e);
            }
        };
    }

    @Transactional
    public void deleteInstanceEntity(final String vnfInstanceId, boolean isNewOperation) {
        LOGGER.info("Deleting identifier: {}", vnfInstanceId);
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        if (isNewOperation) {
            databaseInteractionService.checkLifecycleInProgress(vnfInstanceId);
        }

        if (!vnfInstance.getInstantiationState().equals(InstantiationState.NOT_INSTANTIATED)) {
            throw new IllegalStateException(String.format(
                    "Conflicting resource state - VNF instance resource for VnfInstanceId %s is currently NOT in the NOT_INSTANTIATED state.",
                    vnfInstanceId));
        }

        deleteAssociationBetweenPackageAndVnfInstance(vnfInstance.getVnfPackageId(), vnfInstance.getVnfInstanceId(), false);
        databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstanceId, UPDATE_PACKAGE_STATE);

        try {
            databaseInteractionService.deleteVnfInstance(vnfInstance);
            databaseInteractionService.deleteAllClusterConfigInstancesByInstanceId(vnfInstance.getVnfInstanceId());

            //already deleted in case of terminate operation
            if (isNewOperation) {
                databaseInteractionService.deleteInstanceDetailsByVnfInstanceId(vnfInstance.getVnfInstanceId());
            }

            databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstanceId, DELETE_VNF_INSTANCE);
        } catch (DataAccessException ex) {
            throw new InternalRuntimeException("Unable to delete vnf instance from database", ex);
        }

        notificationService.sendVnfIdentifierDeletionEvent(vnfInstanceId);
        databaseInteractionService.deleteTasksByVnfInstanceAndTaskName(vnfInstanceId, SEND_NOTIFICATION);
    }

    public VnfInstance createTempInstanceForUpgradeOperation(final VnfInstance vnfInstance,
                                                             final PackageResponse packageInfo,
                                                             final Optional<Policies> policies) {

        final var tempInstance = new VnfInstance();

        BeanUtils.copyProperties(vnfInstance,
                                 tempInstance,
                                 "helmCharts",
                                 "policies",
                                 "resourceDetails",
                                 "scaleInfoEntity",
                                 "tempInstance",
                                 "helmNoHooks",
                                 "cleanUpResources");

        updateVnfInstanceWithPackageInfo(tempInstance, packageInfo);

        final var helmReleaseNameGenerator =
                HelmReleaseNameGenerator.forUpgrade(tempInstance, packageInfo, vnfInstance.getHelmCharts());
        tempInstance.setHelmCharts(createHelmChartsFromPackageInfo(packageInfo, tempInstance, helmReleaseNameGenerator));

        tempInstance.setScaleInfoEntity(new ArrayList<>());
        policies.ifPresent(setPoliciesInVnfInstance(tempInstance));
        //Below if condition should not be removed. This sets the default resources details if persistScaleInfo is
        //set to false
        if (policies.isPresent()) {
            tempInstance.setResourceDetails(replicaCountCalculationService.getResourceDetails(tempInstance));
        }

        setSupportedOperations(tempInstance, packageInfo.getId());
        setIsSol003Release4VersionParam(tempInstance, packageInfo.getDescriptorModel());

        return tempInstance;
    }

    public Page<VnfInstance> getVnfInstancePage(String filter, Pageable pageable) {
        Page<VnfInstance> vnfInstancePage = StringUtils.isEmpty(filter) ? databaseInteractionService.getAllVnfInstances(pageable) :
                getAllVnfInstanceWithFilter(filter, pageable);
        final List<VnfInstance> vnfInstances = vnfInstancePage.getContent();
        databaseInteractionService.fetchAssociationForVnfInstances(vnfInstances, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);
        databaseInteractionService.fetchAssociationForVnfInstances(vnfInstances, HelmChart.class, VnfInstance_.helmCharts);
        return vnfInstancePage;
    }

    public Page<VnfInstance> getAllVnfInstanceWithFilter(String filter, Pageable pageable) {
        return vnfInstanceQuery.getPageWithFilter(filter, pageable);
    }

    @Transactional
    public void createAndSaveAssociationBetweenPackageAndVnfInstance(String appPkgId, String instanceId, boolean isInUse) {
        try {
            packageService.updateUsageState(appPkgId, instanceId, isInUse);
        } catch (Exception e) {
            databaseInteractionService.deleteByVnfInstanceId(instanceId);
            throw new InternalRuntimeException(e.getMessage(), e);
        }
    }

    public void deleteAssociationBetweenPackageAndVnfInstance(String appPkgId, String instanceId, boolean isInUse) {
        packageService.updateUsageState(appPkgId, instanceId, isInUse);
    }

    public void updateAssociationBetweenPackageAndVnfInstanceForFailedUpgrade(final VnfInstance actualInstance) {
        final var upgradedInstance = parseJson(actualInstance.getTempInstance(), VnfInstance.class);
        final var targetPackageId = upgradedInstance.getVnfPackageId();

        try {
            updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(actualInstance.getVnfPackageId(),
                                                                             targetPackageId,
                                                                             targetPackageId,
                                                                             actualInstance.getVnfInstanceId(),
                                                                             false);
        } catch (Exception e) {
            LOGGER.warn("Update usage state api failed", e);
        }
    }

    public void updateAssociationBetweenPackageAndVnfInstanceForUpgradeOperation(String sourcePackageId,
                                                                                 String targetPackageId,
                                                                                 String updatePackage,
                                                                                 String instanceId,
                                                                                 boolean inUse) {
        if (!sourcePackageId.equals(targetPackageId)) {
            try {
                packageService.updateUsageState(updatePackage, instanceId, inUse);
            } catch (InternalRuntimeException ire) {
                throw new UsageUpdateException(ire);
            }
        }
    }

    public void setOssTopology(final VnfInstance vnfInstance, final PackageResponse packageResponse) {
        JSONObject vnfdDescriptorModel = new JSONObject(packageResponse.getDescriptorModel());
        String instantiateNameFromVnfd = getInstantiateNameFromVnfd(vnfdDescriptorModel);
        Map<String, PropertiesModel> ossTopology = new HashMap<>();
        if (StringUtils.isNotEmpty(instantiateNameFromVnfd)) {
            ossTopology = getOssTopology(vnfdDescriptorModel, instantiateNameFromVnfd);
        }
        vnfInstance.setOssTopology(new JSONObject(ossTopology).toString());
    }

    @VisibleForTesting
    static String getInstantiateNameFromVnfd(final JSONObject vnfdDescriptorModel) {
        String instantiateName = null;
        try {
            instantiateName = getInstantiateName(vnfdDescriptorModel);
        } catch (IOException e) {
            LOGGER.warn("Error retrieving instantiate inputs type", e);
        }
        return instantiateName;
    }

    public Map<String, Object> extractOssTopologyFromParams(VnfInstance vnfInstance,
                                                            AddNodeToVnfInstanceByIdRequest request) {

        final String message = logAdditionalParameters(request);
        LOGGER.info("Extract Oss topology for instanceId {} with additionalParams {} ", vnfInstance.getVnfInstanceId(), message);

        Map<String, PropertiesModel> ossTopologyFromInstantiate = getOssTopologyAsMap(vnfInstance.getInstantiateOssTopology(), PropertiesModel.class);
        LOGGER.info("Oss topology data from VNFD {}", dontLogPasswords(ossTopologyFromInstantiate));

        if (addNodeRequestProvided(request)) {
            Map<String, Object> additionalParams = convertAddNodeAdditionalParametersRequestToMap(request);

            if (additionalParamsProvided(additionalParams)) {
                mergeMaps(ossTopologyFromInstantiate, removeOssTopologyFromKeyWithAdditionalAttributes(additionalParams));
            }
        }

        Map<String, Object> addNodeFtlData = transformModelToFtlDataTypes(ossTopologyFromInstantiate);
        Utility.setManageElementIdIfNotPresent(addNodeFtlData, vnfInstance);
        setInstanceDetailsToAddNode(vnfInstance, addNodeFtlData);
        setVnfmNameBasedOnNfvo(addNodeFtlData);
        LOGGER.info("Oss topology data from VNFD merged with additional params: {}", logAdditionalParameters(addNodeFtlData));
        vnfInstance.setAddNodeOssTopology(new JSONObject(addNodeFtlData).toString());
        return addNodeFtlData;
    }

    private Map<String, Object> convertAddNodeAdditionalParametersRequestToMap(AddNodeToVnfInstanceByIdRequest additionalParams) {
        return mapper.convertValue(additionalParams, new TypeReference<>() {
        });
    }

    private void setVnfmNameBasedOnNfvo(Map<String, Object> addNodeFtlData) {
        addNodeFtlData.put(AddNode.SMALL_STACK_APPLICATION, smallStackApplication);

        if (nfvoConfig.isEnabled() && StringUtils.isNotEmpty(nfvoConfig.getVnfmName())) {
            LOGGER.info("Using VNFM with name: {} ", nfvoConfig.getVnfmName());
            addNodeFtlData.put(AddNode.VNFM_NAME, nfvoConfig.getVnfmName());
        }
    }

    private static void setInstanceDetailsToAddNode(VnfInstance vnfInstance, Map<String, Object> addNodeFtlData) {
        addNodeFtlData.put(AddNode.VNF_INSTANCE_ID, vnfInstance.getVnfInstanceId());
        if (StringUtils.isNotEmpty(vnfInstance.getMetadata())) {
            Map metadata = convertStringToJSONObj(vnfInstance.getMetadata());
            addNodeFtlData.put(AddNode.TENANT, metadata.get(AddNode.TENANT_NAME));
        }
    }

    public Map<String, Object> extractOssTopologyFromParams(VnfInstance vnfInstance) {

        Map<String, PropertiesModel> ossTopologyFromInstantiate = getOssTopologyAsMap(
                vnfInstance.getInstantiateOssTopology(), PropertiesModel.class);

        Map<String, Object> addNodeFtlData = transformModelToFtlDataTypes(ossTopologyFromInstantiate);
        setInstanceDetailsToAddNode(vnfInstance, addNodeFtlData);
        setVnfmNameBasedOnNfvo(addNodeFtlData);
        Utility.setManageElementIdIfNotPresent(addNodeFtlData, vnfInstance);
        vnfInstance.setAddNodeOssTopology(new JSONObject(addNodeFtlData).toString());
        return addNodeFtlData;
    }

    private static boolean addNodeRequestProvided(final AddNodeToVnfInstanceByIdRequest request) {
        return request != null;
    }

    private static boolean additionalParamsProvided(final Map<String, Object> additionalParams) {
        return !CollectionUtils.isEmpty(additionalParams);
    }

    public Map<String, Object> extractOssTopologyFromValuesYamlMap(VnfInstance vnfInstance, Map<String, Object> valuesYamlMap) {
        LOGGER.info("Extract Oss topology for instanceId {}", vnfInstance.getVnfInstanceId());
        Map<String, PropertiesModel> ossTopologyFromInstantiate = getOssTopologyAsMap(vnfInstance.getInstantiateOssTopology(), PropertiesModel.class);
        mergeMaps(ossTopologyFromInstantiate, removeOssTopologyFromKey(valuesYamlMap));
        Map<String, Object> addNodeFtlData = transformModelToFtlDataTypes(ossTopologyFromInstantiate);
        Utility.setManageElementIdIfNotPresent(addNodeFtlData, vnfInstance);
        setInstanceDetailsToAddNode(vnfInstance, addNodeFtlData);
        setVnfmNameBasedOnNfvo(addNodeFtlData);
        vnfInstance.setAddNodeOssTopology(new JSONObject(addNodeFtlData).toString());
        return addNodeFtlData;
    }

    public void addCommandResultToInstance(final VnfInstance instance, final EnmOperationEnum operation) {
        if (operation.equals(ADD_NODE) || operation.equals(DELETE_NODE) && operation.getAddedToOss() != null) {
            instance.setAddedToOss(operation.getAddedToOss());
        }
        if (operation.equals(ENABLE_ALARM_SUPERVISION) || operation.equals(DISABLE_ALARM_SUPERVISION) && operation.getSetValue() != null) {
            instance.setAlarmSupervisionStatus(operation.getSetValue());
        }
    }

    public static String getManagedElementId(final VnfInstance vnfInstance) {
        Optional<Object> managedElementId = getOssTopologyManagedElementId(vnfInstance.getAddNodeOssTopology());
        if (managedElementId.isPresent() && managedElementId.get() instanceof Map) {
            Object managedIdDetails = managedElementId.get();
            return (String) ((Map) managedIdDetails).get(DEFAULT);
        } else if (managedElementId.isPresent()) {
            return (String) managedElementId.get();
        }
        throw new NotFoundException(
                "Vnf instance with id " + vnfInstance.getVnfInstanceId() + " does not have an associated managedElementId");
    }

    private LifecycleOperation getOperationFromVnfInstance(final VnfInstance vnfInstance) {
        return databaseInteractionService.getLifecycleOperation(vnfInstance.getOperationOccurrenceId());
    }

    public void setAlarmStatusOnInstance(final VnfInstance vnfInstance, final EnmOperationEnum operationEnum) {
        vnfInstance.setAlarmSupervisionStatus(operationEnum.getSetValue());
    }

    public void persistAlarmErrorMessage(final VnfInstance vnfInstance, final EnmOperationEnum operationEnum, String errorMessage) {
        LifecycleOperation operation = getOperationFromVnfInstance(vnfInstance);
        operation.setSetAlarmSupervisionErrorMessage(String.format(ALARM_SUPERVISION_ERROR_MESSAGE, operationEnum.getSetValue(), errorMessage));
        databaseInteractionService.persistLifecycleOperation(operation);
    }

    static List<HelmChart> createHelmChartsFromPackageInfo(final PackageResponse packageInfo,
                                                           final VnfInstance vnfInstance,
                                                           final HelmReleaseNameGenerator helmReleaseNameGenerator) {

        LOGGER.info("Creating a mapping of helm chart to release name(s)");

        final var helmCharts = packageInfo.getHelmPackageUrls().stream()
                .map(helmPackage -> toHelmChart(helmPackage, vnfInstance, helmReleaseNameGenerator))
                .collect(toList());

        return getPrioritizedHelmCharts(helmCharts, packageInfo, vnfInstance);
    }

    static List<HelmChart> getPrioritizedHelmCharts(List<HelmChart> defaultHelmCharts,
                                                    PackageResponse packageResponse,
                                                    final VnfInstance vnfInstance) {
        Map<LCMOperationsEnum, List<HelmPackage>> vnfLcmPrioritizedHelmPackageMap = getVnfLcmPrioritizedHelmPackageMap(packageResponse);

        for (HelmChart helmChartToChange : defaultHelmCharts) {
            Map<LifecycleOperationType, Integer> lifecycleOperationTypeIntegerMap =
                    HelmChartPriorityHelper.calculatePriority(helmChartToChange, vnfLcmPrioritizedHelmPackageMap);
            helmChartToChange.setOperationChartsPriority(lifecycleOperationTypeIntegerMap);
        }
        return getHelmChartsWithReversedTerminate(vnfInstance, defaultHelmCharts)
                .stream()
                .sorted(Comparator.comparing(HelmChartBaseEntity::getPriority))
                .collect(toList());
    }

    public Path getCombinedAdditionalValuesWithoutEVNFMParams(final VnfInstance instance) {
        Map<String, Object> mergedParams = additionalAttributesHelper.getCombinedAdditionalValuesMap(instance);
        if (CollectionUtils.isEmpty(mergedParams)) {
            return null;
        }
        EVNFM_PARAMS.forEach(mergedParams.keySet()::remove);
        EVNFM_PARAMS_FOR_WFS.forEach(mergedParams.keySet()::remove);

        List<ScaleInfoEntity> scaleInfoEntities = instance.getScaleInfoEntity();
        return mergeJsonObjectAndStringAndWriteToValuesFile(mergedParams, getCommentedScaleInfo(scaleInfoEntities));
    }

    @Transactional
    public DowngradeInfo getDowngradePackagesInfo(final String vnfId) {
        VnfInstance sourceVnfInstance;
        try {
            sourceVnfInstance = databaseInteractionService.getVnfInstance(vnfId);
        } catch (NotFoundException nfe) {
            throw new DowngradeInfoInstanceIdNotPresentException(vnfId + VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE, nfe);
        }

        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfInstance(sourceVnfInstance);
        LifecycleOperation lastVnfdChangingOperation = lcmOpSearchService.searchLastChangingOperation(
                        sourceVnfInstance, changePackageOperationDetails)
                .orElseThrow(() -> new DowngradeNotSupportedException(
                        String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, sourceVnfInstance.getVnfInstanceId())));
        PackageResponse targetPackageInfo = getTargetDowngradePackage(lastVnfdChangingOperation.getSourceVnfdId(), vnfId);
        JSONObject vnfd = packageService.getVnfd(sourceVnfInstance.getVnfPackageId());

        if (!isDowngradeSupported(vnfd, sourceVnfInstance, targetPackageInfo.getVnfdId(),
                                  targetPackageInfo.getVnfSoftwareVersion())) {
            throw new DowngradeNotSupportedException(String.format(DOWNGRADE_NOT_SUPPORTED_ERROR_MESSAGE, vnfId));
        }

        DowngradePackageInfo sourceDowngradePackageInfoDto = new DowngradePackageInfo();
        sourceDowngradePackageInfoDto.setPackageId(sourceVnfInstance.getVnfPackageId());
        sourceDowngradePackageInfoDto.setPackageVersion(sourceVnfInstance.getVnfdVersion());
        sourceDowngradePackageInfoDto.setVnfdId(sourceVnfInstance.getVnfDescriptorId());

        DowngradePackageInfo targetDowngradePackageInfoDto = new DowngradePackageInfo();
        targetDowngradePackageInfoDto.setPackageId(targetPackageInfo.getId());
        targetDowngradePackageInfoDto.setPackageVersion(targetPackageInfo.getVnfdVersion());
        targetDowngradePackageInfoDto.setVnfdId(targetPackageInfo.getVnfdId());

        DowngradeInfo downgradeInfo = new DowngradeInfo();
        downgradeInfo.setSourceDowngradePackageInfo(sourceDowngradePackageInfoDto);
        downgradeInfo.setTargetDowngradePackageInfo(targetDowngradePackageInfoDto);

        Map<String, Property> vnfdDowngradeParams = getVnfdDowngradeParams(
                packageService.getVnfd(sourceVnfInstance.getVnfPackageId()),
                sourceDowngradePackageInfoDto.getVnfdId(),
                targetDowngradePackageInfoDto.getVnfdId(),
                sourceVnfInstance.getVnfSoftwareVersion(),
                targetPackageInfo.getVnfSoftwareVersion()
        );

        downgradeInfo.setAdditionalParameters(vnfdDowngradeParams);

        return downgradeInfo;
    }

    @Transactional
    public RollbackInfo getRollbackInfo(final String vnfId) {
        VnfInstance instance = databaseInteractionService.getVnfInstance(vnfId);
        if (instance.getTempInstance() == null) {
            throw new IllegalArgumentException(String.format("Unable to get the rollback info as temp instance not found for %s", vnfId));
        }
        VnfInstance tempInstance = parseJson(instance.getTempInstance(), VnfInstance.class);
        return new RollbackInfo().sourcePackageVersion(instance.getVnfdVersion()).destinationPackageVersion(tempInstance.getVnfdVersion());
    }

    public List<MutablePair<String, String>> getHelmChartCommandRollbackFailurePattern(LifecycleOperation operation,
                                                                                       VnfInstance tempInstance, HelmChart helmChart) {
        JSONObject vnfd = packageService.getVnfd(tempInstance.getVnfPackageId());
        return ChangeVnfPackagePatternUtility
                .getRollbackPatternAtFailureForHelmChart(helmChart.getHelmChartArtifactKey(),
                                                         vnfd,
                                                         tempInstance.getVnfDescriptorId(),
                                                         operation.getVnfInstance().getVnfDescriptorId());
    }

    public List<MutablePair<String, String>> getHelmChartCommandRollbackPattern(final VnfInstance vnfInstance,
                                                                                final VnfInstance tempInstance) {
        JSONObject vnfd = packageService.getVnfd(vnfInstance.getVnfPackageId());
        List<MutablePair<String, String>> helmChartCommandList =
                ChangeVnfPackagePatternUtility.getPattern(
                        vnfd,
                        vnfInstance.getVnfDescriptorId(),
                        tempInstance.getVnfDescriptorId(),
                        vnfInstance.getVnfSoftwareVersion(),
                        tempInstance.getVnfSoftwareVersion(),
                        ChangeVnfPackagePatternUtility.ROLLBACK_PATTERN);
        if (helmChartCommandList.isEmpty()) {
            return getDefaultDowngradePattern(vnfInstance, tempInstance);
        }
        return helmChartCommandList;
    }

    public List<MutablePair<String, String>> getHelmChartCommandUpgradePattern(VnfInstance vnfInstance, VnfInstance tempInstance) {
        JSONObject vnfd = packageService.getVnfd(tempInstance.getVnfPackageId());
        return ChangeVnfPackagePatternUtility.getPattern(
                vnfd,
                vnfInstance.getVnfDescriptorId(),
                tempInstance.getVnfDescriptorId(),
                vnfInstance.getVnfSoftwareVersion(),
                tempInstance.getVnfSoftwareVersion(),
                ChangeVnfPackagePatternUtility.UPGRADE_PATTERN);
    }

    private PackageResponse getTargetDowngradePackage(final String vnfdId, final String vnfId) {
        try {
            return packageService.getPackageInfo(vnfdId);
        } catch (PackageDetailsNotFoundException | UnprocessablePackageException ex) {
            throw new DowngradePackageDeletedException(String.format(DOWNGRADE_NOT_SUPPORTED_AS_DOWNGRADE_PACKAGE_IS_ERROR_MESSAGE, vnfId), ex);
        }
    }

    public static void setInstanceWithSitebasicFile(final VnfInstance instance, final Map<String, Object> additionalParams) {
        if (CollectionUtils.isEmpty(additionalParams)) {
            return;
        }

        validateOtpValidityPeriod(additionalParams);

        Map<String, Object> ossParams = getOssTopologySpecificParameters(additionalParams);
        extendOssTopologySpecificParameters(ossParams, additionalParams,
                                            OTP_VALIDITY_PERIOD_IN_MINUTES,
                                            ENTITY_PROFILE_NAME);

        String sitebasicFileAsString = additionalParams.containsKey(SITEBASIC_XML) ?
                (String) additionalParams.get(SITEBASIC_XML) :
                createSitebasicFileFromOSSParams(instance, ossParams);

        if (!Strings.isNullOrEmpty(sitebasicFileAsString)) {
            convertXMLToJson(sitebasicFileAsString, SITEBASIC_XML);
            instance.setSitebasicFile(sitebasicFileAsString);
        }
    }

    public void setExtensions(final VnfInstance tempInstance,
                              final String descriptorModel,
                              final Map<String, Object> requestExtensions,
                              final String sourceVnfdId) {
        extensionsService.setDefaultExtensions(tempInstance, descriptorModel);
        if (requestExtensions != null) {
            extensionsService.validateVnfControlledScalingExtension(requestExtensions, tempInstance.getPolicies());
            extensionsService.updateInstanceWithExtensionsInRequest(requestExtensions, tempInstance);
        }

        updateExtensionsIfDowngrade(tempInstance, sourceVnfdId);

        if (StringUtils.isNotEmpty(tempInstance.getVnfInfoModifiableAttributesExtensions())) {
            Map<String, Object> extensions = convertStringToJSONObj(tempInstance.getVnfInfoModifiableAttributesExtensions());
            extensionsService.validateVnfControlledScalingExtension(extensions, tempInstance.getPolicies());
            extensionsService.validateDeployableModulesExtension(extensions, tempInstance, descriptorModel);
        }
    }

    private void updateExtensionsIfDowngrade(final VnfInstance vnfInstance, final String sourceVnfdId) {
        if (sourceVnfdId == null) {
            return;
        }
        final Optional<LifecycleOperation> targetDowngradeOperation =
                changeVnfPackageService.getSuitableTargetDowngradeOperation(vnfInstance,
                                                                            sourceVnfdId,
                                                                            vnfInstance.getVnfDescriptorId());
        if (targetDowngradeOperation.isPresent()) {
            final String extensions = targetDowngradeOperation.get().getVnfInfoModifiableAttributesExtensions();
            vnfInstance.setVnfInfoModifiableAttributesExtensions(extensions);
        }
    }

    public void setIsSol003Release4VersionParam(final VnfInstance vnfInstance, final String descriptorModel) {
        JSONObject vnfd = VnfdUtility.validateYamlAndConvertToJsonObject(descriptorModel);
        boolean isRel4 = VnfdUtility.isRel4Vnfd(vnfd);
        vnfInstance.setRel4(isRel4);
    }

    public void setSupportedOperations(VnfInstance vnfInstance, String vnfPackageId) {
        vnfInstance.setSupportedOperations(packageService.getSupportedOperations(vnfPackageId));
    }

    public void setHelmClientVersion(VnfInstance vnfInstance, Map<String, Object> additionalParams) {
        vnfInstance.setHelmClientVersion(lifeCycleManagementHelper.validateAndGetHelmClientVersion(additionalParams));
    }

    public void setIsDeployableModulesSupported(VnfInstance vnfInstance, PackageResponse packageInfo) {
        final NodeTemplate nodeTemplate = ReplicaDetailsUtility.getNodeTemplate(packageInfo.getDescriptorModel());
        vnfInstance.setDeployableModulesSupported(NodeTemplateUtility.isDeployableModulesSupported(nodeTemplate));
    }
}
