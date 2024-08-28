/*******************************************************************************
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
 ******************************************************************************/
--------- VNF instances ---------

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, namespace,
current_life_cycle_operation_id, supported_operations, oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
combined_values_file, combined_additional_params, policies, resource_details, mano_controlled_scaling, temp_instance, override_global_registry,
metadata, alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file, oss_node_protocol_file, sensitive_info, bro_endpoint_url,
vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, is_rel4, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a250', 'vnf-instance-completed-1', 'vnf-instance-completed-desc-1', 'vnf-instance-completed-vnfd-1',
'vnf-provider-completed-1', 'vnf-product-completed-1', 'vnf-instance-software-version-1', 'vnf-instance-vnfd-version-1',
'vnf-instance-completed-package-1', 'INSTANTIATED', 'testClusterName', 'testNamespace', 'd3def1ce-4cf4-477c-aab3-21c454e6a352', '[
      {
        "operationName": "instantiate",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "terminate",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "heal",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "change_package",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "scale",
        "supported": true,
        "errorMessage": null
      }
    ]',
'testOssTopology250', '{"testInstantiateOssTopology250":"test"}', 'testAddNodeOssTopology250', true, 'testCombinedValuesFile250',
'testCombinedAdditionalParams250', 'testPolicies250', 'testResourceDetails250', true, 'testTempInstance250', true, '{"testMetadata250":"test"}',
'testAlarmSupervisionStatus250', true, true, 'testSitebasicFile250', 'testOssNodeProtocolFile250', 'testSensitiveInfo250', 'testBroEndpointUrl250',
'{"testVnfInfoModifiableAttributesExtensions250":"test"}', 'testInstantiationLevel250', 'testCrdNamespace250', true, 'testHelmClientVersion250');

--------- LCM operations ---------

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a350', 'd3def1ce-4cf4-477c-aab3-21c454e6a250', 'COMPLETED',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId350', 'INSTANTIATE', true, '{"testOperationParams350":"test"}', false,
'FORCEFUL', null,'testValuesFileParams350', 'testVnfSoftwareVersion350', 'testVnfProductName350', 'testCombinedValuesFile350',
'testCombinedAdditionalParams350','{"testResourceDetails350":1}', '{"testScaleInfoEntities350":"test"}', 'testSourceVnfdId350', 'testTargetVnfdId350',
true, 'testDeleteNodeErrorMessage350',true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage350', true, true,
'testRollbackFailurePattern350', 'testVnfInfoModifiableAttributesExtensions350', 'testInstantiationLevel350', 'testRollbackPattern350',
'testUsername350', 'testHelmClientVersion350');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a351', 'd3def1ce-4cf4-477c-aab3-21c454e6a250', 'ROLLED_BACK',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId350', 'CHANGE_VNFPKG', true, '{"testOperationParams350":"test"}', false,
'FORCEFUL', null,'testValuesFileParams350', 'testVnfSoftwareVersion350', 'testVnfProductName350', 'testCombinedValuesFile350',
'testCombinedAdditionalParams350','{"testResourceDetails350":1}', '{"testScaleInfoEntities350":"test"}', 'testSourceVnfdId350', 'testTargetVnfdId350',
true, 'testDeleteNodeErrorMessage350',true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage350', true, true,
'testRollbackFailurePattern350', 'testVnfInfoModifiableAttributesExtensions350', 'testInstantiationLevel350', 'testRollbackPattern350',
'testUsername350', 'testHelmClientVersion350');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a352', 'd3def1ce-4cf4-477c-aab3-21c454e6a250', 'FAILED',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId350', 'SCALE', true, '{"testOperationParams350":"test"}', false,
'FORCEFUL', null,'testValuesFileParams350', 'testVnfSoftwareVersion350', 'testVnfProductName350', 'testCombinedValuesFile350',
'testCombinedAdditionalParams350','{"testResourceDetails350":1}', '{"testScaleInfoEntities350":"test"}', 'testSourceVnfdId350', 'testTargetVnfdId350',
true, 'testDeleteNodeErrorMessage350',true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage350', true, true,
'testRollbackFailurePattern350', 'testVnfInfoModifiableAttributesExtensions350', 'testInstantiationLevel350', 'testRollbackPattern350',
'testUsername350', 'testHelmClientVersion350');

--------- Helm Charts ---------

INSERT INTO helm_chart(id, helm_chart_name, helm_chart_version, helm_chart_type, helm_chart_artifact_key,
helm_chart_url, priority, release_name, revision_number, state, retry_count, delete_pvc_state, downsize_state,
replica_details, vnf_id)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a450', 'testHelmChartName450', 'testHelmChartVersion450', 'CNF', 'testHelmChartArtifactKey450',
'testHelmChartUrl450', 1, 'testReleaseName450', 'testRevisionNumber450', 'testState450', 3, 'testDeletePvcState450', 'testDownsizeState450',
'testReplicaDetails450', 'd3def1ce-4cf4-477c-aab3-21c454e6a250');

INSERT INTO helm_chart(id, helm_chart_name, helm_chart_version, helm_chart_type, helm_chart_artifact_key,
helm_chart_url, priority, release_name, revision_number, state, retry_count, delete_pvc_state, downsize_state,
replica_details, vnf_id)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a451', 'testHelmChartName451', 'testHelmChartVersion451', 'CNF', 'testHelmChartArtifactKey451',
'testHelmChartUrl451', 0, 'testReleaseName451', 'testRevisionNumber451', 'testState451', 2, 'testDeletePvcState451', 'testDownsizeState451',
'testReplicaDetails451', 'd3def1ce-4cf4-477c-aab3-21c454e6a250');

--------- Scale Info ---------

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a550', 'd3def1ce-4cf4-477c-aab3-21c454e6a250', 'Aspect1', 0);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a551', 'd3def1ce-4cf4-477c-aab3-21c454e6a250', 'Aspect3', 2);

INSERT INTO app_cluster_config_file
(id,
 config_file_name,
 config_file_status,
 config_file_description,
 config_file,
 cluster_server)
 VALUES
 ('40',
 'hall914.config',
 'NOT_IN_USE',
 'test file',
 decode('61706956657273696F6E3A2076310D0A636C7573746572733A0D0A2D20636C75737465723A0D0A20202020696E7365637572652D736B69702D746C732D7665726966793A20747275650D0A202020207365727665723A2068747470733A2F2F746573742E636C75737465722E6572696373736F6E2E73653A3434330D0A20206E616D653A206B756265726E657465730D0A636F6E74657874733A0D0A2D20636F6E746578743A0D0A20202020636C75737465723A206B756265726E657465730D0A20202020757365723A206B756265726E657465732D61646D696E0D0A20206E616D653A206B756265726E657465732D61646D696E406B756265726E657465730D0A63757272656E742D636F6E746578743A206B756265726E657465732D61646D696E406B756265726E657465730D0A6B696E643A20436F6E6669670D0A707265666572656E6365733A207B7D0D0A75736572733A0D0A2D206E616D653A206B756265726E657465732D61646D696E0D0A2020757365723A0D0A20202020636C69656E742D63657274696669636174652D646174613A204C5330744C5331435255644A54694244520D0A20202020636C69656E742D6B65792D646174613A204C5330744C5331435255644A546942535530456755464A4A0D0A', 'hex'),
 'hall914.config');