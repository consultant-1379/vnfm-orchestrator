---------------------- modify with single helm chart -----------------------
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('865e3873-6a0e-443c-9b0c-4da9d9c2543', 'msg-mod-complete', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 'ab3d12f2-8084-4975-9d33-0577aedc61b7', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv
 .ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1
 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps
 to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies, metadata, vnf_info_modifiable_attributes_extensions)
VALUES ('865e3873-6a0e-443c-9b0c-4da9d9c2543-temp', 'msg-mod-complete-updated', 'vnfInstanceDescription-updated',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-updated', 'INSTANTIATED', 'default66',
 'ab3d12f2-8084-4975-9d33-0577aedc61b7', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv
 .ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1
 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps
 to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},
 "allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}',
 '{"tenantName": "ecm"}', '{"vnfControlledScaling":{"Aspect1":"ManualControlled"}}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b2d96', '865e3873-6a0e-443c-9b0c-4da9d9c2543',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-mod-complete');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b2d12212', '865e3873-6a0e-443c-9b0c-4da9d9c2543-temp',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-mod-complete');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('a4b29e7c-af08-459a-8040-0211c7eeda', '865e3873-6a0e-443c-9b0c-4da9d9c2543', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('c07b8e5d-be55-4f0b-a989-8eb0b0071f',
'865e3873-6a0e-443c-9b0c-4da9d9c2543', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('a4b29e7c-af08-459a-8040-0211c7eedg',
'865e3873-6a0e-443c-9b0c-4da9d9c2543-temp', 'Payload', 0);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('c07b8e5d-be55-4f0b-a989-8eb0b007dg',
'865e3873-6a0e-443c-9b0c-4da9d9c2543-temp', 'Payload', 0);
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, vnf_info_modifiable_attributes_extensions)
VALUES
('865e3873-6a0e-443c-9b0c-4da9d9c2ab7', '865e3873-6a0e-443c-9b0c-4da9d9c2543', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'MODIFY_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null, '{"vnfControlledScaling":{"Aspect1":"ManualControlled"}}');

INSERT INTO changed_info(id, vnf_pkg_id, vnf_instance_description, metadata)
VALUES
('865e3873-6a0e-443c-9b0c-4da9d9c2ab7', '9392468011745350001-updated', 'vnfInstanceDescription-updated',
'{"tenantName": "ecm"}');

---------------------- modify with multiple helm charts -----------------------

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('865e3873-6a0e-443c-9b0c-4da9d9c2222', 'msg-mod-complete-multi', 'vnfInstanceDescription-multi',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-multi', 'INSTANTIATED', 'default66',
 'ab3d12f2-8084-4975-9d33-0577aedc61b7', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv
 .ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1
 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps
 to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies, metadata, vnf_info_modifiable_attributes_extensions)
VALUES ('865e3873-6a0e-443c-9b0c-4da9d9c2222-temp', 'msg-mod-complete-multi-updated', 'vnfInstanceDescription-multi-updated',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-multi-updated', 'INSTANTIATED', 'default66',
 'ab3d12f2-8084-4975-9d33-0577aedc61b7', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv
 .ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1
 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps
 to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},
 "allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}',
 '{"tenantName": "ecm"}', '{"vnfControlledScaling":{"Aspect1":"ManualControlled"}}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b222', '865e3873-6a0e-443c-9b0c-4da9d9c2222',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-mod-multi-complete-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b2d12213', '865e3873-6a0e-443c-9b0c-4da9d9c2222',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-test--2.74.6.tgz', '2',
'msg-mod-multi-complete-2');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b333', '865e3873-6a0e-443c-9b0c-4da9d9c2222-temp',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-mod-multi-complete-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('8515ba79-35b4-4467-881b-2ee2d8b2d122333', '865e3873-6a0e-443c-9b0c-4da9d9c2222-temp',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-test--2.74.6.tgz', '2',
'msg-mod-multi-complete-2');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('a4b29e7c-3333-459a-8040-0211c7eeda',
'865e3873-6a0e-443c-9b0c-4da9d9c2222', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('c07b8e5d-3333be55-4f0b-a989-8eb0b0071f',
'865e3873-6a0e-443c-9b0c-4da9d9c2222', 'Payload', 3);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('a4b29e7c-3333-459a-8040-0211c7eedg',
'865e3873-6a0e-443c-9b0c-4da9d9c2222-temp', 'Payload', 0);
INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level) VALUES ('c07b8e5d-4444-4f0b-a989-8eb0b007dg',
'865e3873-6a0e-443c-9b0c-4da9d9c2222-temp', 'Payload', 0);
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, vnf_info_modifiable_attributes_extensions)
VALUES
('865e3873-6a0e-443c-9b0c-4da9d9c23434', '865e3873-6a0e-443c-9b0c-4da9d9c2222', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'MODIFY_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null, '{"vnfControlledScaling":{"Aspect1":"ManualControlled"}}');

INSERT INTO changed_info(id, vnf_pkg_id, vnf_instance_description, metadata)
VALUES
('865e3873-6a0e-443c-9b0c-4da9d9c23434', '9392468011745350001-multi-updated', 'vnfInstanceDescription-multi-updated',
'{"tenantName": "ecm"}');
