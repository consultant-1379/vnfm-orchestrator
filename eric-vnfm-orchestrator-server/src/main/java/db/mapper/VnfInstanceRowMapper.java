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
package db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;

public class VnfInstanceRowMapper implements RowMapper<VnfInstance> {

    private static final String POLICIES = "policies";
    private static final String RESOURCE_DETAILS = "resource_details";
    private static final String VNF_ID_COLUMN = "vnf_id";

    @Override
    public VnfInstance mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setVnfInstanceId(rs.getString(VNF_ID_COLUMN));
        vnfInstance.setResourceDetails(rs.getString(RESOURCE_DETAILS));
        vnfInstance.setPolicies(rs.getString(POLICIES));
        return vnfInstance;
    }
}
