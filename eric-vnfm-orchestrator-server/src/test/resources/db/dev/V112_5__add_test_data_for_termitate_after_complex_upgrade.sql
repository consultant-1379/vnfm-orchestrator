INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
 vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
 added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'my-release-name-dynamic-capacity-terminate-1', 'vnfInstanceDescription',
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

-- (1) COMPLETED INSTANTIATE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('cc3c26a9-60ef-4c3c-8c63-f98140b766bf', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '11 hours',
 now()- INTERVAL '11 hours', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"vnfc3":{"replicaCount":113},"vnfc5":{"replicaCount":115},"eric-pm-bulk-reporter":{"replicaCount":116}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}', null,
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

-- (2) COMPLETED SELF-UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('c83193bf-f7ae-448b-a9db-ce0d19cc2988', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '10 hours',
now()- INTERVAL '10 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
null, 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

-- (3) COMPLETED UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('86365a5a-9e85-4cfc-9dfe-5348de9e7532', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '9 hours',
now()- INTERVAL '9 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":441},"eric-pm-bulk-reporter":{"replicaCount":442}}',
'1.0.4s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');

-- (4) COMPLETED SELF-UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('75b2a709-9b87-4727-a90f-9ac7d79c750f', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '8 hours',
now()- INTERVAL '8 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":661},"eric-pm-bulk-reporter":{"replicaCount":662}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

-- (5) COMPLETED DOWNGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('7e9bf58b-ecda-4652-bf6d-08003d211f25', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '7 hours',
now()- INTERVAL '7 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":771},"eric-pm-bulk-reporter":{"replicaCount":772}}',
'1.0.2s', 'basic-app-a', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', 'single-chart-527c-arel4-5fcb086597zz', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');

-- (6) COMPLETED SELF-UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('20372f8c-6d9a-4065-a23d-a0cd7e9e58c4', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '6 hours',
now()- INTERVAL '6 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332},"eric-pm-bulk-reporter":{"replicaCount":336}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
null, 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

-- (7) COMPLETED UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('f78ef97f-7efb-4d61-b125-c98a2e325668', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '5 hours',
now()- INTERVAL '5 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":881},"eric-pm-bulk-reporter":{"replicaCount":882}}',
'1.0.4s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');

-- (8) COMPLETED SELF-UPGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('74a37723-4884-470a-9f21-6be75d6c498d', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '4 hours',
now()- INTERVAL '4 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":991},"eric-pm-bulk-reporter":{"replicaCount":992}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

-- (9) COMPLETED DOWNGRADE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('364e7b09-f801-4ba4-ace5-509b75cc4dcb', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'COMPLETED', now()- INTERVAL '3 hours',
now()- INTERVAL '3 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":1001},"eric-pm-bulk-reporter":{"replicaCount":1002}}',
'1.0.2s', 'basic-app-a', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', 'single-chart-527c-arel4-5fcb086597zz', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');


INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES
('86365a5a-9e85-4cfc-9dfe-5348de9e7532', 'UPGRADE', null),
('7e9bf58b-ecda-4652-bf6d-08003d211f25', 'DOWNGRADE', 'c83193bf-f7ae-448b-a9db-ce0d19cc2988'),
('f78ef97f-7efb-4d61-b125-c98a2e325668', 'UPGRADE', null),
('364e7b09-f801-4ba4-ace5-509b75cc4dcb', 'DOWNGRADE', '20372f8c-6d9a-4065-a23d-a0cd7e9e58c4');

INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                        life_cycle_operation_id)
VALUES
('62153344-ea3e-43cd-8ccb-13b6b09cf97b', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
'my-release-name-dynamic-capacity-selfupgrade-test-4', 'COMPLETED', '2', 1, 'c83193bf-f7ae-448b-a9db-ce0d19cc2988'),
('364e7b09-f801-4ba4-ace5-509b75cc4dcb', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
'my-release-name-dynamic-capacity-selfupgrade-test-4', 'COMPLETED', '2', 1, '20372f8c-6d9a-4065-a23d-a0cd7e9e58c4');


-- CURRENT TERMINATE OPERATION FOR INSTANCE 1eae9bd0-2968-496c-ad11-4d66d2b5bccb
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('b393592b-83eb-4838-ac17-b51611094b39', '1eae9bd0-2968-496c-ad11-4d66d2b5bccb', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);