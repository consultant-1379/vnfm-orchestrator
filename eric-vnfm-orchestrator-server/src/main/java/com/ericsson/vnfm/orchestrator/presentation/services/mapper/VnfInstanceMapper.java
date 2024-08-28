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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.ericsson.vnfm.orchestrator.model.ComponentStatusResponse;
import com.ericsson.vnfm.orchestrator.model.ComputeResource;
import com.ericsson.vnfm.orchestrator.model.InstantiatedVnfInfo;
import com.ericsson.vnfm.orchestrator.model.McioInfo;
import com.ericsson.vnfm.orchestrator.model.OwnerReference;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfo;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoBase;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoDeploymentStatefulSet;
import com.ericsson.vnfm.orchestrator.model.VimLevelAdditionalResourceInfoRel4;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfcResourceInfo;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.utils.ScalingUtils;
import com.ericsson.vnfm.orchestrator.utils.StringsConvertUtils;
import com.ericsson.vnfm.orchestrator.utils.UrlUtils;
import com.ericsson.vnfm.orchestrator.utils.Utility;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class VnfInstanceMapper {

    @Mapping(source = "vnfInstanceId", target = "id")
    @Mapping(source = "vnfDescriptorId", target = "vnfdId")
    @Mapping(source = "vnfProviderName", target = "vnfProvider")
    @Mapping(source = "vnfPackageId", target = "vnfPkgId")
    public abstract VnfInstanceResponse toOutputModel(VnfInstance vnfInstance);

    @Named("vnfInstanceResponseModel")
    @Mapping(source = "vnfInstance.vnfInstanceId", target = "id")
    @Mapping(source = "vnfInstance.vnfDescriptorId", target = "vnfdId")
    @Mapping(source = "vnfInstance.vnfProviderName", target = "vnfProvider")
    @Mapping(source = "vnfInstance.vnfPackageId", target = "vnfPkgId")
    @Mapping(source = "vnfInstance.clusterName", target = "clusterName")
    public abstract VnfInstanceResponse toOutputModelWithResourceInfo(VnfInstance vnfInstance,
                                                                      ComponentStatusResponse componentStatusResponse);

    public List<VnfInstanceResponse> toOutputModelWithResourceInfo(List<VnfInstance> vnfInstances,
                                                                   List<ComponentStatusResponse> componentStatusResponses) {
        List<VnfInstanceResponse> vnfInstanceResponses = vnfInstances.stream()
                .map(this::toOutputModel)
                .collect(Collectors.toList());
        mapResourceInfoList(componentStatusResponses, vnfInstances, vnfInstanceResponses);

        return vnfInstanceResponses;
    }

    @AfterMapping
    protected void toOutputModel(VnfInstance vnfInstance, @MappingTarget VnfInstanceResponse vnfInstanceResponse) {
        setInstantiatedVnfInfo(vnfInstance, vnfInstanceResponse);
        vnfInstanceResponse.setExtensions(
                StringsConvertUtils.mapIfNotEmpty(vnfInstance.getVnfInfoModifiableAttributesExtensions(), Utility::convertStringToJSONObj));
        vnfInstanceResponse.setMetadata(StringsConvertUtils.mapIfNotEmpty(vnfInstance.getMetadata(), Utility::convertStringToJSONObj));
        UrlUtils.updateVnfInstanceWithLinks(vnfInstanceResponse);
    }

    @AfterMapping
    protected void mapResourceInfo(VnfInstance vnfInstance, ComponentStatusResponse componentStatusResponse,
                                   @MappingTarget VnfInstanceResponse vnfInstanceResponse) {
        InstantiatedVnfInfo instantiatedVnfInfo = vnfInstanceResponse.getInstantiatedVnfInfo();
        String clusterName = vnfInstanceResponse.getClusterName();
        boolean isRel4 = vnfInstance.isRel4();

        final List<VnfcResourceInfo> vnfcResourceInfoList = buildVnfcResourceInfoList(componentStatusResponse, instantiatedVnfInfo, isRel4);
        final List<McioInfo> mcioInfoList = buildMcioInfoList(componentStatusResponse, clusterName, vnfcResourceInfoList);

        instantiatedVnfInfo.setVnfcResourceInfo(vnfcResourceInfoList);

        if (!CollectionUtils.isEmpty(mcioInfoList)) {
            instantiatedVnfInfo.setMcioInfo(mcioInfoList);
        }
    }

    private void mapResourceInfoList(ComponentStatusResponse componentStatusResponse, VnfInstanceResponse vnfInstanceResponse, boolean isRel4) {
        InstantiatedVnfInfo instantiatedVnfInfo = vnfInstanceResponse.getInstantiatedVnfInfo();
        String clusterName = vnfInstanceResponse.getClusterName();

        final List<VnfcResourceInfo> vnfcResourceInfoList = buildVnfcResourceInfoList(componentStatusResponse, instantiatedVnfInfo, isRel4);
        final List<McioInfo> mcioInfoList = buildMcioInfoList(componentStatusResponse, clusterName, vnfcResourceInfoList);

        instantiatedVnfInfo.setVnfcResourceInfo(vnfcResourceInfoList);

        if (!CollectionUtils.isEmpty(mcioInfoList)) {
            mcioInfoList.forEach(instantiatedVnfInfo::addMcioInfoItem);
        }
    }

    private void mapResourceInfoList(List<ComponentStatusResponse> componentStatusResponses,
                                     List<VnfInstance> vnfInstances,
                                     List<VnfInstanceResponse> vnfInstanceResponses) {

        for (final VnfInstance vnfInstance : vnfInstances) {
            mapResourceInfoList(flatComponentStatusResponsesForVnfInstance(componentStatusResponses, vnfInstance),
                                getVnfInstanceResponseById(vnfInstanceResponses, vnfInstance.getVnfInstanceId()),
                                vnfInstance.isRel4());
        }
    }

    private ComponentStatusResponse flatComponentStatusResponsesForVnfInstance(final List<ComponentStatusResponse> componentStatusResponses,
                                                                               final VnfInstance vnfInstance) {

        if (vnfInstance.getClusterName() == null) {
            return new ComponentStatusResponse();
        }

        final List<ComponentStatusResponse> vnfcResources = CollectionUtils.emptyIfNull(vnfInstance.getHelmCharts()).stream()
                .map(helmChart -> getComponentStatusForVnfInstance(componentStatusResponses, vnfInstance, helmChart.getReleaseName()))
                .toList();

        return flatComponentStatusResponses(vnfcResources);
    }

    private ComponentStatusResponse flatComponentStatusResponses(List<ComponentStatusResponse> componentStatusResponses) {
        ComponentStatusResponse allCombinedComponentResponse = new ComponentStatusResponse();
        allCombinedComponentResponse.setPods(new ArrayList<>());
        for (ComponentStatusResponse componentStatusResponse : componentStatusResponses) {
            allCombinedComponentResponse.getPods().addAll(componentStatusResponse.getPods());
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> deployments = componentStatusResponse.getDeployments();
            List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> statefulSets = componentStatusResponse.getStatefulSets();
            if (deployments != null) {
                deployments.forEach(allCombinedComponentResponse::addDeploymentsItem);
            }
            if (statefulSets != null) {
                statefulSets.forEach(allCombinedComponentResponse::addStatefulSetsItem);
            }
        }
        return allCombinedComponentResponse;
    }

    private ComponentStatusResponse getComponentStatusForVnfInstance(List<ComponentStatusResponse> componentStatusResponses,
                                                                     VnfInstance vnfInstance,
                                                                     String releaseName) {

        return componentStatusResponses.stream()
                .filter(status -> Objects.equals(releaseName, status.getReleaseName())
                        && Objects.equals(vnfInstance.getClusterName(), status.getClusterName())
                        && Objects.equals(vnfInstance.getVnfInstanceId(), status.getVnfInstanceId()))
                .findFirst()
                .orElse(new ComponentStatusResponse());
    }

    private VnfInstanceResponse getVnfInstanceResponseById(List<VnfInstanceResponse> vnfInstanceResponses, String id) {
        return vnfInstanceResponses.stream()
                .filter(current -> current.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private void setInstantiatedVnfInfo(final VnfInstance vnfInstance,
                                        final VnfInstanceResponse vnfInstanceResponse) {
        List<ScaleInfoEntity> scaleInfoEntity = vnfInstance.getScaleInfoEntity();
        InstantiatedVnfInfo instantiatedVnfInfo = new InstantiatedVnfInfo();
        instantiatedVnfInfo.setFlavourId("flavourId-not-supported");
        instantiatedVnfInfo.setVnfState(InstantiatedVnfInfo.VnfStateEnum.STOPPED); //must not be null as per ETSI spec
        if (scaleInfoEntity != null && !scaleInfoEntity.isEmpty()) {
            mapScaleVnfInfo(instantiatedVnfInfo, scaleInfoEntity);
        }
        vnfInstanceResponse.setInstantiatedVnfInfo(instantiatedVnfInfo);
    }

    private void mapScaleVnfInfo(InstantiatedVnfInfo instantiatedVnfInfo, final List<ScaleInfoEntity> scaleInfoEntityList) {
        List<ScaleInfo> scaleInfos = scaleInfoEntityList.stream().map(ScalingUtils::toScaleInfo).collect(Collectors.toList());
        instantiatedVnfInfo.setScaleStatus(scaleInfos);
    }

    private List<VnfcResourceInfo> buildVnfcResourceInfoList(ComponentStatusResponse componentStatusResponse,
                                                             InstantiatedVnfInfo instantiatedVnfInfo, boolean isRel4) {
        List<VimLevelAdditionalResourceInfo> vimLevelResourceInfoList = componentStatusResponse.getPods();
        List<VnfcResourceInfo> vnfcResourceInfoList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(vimLevelResourceInfoList)) {
            for (VimLevelAdditionalResourceInfo resourceInfo : vimLevelResourceInfoList) {
                final VnfcResourceInfo vnfcResourceInfo = buildVnfcResourceInfo(resourceInfo, isRel4);
                vnfcResourceInfoList.add(vnfcResourceInfo);
            }
            if (Utility.isRunning(vimLevelResourceInfoList)) {
                instantiatedVnfInfo.setVnfState(InstantiatedVnfInfo.VnfStateEnum.STARTED);
            }
        }

        return vnfcResourceInfoList;
    }

    private void mapMcioInfo(List<McioInfo> mcioInfoList,
                             List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> additionalResourceInfoList,
                             String clusterName, List<VnfcResourceInfo> vnfcResourceInfoLIst) {
        additionalResourceInfoList.stream()
                .map(additionalResourceInfo -> buildMcioInfo(additionalResourceInfo, clusterName, vnfcResourceInfoLIst))
                .forEach(mcioInfoList::add);
    }

    private List<McioInfo> buildMcioInfoList(ComponentStatusResponse componentStatusResponse, String clusterName, // NOSONAR
                                             List<VnfcResourceInfo> vnfcResourceInfoLIst) {
        List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> statefulSets = componentStatusResponse.getStatefulSets();
        List<VimLevelAdditionalResourceInfoDeploymentStatefulSet> deployments = componentStatusResponse.getDeployments();
        List<McioInfo> mcioInfoList = new ArrayList<>();

        if (statefulSets != null) {
            mapMcioInfo(mcioInfoList, statefulSets, clusterName, vnfcResourceInfoLIst);
        }
        if (deployments != null) {
            mapMcioInfo(mcioInfoList, deployments, clusterName, vnfcResourceInfoLIst);
        }

        return mcioInfoList;
    }

    private McioInfo buildMcioInfo(VimLevelAdditionalResourceInfoDeploymentStatefulSet additionalResourceInfo,
                                   String clusterName, List<VnfcResourceInfo> vnfcResourceInfoList) {
        String mcioId = additionalResourceInfo.getKind() + "/" + additionalResourceInfo.getName();
        McioInfo mcioInfo = new McioInfo()
                .mcioId(mcioId)
                .mcioName(additionalResourceInfo.getName())
                .mcioType(convertMcioType(additionalResourceInfo.getKind()))
                .cismId(clusterName)
                .mcioNamespace(additionalResourceInfo.getNamespace())
                .desiredInstances(additionalResourceInfo.getReplicas())
                .availableInstances(additionalResourceInfo.getAvailableReplicas());

        String vduId = vnfcResourceInfoList.stream()
                .filter(vnfcResourceInfo -> vnfcResourceInfo.getComputeResource().getResourceId().contains(additionalResourceInfo.getName()))
                .map(VnfcResourceInfo::getVduId)
                .findFirst()
                .orElse(StringUtils.EMPTY);
        mcioInfo.setVduId(vduId);

        return mcioInfo;
    }

    private McioInfo.McioTypeEnum convertMcioType(final String kind) {
        for (McioInfo.McioTypeEnum mcioType :McioInfo.McioTypeEnum.values()) {
            if (mcioType.toString().equalsIgnoreCase(kind)) {
                return mcioType;
            }
        }
        return null;
    }

    private VnfcResourceInfo buildVnfcResourceInfo(final VimLevelAdditionalResourceInfo additionalResourceInfo, boolean isRel4) {
        VnfcResourceInfo vnfcResourceInfo = new VnfcResourceInfo();
        vnfcResourceInfo.setId(additionalResourceInfo.getUid());
        final List<OwnerReference> ownerReferences = additionalResourceInfo.getOwnerReferences();
        if (!CollectionUtils.isEmpty(ownerReferences)) {
            vnfcResourceInfo.setVduId(additionalResourceInfo.getOwnerReferences().get(0).getUid());
        }
        ComputeResource computeResource = new ComputeResource();
        computeResource.setResourceId(additionalResourceInfo.getName());
        computeResource.setVimLevelResourceType("Pod");
        if (isRel4) {
            VimLevelAdditionalResourceInfoRel4 additionalResourceInfoRel4 = buildRel4VimLevelAdditionalResourceInfo(additionalResourceInfo);
            computeResource.setVimLevelAdditionalResourceInfo(additionalResourceInfoRel4);
        } else {
            computeResource.setVimLevelAdditionalResourceInfo(additionalResourceInfo);
        }
        vnfcResourceInfo.setComputeResource(computeResource);

        return vnfcResourceInfo;
    }

    private VimLevelAdditionalResourceInfoBase buildLegacyVimLevelAdditionalResourceInfo(VimLevelAdditionalResourceInfo additionalResourceInfo) {
        return new VimLevelAdditionalResourceInfoBase()
                .uid(additionalResourceInfo.getUid())
                .name(additionalResourceInfo.getName())
                .status(additionalResourceInfo.getStatus())
                .namespace(additionalResourceInfo.getNamespace())
                .annotations(additionalResourceInfo.getAnnotations())
                .ownerReferences(additionalResourceInfo.getOwnerReferences())
                .labels(additionalResourceInfo.getLabels());
    }

    private VimLevelAdditionalResourceInfoRel4 buildRel4VimLevelAdditionalResourceInfo(VimLevelAdditionalResourceInfo // NOSONAR
                                                                                               additionalResourceInfo) {
        VimLevelAdditionalResourceInfoBase additionalResourceInfoBase = buildLegacyVimLevelAdditionalResourceInfo(additionalResourceInfo);
        return new VimLevelAdditionalResourceInfoRel4()
                .persistentVolume(StringUtils.EMPTY)
                .hostname(additionalResourceInfo.getHostname())
                .additionalInfo(additionalResourceInfoBase);
    }
}