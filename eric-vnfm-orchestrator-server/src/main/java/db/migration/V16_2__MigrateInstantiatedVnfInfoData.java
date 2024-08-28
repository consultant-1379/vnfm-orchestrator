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
package db.migration;

import static db.migration.MigrationUtilities.generateUUIDUniqueToTheTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class V16_2__MigrateInstantiatedVnfInfoData extends BaseJavaMigration {

    private static final String INSTANTIATED_VNF_INFO_COLUMN = "instantiated_vnf_info";
    private static final String VNF_ID = "vnf_id";
    private static final String SCALE_INFO_TABLE = "scale_info";
    private static final String SCALE_INFO_ID_COLUMN = "scale_info_id";
    private static final String SCALE_INFO_VNF_INSTANCE_ID_COLUMN = "vnf_instance_id";
    private static final String ASPECT_ID_COLUMN = "aspect_id";
    private static final String SCALE_LEVEL_COLUMN = "scale_level";

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void migrate(final Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        getAllVnfInstances(jdbcTemplate).stream()
                .filter(row -> StringUtils.isNotEmpty(row.get(INSTANTIATED_VNF_INFO_COLUMN)))
                .forEach(row -> migrateToInstantiatedVnfInfoObject(row, jdbcTemplate));
    }

    private List<Map<String, String>> getAllVnfInstances(final JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query("SELECT * FROM app_vnf_instance", new VnfInstanceRowMapper());
    }

    private void migrateToInstantiatedVnfInfoObject(final Map<String, String> row, final JdbcTemplate jdbcTemplate) {
        try {
            List<ScaleInfoEntity> scaleInfoEntityList =
                    mapper.readValue(new JSONObject(row.get(INSTANTIATED_VNF_INFO_COLUMN)).get("scaleStatus").toString(),
                                     new TypeReference<List<ScaleInfoEntity>>() { });
            persistScaleInfo(jdbcTemplate, scaleInfoEntityList, row.get(VNF_ID));
        } catch (JsonProcessingException e) {
            throw new InternalRuntimeException(String.format("Failed to parse Json string representation of scaleInfo: %s",
                                               row.get(INSTANTIATED_VNF_INFO_COLUMN)), e);
        }
    }

    private void persistScaleInfo(final JdbcTemplate jdbcTemplate,
                                  final List<ScaleInfoEntity> scaleInfoList,
                                  final String instantiatedVnfInfoId) {
        SimpleJdbcInsert insertIntoScaleInfo = new SimpleJdbcInsert(jdbcTemplate).withTableName(SCALE_INFO_TABLE);
        final List<String> listOfIds = jdbcTemplate.query("SELECT * FROM " + SCALE_INFO_TABLE, new ScaleInfoRowMapper());
        for (ScaleInfoEntity scaleInfoEntity : scaleInfoList) {
            Map<String, Object> scaleInfoParameters = new HashMap<>();
            scaleInfoParameters.put(SCALE_INFO_ID_COLUMN, generateUUIDUniqueToTheTable(listOfIds));
            scaleInfoParameters.put(SCALE_INFO_VNF_INSTANCE_ID_COLUMN, instantiatedVnfInfoId);
            scaleInfoParameters.put(ASPECT_ID_COLUMN, scaleInfoEntity.getAspectId());
            scaleInfoParameters.put(SCALE_LEVEL_COLUMN, Integer.valueOf(scaleInfoEntity.getScaleLevel()));
            insertIntoScaleInfo.execute(scaleInfoParameters);
        }
    }

    private class VnfInstanceRowMapper implements RowMapper<Map<String, String>> {

        @Override
        public Map<String, String> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Map<String, String> row = new HashMap<>();
            row.put(VNF_ID, rs.getString(VNF_ID));
            row.put(INSTANTIATED_VNF_INFO_COLUMN, rs.getString(INSTANTIATED_VNF_INFO_COLUMN));
            return row;
        }
    }

    private class ScaleInfoRowMapper implements RowMapper<String> {

        @Override
        public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return rs.getString(SCALE_INFO_ID_COLUMN);
        }
    }
}
