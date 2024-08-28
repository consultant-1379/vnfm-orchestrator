 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies, resource_details)
VALUES ('d8a8da6b-4488-4b14-a578-40w4f9f9e34q1256', 'vnf-release-scale-upgrade', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}', '{"PL__scaled_vm": 9, "CL_scaled_vm": 9}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('l08fcbc8-474f-4673-91ee-861fd864780965', 'd8a8da6b-4488-4b14-a578-40w4f9f9e34q1256', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ "type": "about:blank", "title": "Onboarding
Overloaded", "status": 503, "detail": "Onboarding service not available", "instance": "" }');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('21c454e6a389-4488-4c33', 'd8a8da6b-4488-4b14-a578-40w4f9f9e34q1256', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('21c454e6a389-4488-4c34', 'd8a8da6b-4488-4b14-a578-40w4f9f9e34q1256', 'Payload_2', 4);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('21c454e6a389-4488-4c35', 'd8a8da6b-4488-4b14-a578-40w4f9f9e34q1256', 'Payload', 3);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('bd7a1f49-6a3b-40b7-ab48-dd15d88332a7', 'msg-inst-failed', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'default66',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('713ec68e-708c-4dab-9cd4-826a5d3d31e1', 'bd7a1f49-6a3b-40b7-ab48-dd15d88332a7', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('865e3873-6a0e-443c-9b0c-4da9d9c2ab71', 'msg-chg-complete', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'ab3d12f2-8084-4975-9d33-0577aedc61b7', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv
 .ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}'),
 ('837dbdb3-2240-4d3b-b840-c9deafa98c83', 'msg-chg-complete-a', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 '837dbdb3-2240-4d3b-b840-c9deafa98c83', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b2d969', '865e3873-6a0e-443c-9b0c-4da9d9c2ab71',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-chg-complete'),
('e264eeca-3450-478e-9ed6-0a70448b710f', '837dbdb3-2240-4d3b-b840-c9deafa98c83',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-chg-complete-a');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('a4b29e7c-af08-459a-8040-0211c7eeda27', '865e3873-6a0e-443c-9b0c-4da9d9c2ab71', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('c07b8e5d-be55-4f0b-a989-8eb0b0071fb4', '865e3873-6a0e-443c-9b0c-4da9d9c2ab71', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('3f053fe1-8c8b-4448-963d-89101e00d443', '837dbdb3-2240-4d3b-b840-c9deafa98c83', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('ca35c546-4a75-4036-87b1-bdd070367a1b', '837dbdb3-2240-4d3b-b840-c9deafa98c83', 'Payload', 3);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('865e3873-6a0e-443c-9b0c-4da9d9c2ab71', '865e3873-6a0e-443c-9b0c-4da9d9c2ab71', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null),
('837dbdb3-2240-4d3b-b840-c9deafa98c83', '837dbdb3-2240-4d3b-b840-c9deafa98c83', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null),
('ab3d12f2-8084-4975-9d33-0577aedc61b7', '865e3873-6a0e-443c-9b0c-4da9d9c2ab71', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('73f80147-8d0e-43b8-a017-84713feb8255', 'msg-inst-complete', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('b744d48d-ec2f-446f-8d65-02832b534b72', '73f80147-8d0e-43b8-a017-84713feb8255', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('804a34ac-d35d-4ba7-8a6a-24561e2ea4d7', '73f80147-8d0e-43b8-a017-84713feb8255', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('a375be4e-0768-4a67-84a9-3b2192f7ce28', 'msg-inst-already-complete', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('71aa2228-96b5-4ce6-9996-8aa994dd841a', 'a375be4e-0768-4a67-84a9-3b2192f7ce28', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('ecf7d304-972b-4324-9342-3a7095f7d194', 'a375be4e-0768-4a67-84a9-3b2192f7ce28', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('ard5be4e-0768-4a67-84a9-3b2192f7ce28', 'msg1-inst-already-complete', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('6rda2228-96b5-4ce6-9996-8aa994dd841a', 'ard5be4e-0768-4a67-84a9-3b2192f7ce28', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);
