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

import static com.ericsson.vnfm.orchestrator.utils.VnfdUtils.searchForInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.vnfm.orchestrator.model.VnfDomainModel;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
public class UpdateIsHealSupportedVnfInstance extends UpdateMissingFieldVnfInstance {
    private static final String ALTER_TABLE_SQL = "ALTER TABLE app_vnf_instance ALTER COLUMN is_heal_supported SET NOT NULL";
    private static final String SQL_MIGRATION_FILE_PATH = "/db/dev/V105_2__UpdateIsHealSupportedVnfInstance.sql";
    private static final String UPDATE_TABLE_SQL = "UPDATE app_vnf_instance SET is_heal_supported = :isHealSupported WHERE vnf_id = :vnfId";

    @Override
    public void update() {
        LOGGER.info("IsHealSupported update via sql migration has started");
        updateViaSqlMigration(SQL_MIGRATION_FILE_PATH);
    }

    @Override
    @Transactional
    public void update(List<VnfDomainModel> vnfDomainModels) {
        LOGGER.info("IsHealSupported update via onboarding-service calls has started");
        updateIsHealSupported(vnfDomainModels);
        LOGGER.info("Make is_heal_supported not null");
        alterTable(ALTER_TABLE_SQL);
    }

    private void updateIsHealSupported(List<VnfDomainModel> vnfDomainModels) {
        vnfDomainModels.forEach(vnfDomainModel -> {
            boolean isHealSupported = vnfDomainModel.getVnfd() != null &&
                    searchForInterface(vnfDomainModel.getVnfd(), "heal").isPresent();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("isHealSupported", isHealSupported);
            parameters.put("vnfId", vnfDomainModel.getVnfId());

            updateTable(UPDATE_TABLE_SQL, parameters);
        });

        LOGGER.info("Is_heal_supported field updated for {} instances", vnfDomainModels.size());
    }
}