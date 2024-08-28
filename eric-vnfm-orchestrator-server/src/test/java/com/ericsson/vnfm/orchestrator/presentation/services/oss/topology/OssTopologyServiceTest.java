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
package com.ericsson.vnfm.orchestrator.presentation.services.oss.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.OPERATION_RESPONSE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.ALARM_SET_VALUE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.AddNode;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.Error.PARAMETER_MISSING_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.FILE_TO_UPLOAD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateEnrollment.GENERATED_XML_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.FALLBACK_LDAP_IPV4_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.FALLBACK_LDAP_IPV6_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_IPV4_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.GenerateOssNodeProtocol.LDAP_IPV6_ADDRESS_STRING;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.MANAGED_ELEMENT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.ACTION_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE_REF;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.BACKUP_FILE_REF_PASSWORD;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OssTopologyConstants.RestoreBackup.RESTORE_BACKUP_FILE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ADD_NODE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DELETE_NODE;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.DISABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENABLE_ALARM_SUPERVISION;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.ENROLLMENT_INFO;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.IMPORT_BACKUP;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.IMPORT_BACKUP_PROGRESS;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.RESTORE_BACKUP;
import static com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum.RESTORE_LATEST_BACKUP;
import static com.ericsson.vnfm.orchestrator.utils.Utility.convertXMLToJson;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.FreemarkerConfiguration;
import com.ericsson.vnfm.orchestrator.utils.EnmOperationEnum;
import com.jayway.jsonpath.JsonPath;


@TestPropertySource(properties = {"spring.cloud.kubernetes.enabled = false", "spring.main.cloud-platform = NONE"})
@SpringBootTest(classes = {OssTopologyService.class, EnmTopologyService.class, FreemarkerConfiguration.class })
public class OssTopologyServiceTest {

    private static final String ERROR_MESSAGE = "The parameter managedElementId was not provided";

    @Autowired
    private OssTopologyService ossTopologyService;

    private static Map<String, Object> addCommonScriptAttributes(String managedElementId, EnmOperationEnum operation) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(MANAGED_ELEMENT_ID, managedElementId);
        attributes.put(OPERATION, operation.getOperation());
        attributes.put(OPERATION_RESPONSE, operation.getOperationResponse());
        return attributes;
    }

    @Test
    public void generatePythonScriptForAddNode() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("cmedit"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(add_node_data)"));
    }

    @Test
    public void generatePythonScriptForAddNodeWithOptionalCnfTypeParameter() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.CNF_TYPE, "cnftype");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("cmedit"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("cnfType=cnftype"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(add_node_data)"));
    }

    @Test
    public void generatePythonScriptForAddNodeContainSecureUserPasswordInDoubleQuotes() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("secureuserpassword \"mypassword\""));
    }

    @Test
    public void generatePythonScriptWithTenant() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_VERSION, "2");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.PM_FUNCTION, true);
        topologyAttributes.put(AddNode.CM_NODE_HEARTBEAT_SUPERVISION, "false");
        topologyAttributes.put(AddNode.FM_ALARM_SUPERVISION, false);
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        topologyAttributes.put(AddNode.TENANT, "ECM");
        topologyAttributes.put(AddNode.SMALL_STACK_APPLICATION, false);
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(script -> script.contains("VirtualNetworkFunctionData=1 virtualNetworkFunctionDataId=1,tenant=\"ECM\",vnfInstanceId=\"10def1ce-4cf4-477c-aab3-21c454e6a389\""));
    }

    @Test
    public void generatePythonScriptWithTenantWithSmallStackWithoutVnfm() {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("enm-test", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "UDM-AUSF");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "10.210.174.58");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.SNMP_AUTH_PROTOCOL, "MD5");
        topologyAttributes.put(AddNode.PM_FUNCTION, true);
        topologyAttributes.put(AddNode.SNMP_PRIV_PROTOCOL, "AES128");
        topologyAttributes.put(AddNode.CM_NODE_HEARTBEAT_SUPERVISION, "true");
        topologyAttributes.put(AddNode.FM_ALARM_SUPERVISION, "true");
        topologyAttributes.put(AddNode.TRANSPORT_PROTOCOL, "SSH");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        topologyAttributes.put(AddNode.SMALL_STACK_APPLICATION, true);
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
    }

    @Test
    public void generatePythonScriptWithTenantWithFullStackWithoutVnfm() {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("enm-test", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "UDM-AUSF");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "10.210.174.58");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.SNMP_AUTH_PROTOCOL, "MD5");
        topologyAttributes.put(AddNode.PM_FUNCTION, true);
        topologyAttributes.put(AddNode.SNMP_PRIV_PROTOCOL, "AES128");
        topologyAttributes.put(AddNode.CM_NODE_HEARTBEAT_SUPERVISION, "true");
        topologyAttributes.put(AddNode.FM_ALARM_SUPERVISION, "true");
        topologyAttributes.put(AddNode.TRANSPORT_PROTOCOL, "SSH");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        topologyAttributes.put(AddNode.SMALL_STACK_APPLICATION, false);
        topologyAttributes.put(AddNode.TENANT, "ECM");
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
    }

    @Test
    public void generatePythonScriptWithTenantWithFullStackWithVnfm() {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("enm-test", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "UDM-AUSF");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "10.210.174.58");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.SNMP_AUTH_PROTOCOL, "MD5");
        topologyAttributes.put(AddNode.PM_FUNCTION, true);
        topologyAttributes.put(AddNode.SNMP_PRIV_PROTOCOL, "AES128");
        topologyAttributes.put(AddNode.CM_NODE_HEARTBEAT_SUPERVISION, "true");
        topologyAttributes.put(AddNode.FM_ALARM_SUPERVISION, "true");
        topologyAttributes.put(AddNode.TRANSPORT_PROTOCOL, "SSH");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        topologyAttributes.put(AddNode.SMALL_STACK_APPLICATION, false);
        topologyAttributes.put(AddNode.TENANT, "ECM");
        topologyAttributes.put(AddNode.VNFM_NAME, "ECM_01");
        final Path pythonScript = ossTopologyService.generateAddNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
    }

    @Test
    public void failForNullParameterMapInAddNode(){
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Attributes must be specified for the generation of the script");
    }

    @Test
    public void failForMissingMandatoryParameterInAddNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                ERROR_MESSAGE);
    }

    @Test
    public void failForNullMandatoryParameterInAddNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, null);
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    public void failForBlankMandatoryParameterInAddNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, "");
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter managedElementId was found to be null or empty");
    }

    @Test
    public void failForWrongParameterTypeBooleanInAddNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, "gjntkr");
        topologyAttributes.put(AddNode.PM_FUNCTION, "vnbjoew");
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter pmFunction was the wrong type");
    }

    @Test
    public void failForWrongParameterTypeNumberInAddNode(){
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_VERSION, "2");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NET_CONF_PORT, "gke");
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter netConfPort was the wrong type");
    }

    @Test
    public void generatePythonScriptForDeleteNode() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("nvriujefvnrei", DELETE_NODE);
        final Path pythonScript = ossTopologyService.generateDeleteNodeScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("cmedit"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(delete_node_data)"));
    }

    @Test
    public void failForMissingMandatoryParameterInDelete(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        assertThatThrownBy(() -> ossTopologyService.generateDeleteNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);

    }

    @Test
    public void failForNullParameterMapInDeleteNode(){
        assertThatThrownBy(() -> ossTopologyService.generateDeleteNodeScript(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Attributes must be specified for the generation of the script");
    }

    @Test
    public void failForNullMandatoryParameterInDeleteNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, null);
        assertThatThrownBy(() -> ossTopologyService.generateDeleteNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    public void failForBlankMandatoryParameterInDeleteNode(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, "");
        assertThatThrownBy(() -> ossTopologyService.generateDeleteNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter managedElementId was found to be null or empty");
    }

    @Test
    public void failForNullVnfInstanceId() {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_VERSION, "2");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.PM_FUNCTION, true);
        topologyAttributes.put(AddNode.CM_NODE_HEARTBEAT_SUPERVISION, "false");
        topologyAttributes.put(AddNode.FM_ALARM_SUPERVISION, false);
        topologyAttributes.put(AddNode.SMALL_STACK_APPLICATION, false);
        topologyAttributes.put(AddNode.TENANT, "ECM");
        assertThatThrownBy(() -> ossTopologyService.generateAddNodeScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter vnfInstanceId was not provided");
    }

    @Test
    public void generatePythonScriptForEnableSupervisions() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("gjntkr", ADD_NODE);
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_TYPE, "ERBS");
        topologyAttributes.put(AddNode.NODE_IP_ADDRESS, "my.vnf.com");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_USERNAME, "myuser");
        topologyAttributes.put(AddNode.NETWORK_ELEMENT_PASSWORD, "mypassword");
        topologyAttributes.put(AddNode.VNF_INSTANCE_ID, "10def1ce-4cf4-477c-aab3-21c454e6a389");
        final Path pythonScript = ossTopologyService.generateEnableSupervisionsScript(topologyAttributes);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("cmedit"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(enable_supervisions_data)"));
    }

    @Test
    public void failForMissingMandatoryParameterInEnableSupervisions(){
        final Map<String, Object> topologyAttributes = new HashMap<>();
        assertThatThrownBy(() -> ossTopologyService.generateEnableSupervisionsScript(topologyAttributes))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    public void generateLdapDetailsJsonSuccess() throws IOException, URISyntaxException {
        String ldapDetails = Files.readString(TestUtils.getResource("enrollmentAndLdapFile/validLdapDetails.json"));
        String ldapDetailsIpV4 = ossTopologyService.generateLdapConfigurationJSONString(ldapDetails, StandardProtocolFamily.INET);
        String ldapDetailsIpV6 = ossTopologyService.generateLdapConfigurationJSONString(ldapDetails, StandardProtocolFamily.INET6);

        verifyLdapConfiguration(ldapDetailsIpV4, "131.160.205.18", "131.160.205.19");
        verifyLdapConfiguration(ldapDetailsIpV6, "2001:1b70:6207:0029:0000:0878:1010:0029", "2001:1b70:6207:0029:0000:0878:1010:002b");

    }

    private void verifyLdapConfiguration(String ldapDetails, String primaryIp, String fallbackIp) {
        JSONObject ldapJson = new JSONObject(ldapDetails);
        assertThat(ldapDetails).isNotEmpty();
        assertThat(ldapJson.getJSONObject("security").get("tls")).isNotNull();
        JSONArray servers = ldapJson.getJSONArray("server");
        assertThat(servers.toList()).hasSize(2);
        for (int i = 0; i < servers.length(); i++) {
            JSONObject server = servers.getJSONObject(i);
            assertThat(server).isNotNull();
            assertThat(server.getString("name")).isNotEmpty();
            if (server.getString("name").equals("primary-server")) {
                assertThat(server.getJSONObject("tcp").getString("address")).isEqualTo(primaryIp);
            } else {
                assertThat(server.getJSONObject("tcp").getString("address")).isEqualTo(fallbackIp);
            }
            assertThat(server.getJSONObject("tcp").getJSONObject("ldaps").getInt("port")).isEqualTo(1636);
            assertThatThrownBy(() -> server.getJSONObject("tcp").get("ldap"))
                    .isInstanceOf(JSONException.class)
                    .hasMessageContaining("JSONObject[\"ldap\"] not found");
        }
    }

    @Test
    public void generateEnrollmentJsonSuccess() throws IOException, URISyntaxException {
        String enrollmentXml = Files.readString(TestUtils.getResource("enrollmentAndLdapFile/validEnrollmentFile.xml"));
        String enrollmentConfig = ossTopologyService.generateEnrollmentConfigurationJSONString(enrollmentXml);

        JSONObject enrollmentJson = new JSONObject(enrollmentConfig);
        assertThat(enrollmentJson.getJSONArray("ca-certs").toList()).hasSize(1);
        assertThat(enrollmentJson.getJSONObject("certificate-authorities")
                .getJSONArray("certificate-authority")
                .getJSONObject(0)
                .getString("name"))
                .isNotEmpty();
        assertThat(enrollmentJson.getJSONArray("enrollments").toList()).hasSize(1);
        assertThat(enrollmentJson.getJSONObject("cmp-server-groups").getJSONArray("cmp-server-group").toList())
                .hasSize(1);
    }

    @Test
    public void failGenerateLdapDetailsJsonDueToMissingParams() throws IOException, URISyntaxException {
        String ldapDetails = Files.readString(TestUtils.getResource("enrollmentAndLdapFile/invalidLdapDetailswithMissingLdapPort.json"));
        assertThatThrownBy(() -> ossTopologyService.generateLdapConfigurationJSONString(ldapDetails, StandardProtocolFamily.INET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Some parameters are missing from LDAP configuration");
    }

    @Test
    public void failGenerateEnrollmentJsonDueToInvalidXML() throws IOException, URISyntaxException {
        String enrollmentXml = Files.readString(TestUtils.getResource("enrollmentAndLdapFile/invalidEnrollmentFileWithMissingPassword.xml"));
        assertThatThrownBy(() -> ossTopologyService.generateEnrollmentConfigurationJSONString(enrollmentXml))
                .isInstanceOf(JSONException.class);

    }

    @Test
    public void generatePythonScriptForDisableAlarm() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("disable-alarm-id", DISABLE_ALARM_SUPERVISION);
        topologyAttributes.put(ALARM_SET_VALUE, DISABLE_ALARM_SUPERVISION.getSetValue());
        final Path pythonScript = ossTopologyService.generateSetAlarmScript(topologyAttributes, DISABLE_ALARM_SUPERVISION);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("def disableAlarmSupervision():"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("command = 'alarm disable disable-alarm-id'"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(set_alarm_data)"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("import enmscripting"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("setAlarmSupervisionStatus('off')"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("logging.basicConfig(filename='disableAlarmSupervision.log', level=logging"));
    }

    @Test
    public void generatePythonScriptForEnableAlarm() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("enable-alarm-id", ENABLE_ALARM_SUPERVISION);
        topologyAttributes.put(ALARM_SET_VALUE, ENABLE_ALARM_SUPERVISION.getSetValue());
        final Path pythonScript = ossTopologyService.generateSetAlarmScript(topologyAttributes, ENABLE_ALARM_SUPERVISION);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("def enableAlarmSupervision():"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("command = 'alarm enable enable-alarm-id'"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("response_json_data = json.dumps(set_alarm_data)"));
        assertThat(Files.readAllLines(pythonScript)).anyMatch(s -> s.contains("setAlarmSupervisionStatus('on')"));
    }

    @Test
    public void failForMissingMandatoryParameterInSetAlarm() {
        final Map<String, Object> topologyAttributes = new HashMap<>();
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, DISABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, ENABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    public void failForNullParameterMapInSetAlarm() {
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(null, DISABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Attributes must be specified for the generation of the script");
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(null, ENABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Attributes must be specified for the generation of the script");
    }

    @Test
    public void failForNullMandatoryParameterInSetAlarm() {
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, null);
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, DISABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, ENABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(ERROR_MESSAGE);
    }

    @Test
    public void failForBlankMandatoryParameterInSetAlarm() {
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(MANAGED_ELEMENT_ID, "");
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, DISABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter managedElementId was found to be null or empty");
        assertThatThrownBy(() -> ossTopologyService.generateSetAlarmScript(topologyAttributes, ENABLE_ALARM_SUPERVISION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter managedElementId was found to be null or empty");
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithIpv4LDapDetails() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetails.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                StandardProtocolFamily.INET);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, LDAP_IPV4_ADDRESS_STRING);
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithIpv6LDapDetails() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetails.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                StandardProtocolFamily.INET6);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, LDAP_IPV6_ADDRESS_STRING);
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithNullIpVersion() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetails.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                null);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, LDAP_IPV4_ADDRESS_STRING);
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithNullIpVersionAndMissingAllIpv4LdapDetails()
            throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetailsWith" +
                "MissingAllIpv4Details.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                null);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, LDAP_IPV6_ADDRESS_STRING);
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithNullIpVersionAndMissingIpv4LdapDetails() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLDapDetailsWithMissing" +
                "LdapIpv4Address.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                null);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, FALLBACK_LDAP_IPV4_ADDRESS_STRING);
    }

    @Test
    public void testSuccessGetOssNodeProtocolFileContentWithNullIpVersionAndMissingAllIpv4LdapAndIpv6Details()
            throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetailsWith" +
                "MissingAllIpv4AndIpv6Details.json");
        String ossNodeProtocolFile = ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                null);
        validateNodeProtocolFile(ossNodeProtocolFile, enrollmentFile, ldapDetails, FALLBACK_LDAP_IPV6_ADDRESS_STRING);
    }

    @Test
    public void testFailGetOssNodeProtocolFileContentWithMissingPasswordInEnrollmentFile() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/invalidEnrollment" +
                "FileWithMissingPassword.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLDapDetailsWithMissing" +
                "LdapIpv4Address.json");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Following details are missing in " +
                "the enrollment file : [challengePassword]");
    }

    @Test
    public void testFailGetOssNodeProtocolFileContentWithMissingLdapPortInLdapDetails() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/invalidLdapDetailswith" +
                "MissingLdapPort.json");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Following details are missing in " +
                "the enrollment file : [ldapsPort]");
    }

    @Test
    public void testFailGetOssNodeProtocolFileWithNullEnrollmentFile() {
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/invalidLdapDetailswith" +
                "MissingLdapPort.json");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(null, ldapDetails, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(String
                .format(PARAMETER_MISSING_ERROR_MESSAGE, "enrollment"));
    }

    @Test
    public void testFailGetOssNodeProtocolFileWithNullLdapDetails() throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/invalidEnrollment" +
                "FileWithMissingPassword.xml");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(String
                .format(PARAMETER_MISSING_ERROR_MESSAGE, "ldap"));
    }

    @Test
    public void testFailGetOssNodeProtocolFileContentWithIpVert4ProvidedAndIpv4DetailsMissingFromLdapDetails()
            throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLdapDetailsWithMissing" +
                "AllIpv4AndIpv6Details.json");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                StandardProtocolFamily.INET))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Following details are missing in " +
                "the enrollment file : [ldapIP]");
    }

    @Test
    public void testFailGetOssNodeProtocolFileContentWithIpVert6ProvidedAndIpv6DetailsMissingFromLdapDetails()
            throws Exception {
        String enrollmentFile = TestUtils.readDataFromFile("enrollmentAndLdapFile/validEnrollmentFile.xml");
        String ldapDetails = TestUtils.parseJsonFile("enrollmentAndLdapFile/validLDapDetailsWith" +
                "MissingLdapIpv4Address.json");
        assertThatThrownBy(() -> ossTopologyService.getOssNodeProtocolFileContent(enrollmentFile, ldapDetails,
                StandardProtocolFamily.INET6))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Following details are missing in " +
                "the enrollment file : [ldapIP]");
    }

    private void validateNodeProtocolFile(String ossNodeProtocolFile, String enrollmentFile, String ldapDetails,
                                          String ldapIpAddressKey) {
        JSONObject json = convertXMLToJson(ossNodeProtocolFile, "ossNodeProtocol");
        JSONObject enrollmentFileJsonObject = convertXMLToJson(enrollmentFile, ENROLLMENT);
        JSONArray rpc = json.getJSONArray("rpc");
        JSONObject ldapAddressJsonObject = new JSONObject(ldapDetails);
        Map<String, Object> parameter = new HashMap<>();
        EnmTopologyService ets = new EnmTopologyService();
        ets.validateAndSetCertificateInNodeProtocolParameter(ets.getAllCertificateFromJson(enrollmentFileJsonObject),
                parameter);
        for (int i = 0; i < rpc.length(); i++) {
            JSONObject comp = rpc.getJSONObject(i);
            if ("Configure LDAP Server Data".equals(comp.getString("message-id"))) {
                String ipAddress = JsonPath.read(comp.toString(), "$.edit-config.config.system.ldap.server" +
                        ".tcp.address");
                assertThat(ipAddress).isNotEmpty().isEqualTo(ldapAddressJsonObject.getString(ldapIpAddressKey));
                int port = JsonPath.read(comp.toString(), "$.edit-config.config.system.ldap.server.tcp" +
                        ".ldaps.port");
                assertThat(port).isEqualTo(Integer.parseInt(ldapAddressJsonObject.getString("ldapsPort")));
                String baseDn = JsonPath.read(comp.toString(), "$.edit-config.config.system.ldap.security" +
                        ".user-base-dn");
                assertThat(baseDn).isEqualTo(ldapAddressJsonObject.getString("baseDn"));
                String bindPassword = JsonPath.read(comp.toString(), "$.edit-config.config.system.ldap" +
                        ".security.simple-authenticated.bind-password");
                assertThat(bindPassword).isEqualTo(ldapAddressJsonObject.getString("bindPassword"));
                String bindDn = JsonPath.read(comp.toString(), "$.edit-config.config.system.ldap" +
                        ".security.simple-authenticated.bind-dn");
                assertThat(bindDn).isEqualTo(ldapAddressJsonObject.getString("bindDn"));
            } else if ("OAM CMP Server Configuration".equals(comp.getString("message-id"))) {
                String ca = JsonPath.read(comp.toString(), "$.edit-config.config.keystore.cmp" +
                        ".cmp-server-groups.cmp-server-group.cmp-server.certificate-authority");
                String caFromEnrollment = JsonPath.read(enrollmentFileJsonObject.toString(), "$.enrollmentInfo" +
                        ".issuerCA");
                assertThat(ca).isEqualTo(caFromEnrollment);
                String url = JsonPath.read(comp.toString(), "$.edit-config.config.keystore.cmp" +
                        ".cmp-server-groups.cmp-server-group.cmp-server.uri");
                String urlFromEnrollment = JsonPath.read(enrollmentFileJsonObject.toString(), "$.enrollmentInfo" +
                        ".url");
                assertThat(url).isEqualTo(urlFromEnrollment);
                String caNameInAuthority = JsonPath.read(comp.toString(), "$.edit-config.config.keystore.cmp" +
                        ".certificate-authorities.certificate-authority.name");
                assertThat(caNameInAuthority).isEqualTo(caFromEnrollment);
            } else if ("Install OAM CMP Trusted certificates".equals(comp.getString("message-id"))) {
                String certEnrollmentFile = (String) parameter.get("enmPKIRootCA");
                String certFromOssNodeProtocolFile = JsonPath.read(comp.toString(), "$.action.truststore" +
                        ".certificates.install-certificate-pem.pem");
                assertThat(certEnrollmentFile).isEqualTo(certFromOssNodeProtocolFile);
            } else if ("Intsall OAM Trusted Certificate-1".equals(comp.getString("message-id"))) {
                String certEnrollmentFile = (String) parameter.get("enmOAMCA");
                String certFromOssNodeProtocolFile = JsonPath.read(comp.toString(), "$.action.truststore" +
                        ".certificates.install-certificate-pem.pem");
                assertThat(certEnrollmentFile).isEqualTo(certFromOssNodeProtocolFile);
            } else if ("Intsall OAM Trusted Certificate-2".equals(comp.getString("message-id"))) {
                String certEnrollmentFile = (String) parameter.get("neOAMCA");
                String certFromOssNodeProtocolFile = JsonPath.read(comp.toString(), "$.action.truststore" +
                        ".certificates.install-certificate-pem.pem");
                assertThat(certEnrollmentFile).isEqualTo(certFromOssNodeProtocolFile);
            } else if ("Intsall OAM Trusted Certificate-3".equals(comp.getString("message-id"))) {
                String certEnrollmentFile = (String) parameter.get("enmInfrastructureCA");
                String certFromOssNodeProtocolFile = JsonPath.read(comp.toString(), "$.action.truststore" +
                        ".certificates.install-certificate-pem.pem");
                assertThat(certEnrollmentFile).isEqualTo(certFromOssNodeProtocolFile);
            } else if ("Intsall OAM Trusted Certificate-4".equals(comp.getString("message-id"))) {
                String certEnrollmentFile = (String) parameter.get("enmPKIRootCA");
                String certFromOssNodeProtocolFile = JsonPath.read(comp.toString(), "$.action.truststore" +
                        ".certificates.install-certificate-pem.pem");
                assertThat(certEnrollmentFile).isEqualTo(certFromOssNodeProtocolFile);
            }
        }
        assertThat(json).isNotNull();
        assertThat(ossNodeProtocolFile).isNotEmpty();
        assertThat(ossNodeProtocolFile).doesNotContain("${");
    }

    @Test
    public void generatePythonScriptForEnrollmentInformation() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("get-enrollment-information", ENROLLMENT_INFO);
        topologyAttributes.put(FILE_TO_UPLOAD, "node.xml");
        topologyAttributes.put(GENERATED_XML_FILE, "enrollmentConfigurationFile.xml");
        final Path pythonScript = ossTopologyService.generateGetEnrollmentInfoScript(topologyAttributes, ENROLLMENT_INFO);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript))
                .anyMatch(s -> s.contains("def download_first_file( response, download_path=None):"))
                .anyMatch(s -> s.contains("def executeCommandWithFileUploadDownload( command, file_upload, download_path ):"))
                .anyMatch(s -> s.contains("file.download(download_path)"))
                .anyMatch(s -> s.contains("command = 'secadm generateenrollmentinfo --verbose --xmlfile file:%s' % (os.path.basename(file_to_upload))"))
                .anyMatch(s -> s.contains("check_status, check_output = generate_enrollment_info('node.xml', 'enrollmentConfigurationFile.xml')"))
                .anyMatch(s -> s.contains("logging.basicConfig(filename='generateEnrollmentInfo.log', level=logging.INFO)"))
                .anyMatch(s -> s.contains("output['generateEnrollmentInfoStatus'] = generateEnrollmentInfoStatus"));
    }

    @Test
    public void failForBlankMandatoryParameterInGenerateEnrollmentScript() {
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(OPERATION_RESPONSE, "");
        assertThatThrownBy(() -> ossTopologyService.generateGetEnrollmentInfoScript(topologyAttributes, ENROLLMENT_INFO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter operationResponse was found to be null or empty");
    }

    @Test
    public void failForNullMandatoryParameterInGenerateEnrollmentScript() {
        final Map<String, Object> topologyAttributes = new HashMap<>();
        topologyAttributes.put(OPERATION_RESPONSE, null);
        assertThatThrownBy(() -> ossTopologyService.generateGetEnrollmentInfoScript(topologyAttributes, ENROLLMENT_INFO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The parameter operationResponse was not provided");
    }

    @Test
    public void generatePythonScriptForImportBackup() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("restore-backup-id", IMPORT_BACKUP);
        topologyAttributes.put(BACKUP_FILE_REF, "ftp://admin@14BCP04");
        topologyAttributes.put(BACKUP_FILE_REF_PASSWORD, "fasfasf");
        topologyAttributes.put(ACTION_ID, "");
        topologyAttributes.put(BACKUP_FILE, "");
        final Path pythonScript = ossTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, IMPORT_BACKUP);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript))
                .anyMatch(s -> s.contains("if \"importBackup\" == \"importBackup\":"))
                .anyMatch(s -> s.contains("command1 = 'cmedit action MeContext=restore-backup-id,ManagedElement=restore-backup-id,brm=1,"
                                                  + "backup-manager=configuration-system import-backup.(uri=\"ftp://admin@14BCP04\","
                                                  + "password=fasfasf)'"))
                .anyMatch(s -> s.contains("json.dumps(import_backup_data)"));
    }

    @Test
    public void generatePythonScriptForPollImportProgress() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("restore-backup-id", IMPORT_BACKUP_PROGRESS);
        topologyAttributes.put(BACKUP_FILE_REF, "ftp://admin@14BCP04");
        topologyAttributes.put(BACKUP_FILE_REF_PASSWORD, "fasfasf");
        topologyAttributes.put(ACTION_ID, "action-id-ref");
        topologyAttributes.put(BACKUP_FILE, "");
        final Path pythonScript = ossTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, IMPORT_BACKUP_PROGRESS);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript))
                .anyMatch(s -> s.contains("if \"importBackupProgress\" == \"importBackupProgress\":"))
                .anyMatch(s -> s.contains("command1 = 'cmedit show MeContext=restore-backup-id,ManagedElement=restore-backup-id,brm=1,"
                                                  + "backup-manager=configuration-system,progress-report=action-id-ref'"))
                .anyMatch(s -> s.contains("json.dumps(import_progress)"));
    }

    @Test
    public void generatePythonScriptForRestoreWithUriBkup() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("restore-backup-id", RESTORE_BACKUP);
        topologyAttributes.put(BACKUP_FILE_REF, "ftp://admin@14BCP04");
        topologyAttributes.put(BACKUP_FILE_REF_PASSWORD, "fasfasf");
        topologyAttributes.put(ACTION_ID, "action-id-ref");
        topologyAttributes.put(BACKUP_FILE, "backupFile");
        final Path pythonScript = ossTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, RESTORE_BACKUP);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript))
                .anyMatch(s -> s.contains("if \"restoreBackup\" == \"restoreBackup\":"))
                .anyMatch(s -> s.contains(
                        "command1 = 'cmedit action MeContext=restore-backup-id,ManagedElement=restore-backup-id,brm=1,"
                                + "backup-manager=configuration-system,backup=backupFile restore'"))
                .anyMatch(s -> s.contains("json.dumps(restore_backup_data)"));
    }

    @Test
    public void generatePythonScriptForRestoreFromLatestBackup() throws IOException {
        final Map<String, Object> topologyAttributes = addCommonScriptAttributes("restore-backup-id", RESTORE_LATEST_BACKUP);
        topologyAttributes.put(BACKUP_FILE_REF, "ftp://admin@14BCP04");
        topologyAttributes.put(BACKUP_FILE_REF_PASSWORD, "fasfasf");
        topologyAttributes.put(ACTION_ID, "");
        topologyAttributes.put(BACKUP_FILE, "");
        final Path pythonScript = ossTopologyService.generateRestoreScript(topologyAttributes, RESTORE_BACKUP_FILE, RESTORE_LATEST_BACKUP);
        assertThat(pythonScript).exists();
        assertThat(pythonScript.getFileName().toString()).endsWith(".py");
        assertThat(Files.readAllLines(pythonScript))
                .anyMatch(s -> s.contains("if \"restoreLatestBackup\" == \"restoreLatestBackup\":"))
                .anyMatch(s -> s.contains(
                        "command1 = 'cmedit action MeContext=restore-backup-id, ManagedElement=restore-backup-id,brm=1,"
                                + "backup-manager=configuration-system,backup=Latest restore'"))
                .anyMatch(s -> s.contains("json.dumps(restore_latest_backup_data)"));
    }
}
