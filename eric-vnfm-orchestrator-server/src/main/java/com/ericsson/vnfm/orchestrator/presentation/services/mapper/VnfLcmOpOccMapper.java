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

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED_TEMP;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_OP_OCCS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.utils.Utility.getOperationParamsWithoutDay0Configuration;
import static com.ericsson.vnfm.orchestrator.utils.Utility.getProblemDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.am.shared.http.HttpUtility;
import com.ericsson.vnfm.orchestrator.model.URILink;
import com.ericsson.vnfm.orchestrator.model.VnfInfoModifications;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOcc;
import com.ericsson.vnfm.orchestrator.model.VnfLcmOpOccLinks;
import com.ericsson.vnfm.orchestrator.model.entity.ChangedInfo;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = LocalDateMapper.class)
public abstract class VnfLcmOpOccMapper {

    @Autowired
    private ObjectMapper mapper;

    @Named("vnfLcmOpOccModel")
    @Mapping(source = "lifecycleOperation.operationOccurrenceId", target = "id")
    @Mapping(source = "lifecycleOperation.vnfInstance.vnfInstanceId", target = "vnfInstanceId")
    @Mapping(source = "lifecycleOperation.automaticInvocation", target = "isAutomaticInvocation")
    @Mapping(source = "lifecycleOperation.cancelPending", target = "isCancelPending")
    @Mapping(target = "error", ignore = true)
    @Mapping(target = "changedInfo", ignore = true)
    public abstract VnfLcmOpOcc toInternalModel(LifecycleOperation lifecycleOperation, ChangedInfo changedInfo);

    public List<VnfLcmOpOcc> toInternalModel(Collection<LifecycleOperation> lifecycleOperations, Map<String, ChangedInfo> changedInfos) {
        List<VnfLcmOpOcc> vnfLcmOpOccs = new ArrayList<>();
        for (LifecycleOperation lifecycleOperation : lifecycleOperations) {
            final ChangedInfo changedInfo = changedInfos.get(lifecycleOperation.getOperationOccurrenceId());
            VnfLcmOpOcc vnfLcmOpOcc = toInternalModel(lifecycleOperation, changedInfo);
            vnfLcmOpOccs.add(vnfLcmOpOcc);
        }
        return vnfLcmOpOccs;
    }

    @AfterMapping
    protected void toInternalModel(LifecycleOperation lifecycleOperation, ChangedInfo changedInfo, @MappingTarget VnfLcmOpOcc vnfLcmOpOcc) {
        vnfLcmOpOcc.setOperationState(VnfLcmOpOcc.OperationStateEnum.fromValue(lifecycleOperation.getOperationState().name()));
        vnfLcmOpOcc.setOperation(VnfLcmOpOcc.OperationEnum.fromValue(lifecycleOperation.getLifecycleOperationType().name()));
        vnfLcmOpOcc.setError(getProblemDetails(lifecycleOperation.getError()));
        vnfLcmOpOcc.setLinks(getVnfLcmOpOccLinks(lifecycleOperation));
        vnfLcmOpOcc.setOperationParams(getOperationParams(lifecycleOperation));
        if (changedInfo != null) {
            vnfLcmOpOcc.setChangedInfo(getChangedInfoParams(lifecycleOperation, changedInfo));
        }
    }
    @SuppressWarnings("unchecked")
    private VnfInfoModifications getChangedInfoParams(final LifecycleOperation lifecycleOperation, final ChangedInfo changedInfo) {
        VnfInfoModifications vnfInfoModifications = new VnfInfoModifications();
        BeanUtils.copyProperties(changedInfo, vnfInfoModifications);
        if (StringUtils.isNotEmpty(lifecycleOperation.getVnfInfoModifiableAttributesExtensions())) {
            vnfInfoModifications.setExtensions(changedInfo.getVnfInfoModifiableAttributesExtensions());
        }
        if (changedInfo.getMetadata() != null) {
            try {
                Map<String, String> metaData = mapper.readValue(changedInfo.getMetadata(), Map.class);
                vnfInfoModifications.setMetadata(metaData);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(String.format("Unable to parse metadata string from lifecycle operation with ID: %s",
                                                lifecycleOperation.getOperationOccurrenceId()), e);
            }
        }
        return vnfInfoModifications;
    }

    private JsonNode getOperationParams(LifecycleOperation lifecycleOperations) {
        String operationParams = lifecycleOperations.getOperationParams();
        JsonNode preparedOperationParams = mapper.valueToTree(getOperationParamsWithoutDay0Configuration(operationParams));
        return Objects.isNull(operationParams) ? null : preparedOperationParams;
    }

    private static VnfLcmOpOccLinks getVnfLcmOpOccLinks(LifecycleOperation lifecycleOperations) {
        VnfLcmOpOccLinks vnfLcmOpOccLinks = new VnfLcmOpOccLinks();

        String baseOperationsLink = HttpUtility.getHostUrl() + LCM_OP_OCCS + lifecycleOperations.getOperationOccurrenceId();
        URILink selfLink = new URILink();
        selfLink.setHref(baseOperationsLink);
        vnfLcmOpOccLinks.setSelf(selfLink);

        URILink instanceLink = new URILink();
        instanceLink.setHref(HttpUtility.getHostUrl() + LCM_VNF_INSTANCES + lifecycleOperations.getVnfInstance().getVnfInstanceId());
        vnfLcmOpOccLinks.setVnfInstance(instanceLink);

        if (FAILED_TEMP.toString().equals(lifecycleOperations.getOperationState().toString())) {
            URILink fail = new URILink();
            fail.setHref(baseOperationsLink + "/fail");
            vnfLcmOpOccLinks.setFail(fail);
        }
        return vnfLcmOpOccLinks;
    }

}
