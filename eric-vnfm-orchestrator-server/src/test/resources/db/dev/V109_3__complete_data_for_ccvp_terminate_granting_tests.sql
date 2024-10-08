INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc69a-0c2f-11ed-861d-0242ac120002
('254f9ab0-5c3d-4bb5-a602-9557c1827bc7', '186dc69a-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc8db-0c2f-11ed-861d-0242ac120002
('8d368c1a-f3c0-4614-93d1-13adbb597382', '186dc8db-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc9fc-0c2f-11ed-861d-0242ac120002
('00edf8f1-9bbf-4be3-bcf2-d7596ca577a3', '186dc9fc-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc3fd-0c2f-11ed-861d-0242ac120002
('c7c8e05a-f89d-424e-be5c-1e3983bc502e', '186dc3fd-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc6de-0c2f-11ed-861d-0242ac120002
('5cecf4bf-a1c7-46b4-a817-9387d78db256', '186dc6de-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dccff-0c2f-11ed-861d-0242ac120002
('28c9ccc2-2c37-4309-a868-628e02b2a727', '186dccff-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc69a-0c2f-11ed-861d-0242ac120003
('b237c4ab-40d9-42c7-8f92-5dd2817c25e3', '186dc69a-0c2f-11ed-861d-0242ac120003', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc69a-0c2f-11ed-861d-0242ac120004
('fa30a280-70a8-4e1b-a1d4-ad956d017610', '186dc69a-0c2f-11ed-861d-0242ac120004', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE 186dc69a-0c2f-11ed-861d-0242ac120005
('dad08d56-a773-46f3-bcc4-d9632d2f83ca', '186dc69a-0c2f-11ed-861d-0242ac120005', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE g3def1ce-4cf4-477c-aab3-21c454e6a393
('d7ade503-0b9c-4934-bb98-f113a3578fcd', 'g3def1ce-4cf4-477c-aab3-21c454e6a393', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zs', 'single-chart-527c-arel4-5fcb086597zs', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
 ('d7ade503-0b9c-4934-bb98-f113a3578777', 'g3def1ce-4cf4-477c-aab3-21c454e6a777', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zs', 'single-chart-527c-arel4-5fcb086597zs', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE g3def1ce-4cf4-477c-aab3-21c454e6a394
('04a17229-1bd1-4788-939e-697a57014d1b', 'g3def1ce-4cf4-477c-aab3-21c454e6a394', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zs', 'single-chart-527c-arel4-5fcb086597zs', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm'),
--COMPLETED INSTANTIATE OPERATION FOR INSTANCE g3def1ce-4cf4-477c-aab3-21c454e6a395
('868ea1dc-719d-430c-aa87-634671891cde', 'g3def1ce-4cf4-477c-aab3-21c454e6a395', 'COMPLETED', '2022-10-12 11:37:54.187494',
 '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zs', 'single-chart-527c-arel4-5fcb086597zs', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');