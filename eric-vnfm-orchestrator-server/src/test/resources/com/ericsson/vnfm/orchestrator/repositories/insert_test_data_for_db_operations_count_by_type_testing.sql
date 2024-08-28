--------- VNF instances ---------

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, namespace,
current_life_cycle_operation_id, supported_operations, oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
combined_values_file, combined_additional_params, policies, resource_details, mano_controlled_scaling, temp_instance, override_global_registry,
metadata, alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file, oss_node_protocol_file, sensitive_info, bro_endpoint_url,
vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, is_rel4, helm_client_version)
VALUES ('ef410d07-ce1d-4c86-a73c-5343e3906a50', 'testVnfInstanceNameCountTest', 'testVnfInstanceDescriptionCountTest', 'testVnfInstanceDescriptorIdCountTest',
'testVnfProviderNameCountTest', 'testVnfProductNameCountTest', 'testVnfSoftwareVersionCountTest', 'testVnfdVersionCountTest', 'testVnfPackageIdCountTest',
'NOT_INSTANTIATED', 'testClusterNameCountTest', 'testNamespaceCountTest', 'd3def1ce-4cf4-477c-aab3-21c454e6a350', '[
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
'testOssTopologyCountTest', '{"testInstantiateOssTopologyCountTest":"test"}', 'testAddNodeOssTopologyCountTest', true, 'testCombinedValuesFileCountTest',
'testCombinedAdditionalParamsCountTest', 'testPoliciesCountTest', 'testResourceDetailsCountTest', true, 'testTempInstanceCountTest', true, '{"testMetadataCountTest":"test"}',
'testAlarmSupervisionStatusCountTest', true, true, 'testSitebasicFileCountTest', 'testOssNodeProtocolFileCountTest', 'testSensitiveInfoCountTest', 'testBroEndpointUrlCountTest',
'{"testVnfInfoModifiableAttributesExtensionsCountTest":"test"}', 'testInstantiationLevelCountTest', 'testCrdNamespaceCountTest', true, 'testHelmClientVersionCountTest');

--------- LCM operations ---------
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('81e556a5-3cfc-4265-adb6-8adb759690a7', 'ef410d07-ce1d-4c86-a73c-5343e3906a50', 'STARTING',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId355', 'INSTANTIATE', true, '{"testOperationParams355":"test"}', false, 'GRACEFUL', null,
'testValuesFileParams355', 'testVnfSoftwareVersion355', 'testVnfProductName355', 'testCombinedValuesFile355', 'testCombinedAdditionalParams355',
'{"testResourceDetails355":1}', '{"testScaleInfoEntities355":"test"}', 'testSourceVnfdId355', 'testTargetVnfdId355', true, 'testDeleteNodeErrorMessage355',
true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage355', true, true,
'testRollbackFailurePattern355', 'testVnfInfoModifiableAttributesExtensions355', 'testInstantiationLevel355', 'testRollbackPattern355',
'testUsername355', 'testHelmClientVersion355');
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('c862eb3b-96c8-4e2c-886e-98050c3c4f49', 'ef410d07-ce1d-4c86-a73c-5343e3906a50', 'PROCESSING',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId355', 'INSTANTIATE', true, '{"testOperationParams355":"test"}', false, 'GRACEFUL', null,
'testValuesFileParams355', 'testVnfSoftwareVersion355', 'testVnfProductName355', 'testCombinedValuesFile355', 'testCombinedAdditionalParams355',
'{"testResourceDetails355":1}', '{"testScaleInfoEntities355":"test"}', 'testSourceVnfdId355', 'testTargetVnfdId355', true, 'testDeleteNodeErrorMessage355',
true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage355', true, true,
'testRollbackFailurePattern355', 'testVnfInfoModifiableAttributesExtensions355', 'testInstantiationLevel355', 'testRollbackPattern355',
'testUsername355', 'testHelmClientVersion355');
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('6826d046-051f-47b3-9a0b-e7372e7ad86a', 'ef410d07-ce1d-4c86-a73c-5343e3906a50', 'ROLLING_BACK',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId355', 'INSTANTIATE', true, '{"testOperationParams355":"test"}', false, 'GRACEFUL', null,
'testValuesFileParams355', 'testVnfSoftwareVersion355', 'testVnfProductName355', 'testCombinedValuesFile355', 'testCombinedAdditionalParams355',
'{"testResourceDetails355":1}', '{"testScaleInfoEntities355":"test"}', 'testSourceVnfdId355', 'testTargetVnfdId355', true, 'testDeleteNodeErrorMessage355',
true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage355', true, true,
'testRollbackFailurePattern355', 'testVnfInfoModifiableAttributesExtensions355', 'testInstantiationLevel355', 'testRollbackPattern355',
'testUsername355', 'testHelmClientVersion355');
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
values_file_params, vnf_software_version, vnf_product_name, combined_values_file, combined_additional_params,
resource_details, scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed, delete_node_error_message,
delete_node_finished, application_timeout, expired_application_time, set_alarm_supervision_error_message, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, vnf_info_modifiable_attributes_extensions, instantiation_level, rollback_pattern,
username, helm_client_version)
VALUES ('6ebf272d-32ad-44aa-8f92-7e0829da1342', 'ef410d07-ce1d-4c86-a73c-5343e3906a50', 'COMPLETED',
'2024-03-31 12:30:45-08', '2024-03-31 12:30:45-08', 'testGrantId355', 'INSTANTIATE', true, '{"testOperationParams355":"test"}', false, 'GRACEFUL', null,
'testValuesFileParams355', 'testVnfSoftwareVersion355', 'testVnfProductName355', 'testCombinedValuesFile355', 'testCombinedAdditionalParams355',
'{"testResourceDetails355":1}', '{"testScaleInfoEntities355":"test"}', 'testSourceVnfdId355', 'testTargetVnfdId355', true, 'testDeleteNodeErrorMessage355',
true, '1500', '2024-03-31 12:31:45-08', 'testSetAlarmSupervisionErrorMessage355', true, true,
'testRollbackFailurePattern355', 'testVnfInfoModifiableAttributesExtensions355', 'testInstantiationLevel355', 'testRollbackPattern355',
'testUsername355', 'testHelmClientVersion355');