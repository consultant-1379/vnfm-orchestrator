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
package com.ericsson.vnfm.orchestrator.infrastructure.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ericsson.vnfm.orchestrator.model.VnfDomainModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.VnfdNotFoundException;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
public class VnfInstanceMissingFieldUpdater {

    @Autowired
    private PackageService packageService;

    @Autowired
    private EntityManager em;

    @Value("${ci_test_enabled:false}")
    private boolean ciTestEnabled;

    @Autowired
    private UpdateIsHealSupportedVnfInstance updateIsHealSupportedVnfInstance;

    @Autowired
    private UpdateSupportedOperationsVnfInstance updateSupportedOperationsVnfInstance;

    public void updateMissingFields() {
        if (ciTestEnabled) {
            updateSupportedOperationsVnfInstance.update();
            updateIsHealSupportedVnfInstance.update();
        } else {
            List<String> notFoundPackages = new ArrayList<>();
            List<VnfDomainModel> instancesWithMissingFields = getVnfListWithMissingFields();
            instancesWithMissingFields.forEach(instance -> {
                try {
                    JSONObject vnfd = packageService.getVnfd(instance.getVnfPackageId());
                    instance.setVnfd(vnfd);
                } catch (VnfdNotFoundException exception) { // NOSONAR
                    LOGGER.error(exception.getMessage());
                    notFoundPackages.add(instance.getVnfPackageId());
                }
            });

            updateSupportedOperations(instancesWithMissingFields);
            updateIsHealSupported(instancesWithMissingFields);

            if (!CollectionUtils.isEmpty(notFoundPackages)) {
                LOGGER.error(String.format("Failed to locate VNFD for packages with id %s.",
                                                              String.join(", ", notFoundPackages)));
            }
        }
    }

    private void updateSupportedOperations(List<VnfDomainModel> vnfDomainModels) {
        List<VnfDomainModel> modelsWithoutSupportedOperations = vnfDomainModels.stream()
                        .filter(vnfDomainModel -> StringUtils.isBlank(vnfDomainModel.getSupportedOperations())
                        || Objects.equals(vnfDomainModel.getSupportedOperations(), "[]"))
                                .collect(Collectors.toList());
        updateSupportedOperationsVnfInstance.update(modelsWithoutSupportedOperations);
    }

    private void updateIsHealSupported(List<VnfDomainModel> vnfDomainModels) {
        List<VnfDomainModel> modelsWithoutSupportedOperations = vnfDomainModels.stream()
                .filter(vnfDomainModel -> !Boolean.TRUE.equals(vnfDomainModel.getIsHealSupported()))
                .collect(Collectors.toList());
        updateIsHealSupportedVnfInstance.update(modelsWithoutSupportedOperations);
    }

    private List<VnfDomainModel> getVnfListWithMissingFields() {
        String sql = "SELECT vnf_id, vnf_pkg_id, Cast(supported_operations as varchar) supported_operations, is_heal_supported "
                + "FROM app_vnf_instance WHERE "
                + "supported_operations IS NULL "
                + "OR supported_operations = '[]' OR is_heal_supported IS NULL OR is_heal_supported IS FALSE";
        return (List<VnfDomainModel>) em.createNativeQuery(sql).getResultList()
                .stream()
                .map(vnfPersistence -> getVnfDomainModel((Object[]) vnfPersistence))
                .collect(Collectors.toList());
    }

    private VnfDomainModel getVnfDomainModel(final Object[] vnfPersistence) {
        return VnfDomainModel.builder()
                .vnfId(((String) vnfPersistence[0]))
                .vnfPackageId((String) vnfPersistence[1])
                .supportedOperations((String) vnfPersistence[2])
                .isHealSupported((Boolean) vnfPersistence[3])
                .build();
    }
}
