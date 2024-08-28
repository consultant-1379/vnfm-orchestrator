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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.am.shared.vnfd.validation.ToscaSupportedOperationValidator;
import com.ericsson.vnfm.orchestrator.model.VnfDomainModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Profile("!test")
public class UpdateSupportedOperationsVnfInstance extends UpdateMissingFieldVnfInstance {
    private static final String ALTER_TABLE_SQL = "ALTER TABLE app_vnf_instance ALTER COLUMN supported_operations SET NOT NULL";
    private static final String SQL_MIGRATION_FILE_PATH = "/db/dev/V105_1__UpdateSupportedOperationsVnfInstance.sql";
    private static final String UPDATE_TABLE_SQL = "UPDATE app_vnf_instance SET supported_operations = :supportedOperations WHERE vnf_id = :vnfId";

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public void update() {
        LOGGER.info("SupportedOperations update via sql migration has started");
        updateViaSqlMigration(SQL_MIGRATION_FILE_PATH);
    }

    @Override
    @Transactional
    public void update(List<VnfDomainModel> vnfDomainModels) {
        LOGGER.info("SupportedOperations update via onboarding-service calls has started");
        updateSupportedOperations(vnfDomainModels);
        LOGGER.info("Make supported_operations not null");
        alterTable(ALTER_TABLE_SQL);
    }

    private void updateSupportedOperations(final List<VnfDomainModel> vnfDomainModels) {
        vnfDomainModels.forEach(vnf -> {
            String supportedOperations = getSupportedOperations(vnf);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("supportedOperations", supportedOperations);
            parameters.put("vnfId", vnf.getVnfId());

            updateTable(UPDATE_TABLE_SQL, parameters);
        });

        LOGGER.info("Supported_operation field updated for {} instances", vnfDomainModels.size());
    }

    private String getSupportedOperations(VnfDomainModel vnfDomainModel) {
        if (vnfDomainModel.getVnfd() == null) {
            return "[]";
        }
        String vnfd = vnfDomainModel.getVnfd().toString();
        List<OperationDetail> operationDetails = ToscaSupportedOperationValidator.getVnfdSupportedOperations(vnfd);
        try {
            return objectMapper.writeValueAsString(operationDetails);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to convert object to json", e);
            return  "[]";
        }
    }
}