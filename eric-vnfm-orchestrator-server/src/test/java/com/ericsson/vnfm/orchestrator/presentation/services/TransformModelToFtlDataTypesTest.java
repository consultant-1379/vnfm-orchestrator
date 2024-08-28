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

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.utils.OssTopologyUtility.transformModelToFtlDataTypes;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ericsson.vnfm.orchestrator.model.onboarding.PropertiesModel;

public class TransformModelToFtlDataTypesTest {

    @Test
    public void testPropertiesModelTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();

        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setDefaultValue("22");
        sshPortNo.setType("string");

        PropertiesModel fmAlarmSupervision = new PropertiesModel();
        fmAlarmSupervision.setDefaultValue("true");
        fmAlarmSupervision.setType("boolean");

        PropertiesModel snmpPrivacyPassword = new PropertiesModel();
        snmpPrivacyPassword.setDefaultValue("testPassword");
        snmpPrivacyPassword.setType("string");

        properties.put("sshPortNo", sshPortNo);
        properties.put("fmAlarmSupervision", fmAlarmSupervision);
        properties.put("snmpPrivacyPassword", snmpPrivacyPassword);

        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap.get("sshPortNo")).isEqualTo("22");
        assertThat(finalMap.get("fmAlarmSupervision")).isEqualTo(Boolean.TRUE);
        assertThat(finalMap.get("snmpPrivacyPassword")).isEqualTo("testPassword");
    }

    @Test
    public void testPropertiesModelBooleanNullRequiredTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("boolean");
        sshPortNo.setDefaultValue("true");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).hasSize(1);
        assertThat(finalMap.get("sshPortNo")).isEqualTo(true);
    }

    @Test
    public void testPropertiesModelBooleanNullDefaultValueTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("boolean");
        sshPortNo.setRequired("false");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).isEmpty();
    }

    @Test
    public void testPropertiesModelBooleanNullDefaultValueAndRequiredTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("boolean");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).isEmpty();
    }

    @Test
    public void testPropertiesModelStringNullRequiredTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("string");
        sshPortNo.setDefaultValue("true");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).hasSize(1);
        assertThat(finalMap.get("sshPortNo")).isEqualTo("true");
    }

    @Test
    public void testPropertiesModelNullTypeTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setRequired("false");
        sshPortNo.setDefaultValue("true");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).hasSize(1);
        assertThat(finalMap.get("sshPortNo")).isEqualTo("true");
    }

    @Test
    public void testPropertiesModelStringNullDefaultValueTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("string");
        sshPortNo.setRequired("false");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).isEmpty();
    }

    @Test
    public void testPropertiesModelNullDefaultValueAndTypeTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setRequired("false");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).isEmpty();
    }

    @Test
    public void testPropertiesModelStringNullDefaultValueAndRequiredTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setType("string");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).isEmpty();
    }

    @Test
    public void testPropertiesModelNullTypeAndRequiredTransformation() {
        Map<String, PropertiesModel> properties = new HashMap<>();
        PropertiesModel sshPortNo = new PropertiesModel();
        sshPortNo.setDefaultValue("true");
        properties.put("sshPortNo", sshPortNo);
        Map<String, Object> finalMap = transformModelToFtlDataTypes(properties);

        assertThat(finalMap).hasSize(1);
        assertThat(finalMap.get("sshPortNo")).isEqualTo("true");
    }
}
