--FAILED INSTANTIATE OPERATION FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('d7b553fd-f09c-421c-a238-947ce6b98860', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'FAILED', '2022-10-12 16:37:54.187494',
'2022-10-11 14:37:39.276360', null, 'INSTANTIATE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4evuxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=',
'1.0.2s', 'basic-app-a', '2022-10-11 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7OmkVZG04EXc0ftGvU6AqxBMLd51wa+c7GzVBuXYkj4p6KoDAN4dIBP/n6GomA9dzZ59FZihQ+8ObplHQyaLDYMzCNc/34iQXu7nQlNxDvxPSEfzCvyFSvkr3Ulqepfhww0yI1q+sMZ2JjryXnPVMh64VpRiigXStY0+cClQNqgG8q3+paCdaTh/BOaj5JJqotgu5hyxu3JRFt4aj8VSdV4xvCk+vujGHvSz6gXI2RoyzgURKJ6+rDZ46z2ZdCIcW7bcO2ssOZ63G2XHqPK7SlDdkVDr5/hSgti0Det9TRpkYYcRetgQJVvHDctPL34+AA=',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--FAILED CCVP OPERATION FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('d7b553fd-f09c-421c-a238-947ce6b98861', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'FAILED', '2022-10-12 16:37:54.187494',
'2022-10-12 17:37:39.276360', null, 'CHANGE_VNFPKG', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4evuxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=',
'1.0.2s', 'basic-app-a', '2022-10-11 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7OmkVZG04EXc0ftGvU6AqxBMLd51wa+c7GzVBuXYkj4p6KoDAN4dIBP/n6GomA9dzZ59FZihQ+8ObplHQyaLDYMzCNc/34iQXu7nQlNxDvxPSEfzCvyFSvkr3Ulqepfhww0yI1q+sMZ2JjryXnPVMh64VpRiigXStY0+cClQNqgG8q3+paCdaTh/BOaj5JJqotgu5hyxu3JRFt4aj8VSdV4xvCk+vujGHvSz6gXI2RoyzgURKJ6+rDZ46z2ZdCIcW7bcO2ssOZ63G2XHqPK7SlDdkVDr5/hSgti0Det9TRpkYYcRetgQJVvHDctPL34+AA=',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');

--COMPLETED SCALE OPERATION FOR INSTANCE a014e979-1e0e-48c0-98a9-caeda9787b7e
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                              start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                              cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
                              combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
                              scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
                              set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
                              rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES ('d7b553fd-f09c-421c-a238-947ce6b98862', 'a014e979-1e0e-48c0-98a9-caeda9787b7e', 'COMPLETED', '2022-10-12 16:37:54.187494',
'2022-10-12 17:37:39.276360', null, 'SCALE', false,
'{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
false, null, null, 'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7MkdkortTcRbKG8I7j5NYHK+i8BWxlS+u6mWm+R5LJ0g3iOoXWkgSjXwKnLNCEI4vU4BDeqICSIU85k/4nuyRIsbptkJXEjpM4evuxBu0FRWP4Xpr9lhy46SILbFClOR2kuu1CC8c8A+KKyH3FNgF7tENfVOOv0bGzSQ987vbo8GxpU1R+m6Aez8PSiaVclfXXHtcposHV70PVIMAfDMd1g0uw/rW/QXfgSARnEmeYlTXxrxvNM0/8Veq23fWuMcgWAANao78hyU4KduQr32XrEV8CTYWgsrbz7cZoW37bOEzCzzwzKTw7NzDXYGmzuRjI=',
'1.0.2s', 'basic-app-a', '2022-10-11 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
'AAEGMtUFf8JC15JwOe5ElVs4CDsJOX0FFwXco3AvKPJe9U8rYCd2V8nKiBAGAltgs7OmkVZG04EXc0ftGvU6AqxBMLd51wa+c7GzVBuXYkj4p6KoDAN4dIBP/n6GomA9dzZ59FZihQ+8ObplHQyaLDYMzCNc/34iQXu7nQlNxDvxPSEfzCvyFSvkr3Ulqepfhww0yI1q+sMZ2JjryXnPVMh64VpRiigXStY0+cClQNqgG8q3+paCdaTh/BOaj5JJqotgu5hyxu3JRFt4aj8VSdV4xvCk+vujGHvSz6gXI2RoyzgURKJ6+rDZ46z2ZdCIcW7bcO2ssOZ63G2XHqPK7SlDdkVDr5/hSgti0Det9TRpkYYcRetgQJVvHDctPL34+AA=',
'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
false, null, false, null, '3600', false, false, null, null, '{}', null, 'vnfm');