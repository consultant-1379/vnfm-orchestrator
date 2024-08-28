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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.vnfm.orchestrator.model.VnfDomainModel;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;

public abstract class UpdateMissingFieldVnfInstance {

    @Autowired
    private EntityManager entityManager;


    abstract void update();

    abstract void update(List<VnfDomainModel> vnfDomainModels);

    protected void alterTable(String alterTableSql) {
        entityManager.createNativeQuery(alterTableSql).executeUpdate();
    }

    protected void updateTable(String updateTableSql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(updateTableSql);
        parameters.forEach(query::setParameter);
        query.executeUpdate();
    }

    protected void updateViaSqlMigration(String resourcePath) {
        try {
            String sql = IOUtils.toString(
                    getClass().getResourceAsStream(resourcePath), StandardCharsets.UTF_8);
            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (IOException e) {
            throw new InternalRuntimeException("Unable to load migration", e);
        }
    }
}
