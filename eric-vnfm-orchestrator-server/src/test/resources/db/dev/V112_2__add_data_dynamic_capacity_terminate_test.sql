INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
 vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
 added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('10e3a3b0-5993-42f5-86ff-f325b7500346', 'my-release-name-dynamic-capacity-terminate-1', 'vnfInstanceDescription',
'single-chart-527c-arel4-5fcb086597zz', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'43bf1225-81e1-46b4-rel41-cadea4432940', 'INSTANTIATED', 'granting-change-vnfpkg', false, true,
'{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MJ8r3xUXn+AWET+PuJ7bWVLD0/91ud9Gw40ltScpnphTykPDC8XWa5j3uDeZi0ztxKIlRjcFDsMI7G4F6PKVAXkUc0aAQYZ9U9eNfbcu3TZahhCnzqUln0GtWDM4SahSDg4aMbiRvEqoDE76f10CVBgLiVqr93Hev6o3Id3+RS/oq532AqkmMu9hLp3t2ToqISJldKL+dZplStQXV5s2mzNPcujdWCJ23oU7dCqevA7i9F0zWg4Zb+RwFFmDjS7m08eXjOFU0OIWIRciWpUp0r3sqkP62lnDzEbpesfsqDjkx1iNbw43+WpOf+B7gi0rY=',
'[
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
]'
);

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 10e3a3b0-5993-42f5-86ff-f325b7500346
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('dd1606ab-5f35-4748-9d49-f7622ba0d5ca', '10e3a3b0-5993-42f5-86ff-f325b7500346', 'COMPLETED', now()- INTERVAL '1 hours',
 now()- INTERVAL '1 hours', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

--CURRENT TERMINATE OPERATION FOR INSTANCE 10e3a3b0-5993-42f5-86ff-f325b7500346
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('4c001fc9-6ea1-40b4-bed1-de52c0195f4c', '10e3a3b0-5993-42f5-86ff-f325b7500346', 'STARTING',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);