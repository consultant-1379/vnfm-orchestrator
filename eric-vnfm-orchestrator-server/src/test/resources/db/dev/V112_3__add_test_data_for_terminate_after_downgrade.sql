INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
 vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
 added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('b213fdef-7a2c-4611-83ba-d6f3af804f27', 'my-release-name-dynamic-capacity-terminate-1', 'vnfInstanceDescription',
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

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE b213fdef-7a2c-4611-83ba-d6f3af804f27
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('d618b7b8-db27-4fd8-a845-34169caa5081', 'b213fdef-7a2c-4611-83ba-d6f3af804f27', 'COMPLETED', now()- INTERVAL '3 hours',
 now()- INTERVAL '3 hours', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

-- COMPLETED UPGRADE OPERATION FOR INSTANCE b213fdef-7a2c-4611-83ba-d6f3af804f27
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('a99216b7-9ee2-4bfb-8b22-9982685b31ae', 'b213fdef-7a2c-4611-83ba-d6f3af804f27', 'COMPLETED', now()- INTERVAL '2 hours',
now()- INTERVAL '2 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226}}',
'1.0.4s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');

-- COMPLETED DOWNGRADE OPERATION FOR INSTANCE b213fdef-7a2c-4611-83ba-d6f3af804f27
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('ef1b10f9-4580-4e43-90af-7f6eca464922', 'b213fdef-7a2c-4611-83ba-d6f3af804f27', 'COMPLETED', now()- INTERVAL '1 hours',
now()- INTERVAL '1 hours', null, 'CHANGE_VNFPKG', false, '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332},"eric-pm-bulk-reporter":{"replicaCount":336}}',
'1.0.2s', 'basic-app-a', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177', 'single-chart-527c-arel4-5fcb086597zz', null, null, false, null, false, null, null, false, false,
null, null, null, null, 'vnfm');

INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES
('a99216b7-9ee2-4bfb-8b22-9982685b31ae', 'UPGRADE', null),
('ef1b10f9-4580-4e43-90af-7f6eca464922', 'DOWNGRADE', 'd618b7b8-db27-4fd8-a845-34169caa5081');

INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                        life_cycle_operation_id)
VALUES ('2b8a3c4c-3022-446f-b518-cecd29970392',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
'my-release-name-dynamic-capacity-selfupgrade-test-4', 'COMPLETED',
'2', 1, 'd618b7b8-db27-4fd8-a845-34169caa5081');

--CURRENT TERMINATE OPERATION FOR INSTANCE b213fdef-7a2c-4611-83ba-d6f3af804f27
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('7acaef56-c78b-439e-b4a9-e47da64f233b', 'b213fdef-7a2c-4611-83ba-d6f3af804f27', 'STARTING',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);