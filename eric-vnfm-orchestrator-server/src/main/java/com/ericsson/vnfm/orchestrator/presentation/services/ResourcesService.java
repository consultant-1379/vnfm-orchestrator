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

import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.Errors.VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.filters.VnfResourceViewQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToInstancesQuery;
import com.ericsson.vnfm.orchestrator.filters.VnfResourcesToLifecycleOperationQuery;
import com.ericsson.vnfm.orchestrator.model.ResourceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfResource;
import com.ericsson.vnfm.orchestrator.model.VnfResourceLifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.VnfResourceOperationDetail;
import com.ericsson.vnfm.orchestrator.model.entity.ChangePackageOperationDetails;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView_;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.helper.LifecycleOperationHelper;
import com.ericsson.vnfm.orchestrator.presentation.helper.ScalingAspectsHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ResourceViewResponseMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfInstanceResourceResponseMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfLifecycleMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.VnfResourceMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfResourceViewRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ResourcesService {

    private static final Map<String, Boolean> IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER = new HashMap<>();
    private static final String FILTER_SEPARATOR = ";";
    private static final String INSTANCE_ID_FILTER = ";(eq,lcmOperationDetails/vnfInstanceId,%s)";
    private static final Set<String> IS_DEFAULT_LIFECYCLE_OPERATION_FILTERS =
            IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.keySet();

    static {
        IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.put("(eq,lcmOperationDetails" +
                                                                    "/currentLifecycleOperation,true)", true);
        IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.put("(eq,lcmOperationDetails" +
                                                                    "/currentLifecycleOperation,false)", false);
        IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.put("(neq,lcmOperationDetails" +
                                                                    "/currentLifecycleOperation,true)", false);
        IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.put("(neq,lcmOperationDetails" +
                                                                    "/currentLifecycleOperation,false)", true);
    }

    @Autowired
    private VnfLifecycleMapper vnfLifecycleMapper;

    @Autowired
    private ResourceViewResponseMapper resourceViewResponseMapper;

    @Autowired
    private VnfResourceMapper vnfResourceMapper;

    @Autowired
    private VnfInstanceResourceResponseMapper instanceResourceResponseMapper;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private VnfResourcesToLifecycleOperationQuery lifecycleOperationQuery;

    @Autowired
    private VnfResourcesToInstancesQuery vnfInstanceQuery;

    @Autowired
    private VnfResourceViewQuery vnfResourceViewQuery;

    @Autowired
    private VnfResourceViewRepository vnfResourceViewRepository;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    private LifecycleOperationHelper lifecycleOperationHelper;

    @Autowired
    private PackageService packageService;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private ChangePackageOperationDetailsService changePackageOperationDetailsService;

    @Autowired
    private ScalingAspectsHelper scalingAspectsHelper;

    // Self injection to call transactional methods from itself
    private ResourcesService resourcesServiceProxy; //NOSONAR

    public VnfInstance getInstance(final String instanceId) {
        if (StringUtils.isEmpty(instanceId)) {
            throw new IllegalArgumentException("Instance Id can't be null or empty");
        } else {
            try {
                return databaseInteractionService.getVnfInstance(instanceId);
            } catch (NotFoundException nfe) {
                throw new NotFoundException(instanceId + VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE, nfe);
            }
        }
    }

    public VnfInstance getInstanceWithoutOperations(final String instanceId) {
        if (StringUtils.isEmpty(instanceId)) {
            throw new IllegalArgumentException("Instance Id can't be null or empty");
        } else {
            try {
                return databaseInteractionService.getVnfInstanceWithoutOperations(instanceId);
            } catch (NotFoundException nfe) {
                throw new NotFoundException(instanceId + VNF_RESOURCES_NOT_PRESENT_ERROR_MESSAGE, nfe);
            }
        }
    }

    public List<VnfResource> getAllResourcesWithFilter(final String filters) {
        final String vnfInstanceFilterPresent = vnfInstanceQuery.filterPresent(filters);
        final String vnfInstanceFilterNotPresent = vnfInstanceQuery.filterNotPresent(filters);
        String lifecycleFilterPresent = null;
        Boolean shouldKeepCurrentLifeCycleOperation = null;
        if (!StringUtils.isEmpty(vnfInstanceFilterNotPresent)) {
            lifecycleFilterPresent = lifecycleOperationQuery.filterPresent(vnfInstanceFilterNotPresent);
            String isDefaultLifecycleFilterPresent = isDefaultLifecycleFilterPresent(
                    lifecycleOperationQuery.filterNotPresent(vnfInstanceFilterNotPresent));
            shouldKeepCurrentLifeCycleOperation = (isDefaultLifecycleFilterPresent == null) ? null :
                    IS_DEFAULT_LIFECYCLE_OPERATION_SUPPORTED_FILTER.get(isDefaultLifecycleFilterPresent);
        }
        return checkFilterAndQuery(filters, vnfInstanceFilterPresent, lifecycleFilterPresent, shouldKeepCurrentLifeCycleOperation);
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getVnfResourcesPage(final String filters, Boolean allResources, Pageable pageable) {
        Page<VnfResourceView> vnfResourceViewPage;
        if (Boolean.TRUE == allResources && StringUtils.isEmpty(filters)) {
            vnfResourceViewPage = vnfResourceViewRepository.findAll(pageable);
            initializeVnfResourceViewAssociations(vnfResourceViewPage);
            initializeVnfResourceViewEncryptedFields(vnfResourceViewPage);
        } else {
            String mappedFilters;
            if (filters == null || filters.isEmpty()) {
                mappedFilters = "";
            } else {
                String unmappedFilters = vnfResourceViewQuery.filterNotPresent(filters);
                isDefaultLifecycleFilterPresent(unmappedFilters);
                mappedFilters = vnfResourceViewQuery.filterPresent(filters);
            }
            vnfResourceViewPage = vnfResourceViewQuery.getPageWithFilter(mappedFilters, pageable);
            initializeVnfResourceViewAssociations(vnfResourceViewPage);
            vnfResourceViewPage.forEach(ResourcesService::sortOperations);
        }

        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfResourceViews(vnfResourceViewPage.getContent());
        return vnfResourceViewPage.map(vnfResourceView -> createResourceResponse(vnfResourceView, changePackageOperationDetails));
    }

    private void initializeVnfResourceViewAssociations(Page<VnfResourceView> vnfResourceViewPage) {
        List<VnfResourceView> vnfResourceViews = vnfResourceViewPage.getContent();
        databaseInteractionService.fetchAssociationForVnfResourceViews(vnfResourceViews, LifecycleOperation.class, VnfResourceView_.allOperations);
        databaseInteractionService.fetchAssociationForVnfResourceViews(vnfResourceViews, LifecycleOperation.class,
                                                                       VnfResourceView_.lastLifecycleOperation);
        databaseInteractionService.fetchAssociationForVnfResourceViews(vnfResourceViews, ScaleInfoEntity.class, VnfResourceView_.scaleInfoEntity);
    }

    private void initializeVnfResourceViewEncryptedFields(Page<VnfResourceView> vnfResourceViewPage) {
        List<VnfResourceView> vnfResourceViews = vnfResourceViewPage.getContent();
        databaseInteractionService.initializeInstantiateOssTopologyFieldInVnfResourceViews(vnfResourceViews);
        final List<LifecycleOperation> operations = vnfResourceViews.stream()
                .flatMap(vnfInstance -> vnfInstance.getAllOperations().stream())
                .collect(Collectors.toList());
        databaseInteractionService.initializeOperationParamsFieldInLifecycleOperations(operations);
    }

    private void initializeVnfInstanceEncryptedFields(List<VnfInstance> vnfInstances) {
        databaseInteractionService.initializeInstantiateOssTopologyFieldInVnfInstances(vnfInstances);
        final List<LifecycleOperation> lifecycleOperations = vnfInstances.stream()
                .flatMap(vnfInstance -> vnfInstance.getAllOperations().stream())
                .collect(Collectors.toList());
        databaseInteractionService.initializeOperationParamsFieldInLifecycleOperations(lifecycleOperations);
    }

    private ResourceResponse createResourceResponse(final VnfResourceView vnfResourceView,
                                                    final List<ChangePackageOperationDetails> changePackageOperationDetails) {
        List<LifecycleOperation> allOperations = vnfResourceView.getAllOperations();
        boolean downgradeSupported = Optional.ofNullable(allOperations)
                .map(ArrayList::new)
                .map(allOps -> {
                    allOps.sort(Comparator.comparing(LifecycleOperation::getStateEnteredTime).reversed());
                    return allOps;
                })
                .flatMap(operations -> lifecycleOperationHelper.findLatestUpgradeOperation(operations, vnfResourceView.getVnfInstanceId(),
                                                                                           changePackageOperationDetails))
                .isPresent();
        ResourceResponse response = resourceViewResponseMapper.toInternalModel(vnfResourceView);
        response.setDowngradeSupported(downgradeSupported);
        List<OperationDetail> operationDetails = databaseInteractionService.getSupportedOperationsByVnfInstanceId(vnfResourceView.getVnfInstanceId());
        if (operationDetails != null) {
            List<VnfResourceOperationDetail> vnfResourceOperationDetails = operationDetails.stream()
                    .map(ResourcesService::mapOperationDetailToVnfResourceOperationDetail)
                    .collect(Collectors.toList());
            response.setSupportedOperations(vnfResourceOperationDetails);
        }
        return response;
    }

    private static VnfResourceOperationDetail mapOperationDetailToVnfResourceOperationDetail(OperationDetail supportedOperation) {
        VnfResourceOperationDetail vnfResourceOperationDetail = new VnfResourceOperationDetail();
        vnfResourceOperationDetail.setOperationName(supportedOperation.getOperationName());
        vnfResourceOperationDetail.setSupported(supportedOperation.isSupported());
        vnfResourceOperationDetail.setError(supportedOperation.getErrorMessage());
        return vnfResourceOperationDetail;
    }

    private List<VnfResource> checkFilterAndQuery(
            String filters, String vnfInstanceFilterPresent, String lifecycleFilterPresent,
            Boolean shouldKeepCurrentLifeCycleOperation) {
        List<VnfResource> allResources = new ArrayList<>();
        if (!StringUtils.isEmpty(vnfInstanceFilterPresent)) {
            if (!StringUtils.isEmpty(lifecycleFilterPresent)) {
                createVnfResourcesWithInstanceAndLifecycleFilter(allResources, vnfInstanceFilterPresent,
                                                                 lifecycleFilterPresent, shouldKeepCurrentLifeCycleOperation);
            } else {
                List<VnfInstance> allInstance = vnfInstanceQuery.getAllWithFilter(vnfInstanceFilterPresent);
                databaseInteractionService.fetchAssociationForVnfInstances(allInstance, LifecycleOperation.class, VnfInstance_.allOperations);
                databaseInteractionService.fetchAssociationForVnfInstances(allInstance, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);

                addResources(allInstance, shouldKeepCurrentLifeCycleOperation, allResources);
            }
        } else if (!StringUtils.isEmpty(lifecycleFilterPresent)) {
            createVnfResourcesWithLifecycleFilter(allResources, lifecycleFilterPresent,
                                                  shouldKeepCurrentLifeCycleOperation);
        } else if (shouldKeepCurrentLifeCycleOperation != null) {
            //code block to be executed when CurrentLifeCycleOperation filter is present
            List<VnfInstance> allInstance = getInstances();
            addResources(allInstance, shouldKeepCurrentLifeCycleOperation, allResources);
        } else {
            throw new IllegalArgumentException("Filter not supported " + filters);
        }
        return allResources;
    }

    private void addResources(List<VnfInstance> allInstance, Boolean shouldKeepCurrentLifeCycleOperation,
                              List<VnfResource> allResources) {
        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfInstances(allInstance);
        for (VnfInstance vnfInstance : allInstance) {
            VnfResource vnfResource = createVnfResource(vnfInstance, vnfInstance.getAllOperations(),
                                                        shouldKeepCurrentLifeCycleOperation, changePackageOperationDetails);
            if (vnfResource != null) {
                allResources.add(vnfResource);
            }
        }
    }

    /**
     * These methods validate the filter for the existence of isDefaultLifecycleFilter, and returns single filter if
     * multiple filter of same type is present
     *
     * @param filters
     * @return
     */
    @SuppressWarnings("squid:S4248")
    private static String isDefaultLifecycleFilterPresent(String filters) {
        if (StringUtils.isNotEmpty(filters)) {
            if (filters.contains(FILTER_SEPARATOR)) {
                String[] allFilter = filters.split(FILTER_SEPARATOR);
                //This loop is provided to validate all the filter that are left, If some filters are present that is
                // not valid, Then an exception needs to be thrown
                for (String filter : allFilter) {
                    validateDefaultLifecycleOperationSupportedFilter(filter);
                }
            } else {
                validateDefaultLifecycleOperationSupportedFilter(filters);
            }
        }
        //there could be multiple value provided like eq,currentLifecycleOperation,true;neq,currentLifecycleOperation,
        // false, But we need to return only one value
        return filters.contains(FILTER_SEPARATOR) ? filters.split(FILTER_SEPARATOR)[0] : filters;
    }

    private static void validateDefaultLifecycleOperationSupportedFilter(String filter) {
        if (!IS_DEFAULT_LIFECYCLE_OPERATION_FILTERS.contains(filter)) {
            throw new IllegalArgumentException("Filter not supported for " + filter);
        }
    }

    private void createVnfResourcesWithInstanceAndLifecycleFilter(
            List<VnfResource> allResources, String vnfInstanceFilterPresent, String lifecycleFilterPresent,
            Boolean shouldKeepCurrentLifeCycleOperation) {
        List<VnfInstance> allInstance = vnfInstanceQuery.getAllWithFilter(vnfInstanceFilterPresent);
        databaseInteractionService.fetchAssociationForVnfInstances(allInstance, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);
        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfResources(allResources);
        for (VnfInstance vnfInstance : allInstance) {
            List<LifecycleOperation> lcmOperations = getAllLifecycleWithQueryFilterAndInstanceId(
                    vnfInstance.getVnfInstanceId(), lifecycleFilterPresent);
            if (lcmOperations != null && !lcmOperations.isEmpty()) {
                VnfResource vnfResource = createVnfResource(vnfInstance, lcmOperations,
                                                            shouldKeepCurrentLifeCycleOperation, changePackageOperationDetails);
                if (vnfResource != null) {
                    allResources.add(vnfResource);
                }
            }
        }
    }

    private void createVnfResourcesWithLifecycleFilter(
            List<VnfResource> allResources, String lifecycleFilterPresent,
            Boolean shouldKeepCurrentLifeCycleOperation) {
        List<LifecycleOperation> allOperation = getAllLifecycleOperationWithFilter(lifecycleFilterPresent);
        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfResources(allResources);
        for (LifecycleOperation operation : allOperation) {
            VnfResource resources = getResourcesWithInstanceId(allResources, operation.getVnfInstance());
            if (resources == null) {
                List<LifecycleOperation> resourceOperation = new ArrayList<>();
                resourceOperation.add(operation);
                VnfResource vnfResource = createVnfResource(operation.getVnfInstance(), resourceOperation,
                                                            shouldKeepCurrentLifeCycleOperation, changePackageOperationDetails);
                if (vnfResource != null) {
                    allResources.add(vnfResource);
                }
            } else {
                VnfResourceLifecycleOperation singleLifecycleOperation = vnfLifecycleMapper.toInternalModel(operation);
                singleLifecycleOperation.setCurrentLifecycleOperation(setCurrentLifeCycleOperation(operation
                                                                                                           .getVnfInstance(), operation));
                resources.getLcmOperationDetails().add(singleLifecycleOperation);
            }
        }
    }

    private static VnfResource getResourcesWithInstanceId(List<VnfResource> allResources, VnfInstance vnfInstance) {
        for (VnfResource resources : allResources) {
            if (resources.getInstanceId().equals(vnfInstance.getVnfInstanceId())) {
                return resources;
            }
        }
        return null;
    }

    public List<VnfInstance> getInstances() {
        final List<VnfInstance> vnfInstances = databaseInteractionService.getAllVnfInstances();

        databaseInteractionService.fetchAssociationForVnfInstances(vnfInstances, LifecycleOperation.class, VnfInstance_.allOperations);
        databaseInteractionService.fetchAssociationForVnfInstances(vnfInstances, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);

        initializeVnfInstanceEncryptedFields(vnfInstances);

        return vnfInstances;
    }

    public List<LifecycleOperation> getAllLifecycleOperationsWithInstanceId(final String instanceId) {
        if (StringUtils.isEmpty(instanceId)) {
            throw new IllegalArgumentException("Instance Id can't be null or empty");
        } else {
            VnfInstance vnfInstance = new VnfInstance();
            vnfInstance.setVnfInstanceId(instanceId);
            return databaseInteractionService.getLifecycleOperationsByVnfInstance(vnfInstance);
        }
    }

    public List<VnfResource> getVnfResources(final boolean getAllResources) {
        List<VnfResource> allVnfResources = new ArrayList<>();
        List<VnfInstance> allVnfInstances = getInstances();
        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfInstances(allVnfInstances);
        if (!CollectionUtils.isEmpty(allVnfInstances)) {
            for (VnfInstance vnfInstance : allVnfInstances) {
                VnfResource resource = getVnfResource(vnfInstance, getAllResources, changePackageOperationDetails);
                allVnfResources.add(resource);
            }
        } else {
            LOGGER.warn("No vnf resources present");
        }
        allVnfResources.removeAll(Collections.singletonList(null));
        return allVnfResources;
    }

    @Transactional
    public ResourceResponse getVnfResource(final String resourceId) {
        VnfInstance vnfInstance = getInstance(resourceId);
        if (vnfInstance.getAllOperations() != null && !vnfInstance.getAllOperations().isEmpty()) {
            vnfInstance.getAllOperations().sort(Comparator.comparing(LifecycleOperation::getStateEnteredTime).reversed());
        }
        ResourceResponse resource = instanceResourceResponseMapper.toInternalModel(vnfInstance);
        final List<ChangePackageOperationDetails> changeOperationPackageDetails =
                changePackageOperationDetailsService.findAllByVnfInstance(vnfInstance);
        resource.setDowngradeSupported(isVnfInstanceDowngradeSupported(vnfInstance, changeOperationPackageDetails));
        if (vnfInstance.isDeployableModulesSupported()) {
            scalingAspectsHelper.disableScalingAspectsForDisabledVdus(resource, vnfInstance);
        }
        return resource;
    }

    public VnfResource getVnfResource(final String resourceId, final boolean getAllResources) {
        VnfInstance vnfInstance = getInstance(resourceId);
        final List<ChangePackageOperationDetails> changePackageOperationDetails =
                changePackageOperationDetailsService.findAllByVnfInstance(vnfInstance);
        return getVnfResource(vnfInstance, getAllResources, changePackageOperationDetails);
    }

    private VnfResource getVnfResource(final VnfInstance vnfInstance, final boolean getAllResources,
                                       final List<ChangePackageOperationDetails> changePackageOperationDetails) {
        final List<LifecycleOperation> allLifeCycleOperations = vnfInstance.getAllOperations();
        if (!getAllResources && allLifeCycleOperations.isEmpty()) {
            return null;
        }
        LOGGER.debug("Got %s lifecycle operation details {} for vnf instance {}", allLifeCycleOperations.size(), vnfInstance.getVnfInstanceId());
        return createVnfResource(vnfInstance, allLifeCycleOperations, null, changePackageOperationDetails);
    }

    private List<LifecycleOperation> getAllLifecycleWithQueryFilterAndInstanceId(String instanceId, String lcmFilters) {
        if (StringUtils.isNotEmpty(lcmFilters)) {
            return lifecycleOperationQuery.getAllWithFilter(lifecycleOperationQuery.filterPresent(lcmFilters)
                                                                    + String.format(INSTANCE_ID_FILTER, instanceId));
        } else {
            return getAllLifecycleOperationsWithInstanceId(instanceId);
        }
    }

    private List<LifecycleOperation> getAllLifecycleOperationWithFilter(String lcmFilters) {
        return lifecycleOperationQuery.getAllWithFilter(lcmFilters);
    }

    public VnfResource createVnfResource(final VnfInstance vnfInstance, final List<LifecycleOperation> allLifecycleOperation,
                                         Boolean shouldKeepCurrentLifeCycleOperation,
                                         List<ChangePackageOperationDetails> changePackageOperationDetails) {
        VnfResource vnfResource = vnfResourceMapper.toInternalModel(vnfInstance);
        List<VnfResourceLifecycleOperation> allResourceLifeCycleOperation = new ArrayList<>();
        for (LifecycleOperation lifecycleOperation : allLifecycleOperation) {
            VnfResourceLifecycleOperation singleLifecycleOperation = vnfLifecycleMapper.toInternalModel(lifecycleOperation);
            boolean isCurrentLifecycleOperation = setCurrentLifeCycleOperation(vnfInstance, lifecycleOperation);
            if (shouldKeepCurrentLifeCycleOperation != null && shouldKeepCurrentLifeCycleOperation) {
                if (isCurrentLifecycleOperation) {
                    singleLifecycleOperation.setCurrentLifecycleOperation(true);
                    allResourceLifeCycleOperation.add(singleLifecycleOperation);
                }
            } else if (shouldKeepCurrentLifeCycleOperation != null) {
                if (!isCurrentLifecycleOperation) {
                    singleLifecycleOperation.setCurrentLifecycleOperation(false);
                    allResourceLifeCycleOperation.add(singleLifecycleOperation);
                }
            } else {
                singleLifecycleOperation.setCurrentLifecycleOperation(isCurrentLifecycleOperation);
                allResourceLifeCycleOperation.add(singleLifecycleOperation);
            }
        }
        if (shouldKeepCurrentLifeCycleOperation != null && allResourceLifeCycleOperation.isEmpty()) {
            return null;
        }
        allResourceLifeCycleOperation.sort(Comparator.comparing(VnfResourceLifecycleOperation::getStateEnteredTime).reversed());
        vnfResource.setLcmOperationDetails(allResourceLifeCycleOperation);
        vnfResource.setDowngradeSupported(isVnfInstanceDowngradeSupported(vnfInstance, changePackageOperationDetails));
        return vnfResource;
    }

    private static boolean setCurrentLifeCycleOperation(final VnfInstance vnfInstance, LifecycleOperation lifecycleOperation) {
        return lifecycleOperation.getOperationOccurrenceId().equals(vnfInstance.getOperationOccurrenceId());
    }

    private boolean isVnfInstanceDowngradeSupported(VnfInstance vnfInstance, List<ChangePackageOperationDetails> changePackageOperationDetails) {
        return lcmOpSearchService.searchLastChangingOperation(vnfInstance, changePackageOperationDetails).isPresent();
    }

    private static void sortOperations(VnfResourceView vnfResourceView) {
        if (vnfResourceView.getAllOperations() != null) {
            vnfResourceView.getAllOperations().sort(Comparator.comparing(LifecycleOperation::getStateEnteredTime).reversed());
        }
    }

    @Autowired
    @Lazy
    public void setResourcesServiceProxy(final ResourcesService resourcesServiceProxy) {
        this.resourcesServiceProxy = resourcesServiceProxy;
    }
}
