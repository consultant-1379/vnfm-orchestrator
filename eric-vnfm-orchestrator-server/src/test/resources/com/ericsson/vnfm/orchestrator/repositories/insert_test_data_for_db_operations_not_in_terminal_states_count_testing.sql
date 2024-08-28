--------- VNF instances ---------

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, namespace,
current_life_cycle_operation_id, supported_operations, oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
combined_values_file, combined_additional_params, policies, resource_details, mano_controlled_scaling, temp_instance, override_global_registry,
metadata, alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file, oss_node_protocol_file, sensitive_info, bro_endpoint_url,
vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, is_rel4, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a256', 'testVnfInstanceName256', 'testVnfInstanceDescription256', 'testVnfInstanceDescriptorId256',
'testVnfProviderName256', 'testVnfProductName256', 'testVnfSoftwareVersion256', 'testVnfdVersion256', 'testVnfPackageId256',
'NOT_INSTANTIATED', 'testClusterName256', 'testNamespace256', 'd3def1ce-4cf4-477c-aab3-21c454e6a356', '[
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
'testOssTopology256', '{"testInstantiateOssTopology256":"test"}', 'testAddNodeOssTopology256', true, 'testCombinedValuesFile256',
'testCombinedAdditionalParams256', 'testPolicies256', 'testResourceDetails256', true, 'testTempInstance256', true, '{"testMetadata256":"test"}',
'testAlarmSupervisionStatus256', true, true, 'testSitebasicFile256', 'testOssNodeProtocolFile256', 'testSensitiveInfo256', 'testBroEndpointUrl256',
'{"testVnfInfoModifiableAttributesExtensions256":"test"}', 'testInstantiationLevel256', 'testCrdNamespace256', true, 'testHelmClientVersion256');

--------- LCM operations ---------

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a356', 'd3def1ce-4cf4-477c-aab3-21c454e6a256', 'STARTING',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId356', 'INSTANTIATE', true, '{"testOperationParams356":"test"}', false,
'FORCEFUL', null,'testValuesFileParams356', 'testVnfSoftwareVersion356', 'testVnfProductName356', 'testCombinedValuesFile356',
'testCombinedAdditionalParams356','{"testResourceDetails356":1}', '{"testScaleInfoEntities356":"test"}', 'testSourceVnfdId356',
'testTargetVnfdId356', true, 'testDeleteNodeErrorMessage356', true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage356',
true, true, 'testRollbackFailurePattern356', 'testVnfInfoModifiableAttributesExtensions356', 'testInstantiationLevel356', 'testRollbackPattern356',
'testUsername356', 'testHelmClientVersion356');


INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a357', 'd3def1ce-4cf4-477c-aab3-21c454e6a256', 'PROCESSING',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId357', 'CHANGE_VNFPKG', true, '{"testOperationParams357":"test"}', false,
'GRACEFUL', null,'testValuesFileParams357', 'testVnfSoftwareVersion357', 'testVnfProductName357', 'testCombinedValuesFile357',
'testCombinedAdditionalParams357','{"testResourceDetails357":1}', '{"testScaleInfoEntities357":"test"}', 'testSourceVnfdId357', 'testTargetVnfdId357',
true, 'testDeleteNodeErrorMessage357', true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage357', true, true,
'testRollbackFailurePattern357', 'testVnfInfoModifiableAttributesExtensions357', 'testInstantiationLevel357', 'testRollbackPattern357',
'testUsername357', 'testHelmClientVersion357');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a358', 'd3def1ce-4cf4-477c-aab3-21c454e6a256', 'COMPLETED',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId358', 'INSTANTIATE', true, '{"testOperationParams358":"test"}', false,
'FORCEFUL', null,'testValuesFileParams358', 'testVnfSoftwareVersion358', 'testVnfProductName358', 'testCombinedValuesFile358',
'testCombinedAdditionalParams358','{"testResourceDetails358":1}', '{"testScaleInfoEntities358":"test"}', 'testSourceVnfdId358',
'testTargetVnfdId358', true, 'testDeleteNodeErrorMessage358', true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage358',
true, true, 'testRollbackFailurePattern358', 'testVnfInfoModifiableAttributesExtensions358', 'testInstantiationLevel358', 'testRollbackPattern358',
'testUsername358', 'testHelmClientVersion358');