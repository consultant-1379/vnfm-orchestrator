INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                     vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                     added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('a014e979-1e0e-48c0-98a9-caeda9787b7e', 'my-release-name-dynamic-capacity-selfupgrade-test-1', 'vnfInstanceDescription',
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

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                     vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                     added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('a014e979-1e0e-48c0-98a9-caeda9787b7d', 'my-release-name-dynamic-capacity-selfupgrade-test-2', 'vnfInstanceDescription',
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

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                     vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                     added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('1355196e-43e8-42bb-95ac-06082edf3ce3', 'my-release-name-dynamic-capacity-selfupgrade-test-3', 'vnfInstanceDescription',
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

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                     vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                     added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e97', 'my-release-name-dynamic-capacity-selfupgrade-test-4', 'vnfInstanceDescription',
'8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
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

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                     vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                     added_to_oss, is_heal_supported, combined_additional_params, combined_values_file, supported_operations)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'my-release-name-dynamic-capacity-selfupgrade-test-5', 'vnfInstanceDescription',
'8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
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

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('d7b553fd-f09c-421c-a238-947ce6b98853', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'COMPLETED', '2022-10-12 11:37:54.187494',
'2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4evuxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7OmkVZG04EXc0ftGvU6AqxBMLd51wa+c7GzVBuXYkj4p6KoDAN4dIBP/n6GomA9dzZ59FZihQ+8ObplHQyaLDYMzCNc/34iQXu7nQlNxDvxPSEfzCvyFSvkr3Ulqepfhww0yI1q+sMZ2JjryXnPVMh64VpRiigXStY0+cClQNqgG8q3+paCdaTh/BOaj5JJqotgu5hyxu3JRFt4aj8VSdV4xvCk+vujGHvSz6gXI2RoyzgURKJ6+rDZ46z2ZdCIcW7bcO2ssOZ63G2XHqPK7SlDdkVDr5/hSgti0Det9TRpkYYcRetgQJVvHDctPL34+AA=',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7d
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('d7b553fd-f09c-421c-a238-947ce6b988531', 'a014e979-1e0e-48c0-98a9-caeda9787b7d', 'COMPLETED', '2022-10-12 11:37:54.187494',
'2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'{"additionalParams": {"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 1355196e-43e8-42bb-95ac-06082edf3ce3
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('1355196e-43e8-42bb-95ac-06082edf3ce31', '1355196e-43e8-42bb-95ac-06082edf3ce3', 'COMPLETED', '2022-10-12 11:37:54.187494',
'2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'{"additionalParams": {"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca7481', '3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'COMPLETED', '2022-10-12 11:37:54.187494',
'2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4evuxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7OmkVZG04EXc0ftGvU6AqxBMLd51wa+c7GzVBuXYkj4p6KoDAN4dIBP/n6GomA9dzZ59FZihQ+8ObplHQyaLDYMzCNc/34iQXu7nQlNxDvxPSEfzCvyFSvkr3Ulqepfhww0yI1q+sMZ2JjryXnPVMh64VpRiigXStY0+cClQNqgG8q3+paCdaTh/BOaj5JJqotgu5hyxu3JRFt4aj8VSdV4xvCk+vujGHvSz6gXI2RoyzgURKJ6+rDZ46z2ZdCIcW7bcO2ssOZ63G2XHqPK7SlDdkVDr5/hSgti0Det9TRpkYYcRetgQJVvHDctPL34+AA=',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--CURRENT OPERATION UPGRADE FOR INSTANSE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('24e58306-4a80-42eb-a19f-19bc9b8c71a92', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'STARTING', '2022-10-12 13:16:13.250623',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"applicationTimeOut":360,"skipVerification":false}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4zURAnoM+Yni+WVZFa6m1zdQ4820S4dqBJHGITpI7o7zA5NAaCyhzX1QTJmIu3Wded5jaZjOn66h1Neu/o0xkVNYm1Zfkdsuhlf82r68MJSYlLnrKYJ/zRPBMS4tyzPwaXDhHT+ZBvZu4Dd1T0OQL+a6roYZ7yEFHiCzrzq/IfSJRo/Sa4RtHWE/NTJ2UfZ3/Q5Ox+ftrnZXj31KeM/Ed9ztjpPoiN4ay9LsSBpoaZ5rIZs6qROnzLtTc6AQvNLmi60B7s1g/3bznI9QrXVn2E3h8Q4PsIvr9MJwLdJb+enPFG5quN/vQ6CasbiTv8cbOs0kSUo/7njVW71h+HT6BWA==',
'1.0.2s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', null, null, null, false, null, false, null, null, false, false, null, null, null, null, 'vnfm');

--CURRENT OPERATION UPGRADE FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7d
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('24e58306-4a80-42eb-a19f-19bc9b8c71a922', 'a014e979-1e0e-48c0-98a9-caeda9787b7d', 'STARTING', '2022-10-12 13:16:13.250623',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222}}',
'1.0.2s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', null, null, null, false, null, false, null, null, false, false, null, null, null, null, 'vnfm');


--COMPLETED SELF UPGRADE FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('24e58306-4a80-42eb-a19f-19bc9b8c71a91', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'COMPLETED', '2022-10-12 12:20:57.508258',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '3600', false, true, null, null, '{}', null, 'vnfm');

--COMPLETED SELF UPGRADE FOR INSTANCE 1355196e-43e8-42bb-95ac-06082edf3ce3
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('1355196e-43e8-42bb-95ac-06082edf3ce32', '1355196e-43e8-42bb-95ac-06082edf3ce3', 'COMPLETED', '2022-10-12 12:20:57.508258',
'2022-10-12 12:20:57.508258', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '3600', false, true, null, null, '{}', null, 'vnfm');

--CURRENT OPERATION UPGRADE FOR INSTANCE 1355196e-43e8-42bb-95ac-06082edf3ce3
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('1355196e-43e8-42bb-95ac-06082edf3ce33', '1355196e-43e8-42bb-95ac-06082edf3ce3', 'STARTING', '2022-10-12 13:16:13.250623',
'2022-10-12 13:16:13.250623', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"eric-pm-bulk-reporter":{"replicaCount":336},"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332}}',
'1.0.2s', 'basic-app-a', null, null, null, 'single-chart-527c-arel4-5fcb086597zz', null, null, null, false, null, false,
null, null, false, false, null, null, null, null, 'vnfm');

--COMPLETED SELF UPGRADE FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca7482', '3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'COMPLETED', '2022-10-12 12:20:57.508258',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--COMPLETED SIMPLE UPGRADE FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca7483', '3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'COMPLETED', '2022-10-12 14:25:10.647779',
'2022-10-12 14:24:58.854779', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"eric-pm-bulk-reporter":{"replicaCount":336},"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332}}',
'1.0.2s', 'basic-app-b', '2022-10-12 16:26:59.553969', '{"disableOpenapiValidation":true}',
'{"eric-pm-bulk-reporter":{"replicaCount":336},"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332}"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--COMPLETED SELF UPGRADE FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca7484', '3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'COMPLETED', '2022-10-12 15:20:57.508258',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":441},"eric-pm-bulk-reporter":{"replicaCount":442}}',
'1.0.2s', 'basic-app-a', '2022-10-12 18:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc1":{"replicaCount":441},"eric-pm-bulk-reporter":{"replicaCount":442},"applicationTimeOut":360,"skipVerification":false}',
'8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--CURRENT OPERATION UPGRADE FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('3ef224c1-8c89-437e-bac7-fa6ec97ca7485', '3ef224c1-8c89-437e-bac7-fa6ec97ca748', 'STARTING', '2022-10-12 16:16:13.250623',
'2022-10-12 16:16:13.250623', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"eric-pm-bulk-reporter":{"replicaCount":555}}',
'1.0.2s', 'basic-app-a', null, null, null, '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', null, null, null, false, null, false,
null, null, false, false, null, null, null, null, 'vnfm');

--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 44b13853-58f9-414c-9fc4-69f8cc685e97
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e971', '44b13853-58f9-414c-9fc4-69f8cc685e97', 'COMPLETED', '2022-10-12 11:37:54.187494',
'2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
'1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

--COMPLETED SELF UPGRADE FOR INSTANCE 44b13853-58f9-414c-9fc4-69f8cc685e97
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e972', '44b13853-58f9-414c-9fc4-69f8cc685e97', 'COMPLETED', '2022-10-12 12:20:57.508258',
'2022-10-12 15:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222}}',
'1.0.2s', 'basic-app-a', '2022-10-12 16:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc3":{"replicaCount":223},"eric-pm-bulk-reporter":{"replicaCount":226},"vnfc1":{"replicaCount":221},"vnfc2":{"replicaCount":222},"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--COMPLETED SIMPLE UPGRADE FOR INSTANCE 3ef224c1-8c89-437e-bac7-fa6ec97ca748
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e973', '44b13853-58f9-414c-9fc4-69f8cc685e97', 'COMPLETED', '2022-10-12 14:25:10.647779',
'2022-10-12 14:24:58.854779', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"eric-pm-bulk-reporter":{"replicaCount":336},"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332}}',
'1.0.2s', 'basic-app-b', '2022-10-12 16:26:59.553969', '{"disableOpenapiValidation":true}',
'{"eric-pm-bulk-reporter":{"replicaCount":336},"vnfc1":{"replicaCount":331},"vnfc2":{"replicaCount":332}"applicationTimeOut":360,"skipVerification":false}',
'single-chart-527c-arel4-5fcb086597zz', '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--COMPLETED SELF UPGRADE FOR INSTANCE 44b13853-58f9-414c-9fc4-69f8cc685e97
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities,
                              delete_node_failed, delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message,
                              application_timeout, downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e974', '44b13853-58f9-414c-9fc4-69f8cc685e97', 'COMPLETED', '2022-10-12 15:20:57.508258',
'2022-10-12 16:16:13.250621', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"vnfc1":{"replicaCount":441},"eric-pm-bulk-reporter":{"replicaCount":442}}',
'1.0.2s', 'basic-app-a', '2022-10-12 18:22:47.031663', '{"day0.configuration.param2.value":"testpassword","day0.configuration.param2.key":"password","day0.configuration.secretname":"day0-secret","disableOpenapiValidation":true,"day0.configuration.param1.key":"login","day0.configuration.param1.value":"testlogin"}',
'{"vnfc1":{"replicaCount":441},"eric-pm-bulk-reporter":{"replicaCount":442},"applicationTimeOut":360,"skipVerification":false}',
'8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', '{"busybox":1}',
'[{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0}]', false, null, false, null, '360', false, true, null, null, '{}', null, 'vnfm');

--CURRENT OPERATION UPGRADE FOR INSTANCE 44b13853-58f9-414c-9fc4-69f8cc685e97
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time, start_time, grant_id,
                              lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode, error,
                              values_file_params, vnf_software_version, vnf_product_name, expired_application_time, combined_additional_params,
                              combined_values_file, source_vnfd_id, target_vnfd_id, resource_details, scale_info_entities, delete_node_failed,
                              delete_node_error_message, delete_node_finished, set_alarm_supervision_error_message, application_timeout,
                              downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern, instantiation_level,
                              vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('44b13853-58f9-414c-9fc4-69f8cc685e975', '44b13853-58f9-414c-9fc4-69f8cc685e97', 'STARTING', '2022-10-12 17:16:13.250623',
'2022-10-12 17:16:13.250623', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, '{"eric-pm-bulk-reporter":{"replicaCount":555}}',
'1.0.2s', 'basic-app-a', null, null, null, '8eca7b35-72d8-4cab-9fee-138f1c3b9cacc', null, null, null, false, null, false,
null, null, false, false, null, null, null, null, 'vnfm');

INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES
('44b13853-58f9-414c-9fc4-69f8cc685e972', 'UPGRADE', null),
('44b13853-58f9-414c-9fc4-69f8cc685e973', 'UPGRADE', null),
('44b13853-58f9-414c-9fc4-69f8cc685e974', 'UPGRADE', null);

INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                        life_cycle_operation_id)
VALUES ('fe878127-8390-488e-8be7-92f021512411',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
'my-release-name-dynamic-capacity-selfupgrade-test-4', 'COMPLETED',
'2', 1, '44b13853-58f9-414c-9fc4-69f8cc685e972');