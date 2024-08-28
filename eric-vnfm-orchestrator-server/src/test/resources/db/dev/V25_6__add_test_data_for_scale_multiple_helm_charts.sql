INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, combined_additional_params,  policies, resource_details)
VALUES
('values-4cf4-477c-aab3-21c454e6a380', 'values-sca1e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'values-6a0e-443c-9b0c-4da9d9c2ab71', 'values', 'valuesscale', '{"Payload_InitialDelta.replicaCount":3,
 "Payload_InitialDelta_1.replicaCount":1, "helmWait":true, "manoControlledScaling":true, "helmNoHooks":true, "disableOpenapiValidation":true,
 "skipJobVerification":true, "skipVerification":true, "commandTimeOut":400, "applicationTimeOut":500, "pvcTimeOut":200, "cleanUpResources":true}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","CL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),


('sca1e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca1e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca1e-6a0e-443c-9b0c-4da9d9c2ab71', 'multiplesca1e', 'testscale', '{"Payload_InitialDelta.replicaCount":3,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload", "description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),

('sca2e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca2e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca2e-2240-4d3b-b840-c9deafa98c83', 'multiplesca2e', 'testscale', '{"Payload_InitialDelta.replicaCount":5,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload", "description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),

('sca3e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca3e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca3e-8084-4975-9d33-0577aedc61b7', 'multiplesca3e', 'testscale', '{"Payload_InitialDelta.replicaCount":6,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","CL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),

('sca4e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca4e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca4e-6a0e-443c-9b0c-4da9d9c2ab71', 'multiplesca1e', 'testscale', '{"Payload_InitialDelta.replicaCount":3,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","CL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),

('sca5e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca5e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca5e-6a0e-443c-9b0c-4da9d9c2ab71', 'multiplesca1e', 'testscale', '{"Payload_InitialDelta.replicaCount":3,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","CL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}'),

('sca6e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca6e', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca6e-6a0e-443c-9b0c-4da9d9c2ab71', 'multiplesca1e', 'testscale', '{"Payload_InitialDelta.replicaCount":3,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","CL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}');



INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('sca1e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca1e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null),

('sca2e-2240-4d3b-b840-c9deafa98c83', 'sca2e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE',
'FORCEFUL', null),

('sca3e-8084-4975-9d33-0577aedc61b7', 'sca3e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE',
'FORCEFUL', null),

('sca4e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca4e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null),

('sca5e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca5e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null),

('sca6e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca6e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null);


INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES
('sca1e-4cf4-477c-aab3-21c454e6a382', 'sca1e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca1e-1', 'PROCESSING'),

('sca2e-4cf4-477c-aab3-21c454e6a382', 'sca2e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'multiple-sca2e-2', null),
('sca21e-4cf4-477c-aab3-21c454e6a382', 'sca2e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca2e-1', 'PROCESSING'),

('sca3e-4cf4-477c-aab3-21c454e6a382', 'sca3e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'multiple-sca3e-1', null),
('sca31e-4cf4-477c-aab3-21c454e6a382', 'sca3e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca3e-2', 'PROCESSING'),

('sca4e-4cf4-477c-aab3-21c454e6a382', 'sca4e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca4e-1', 'PROCESSING'),

('sca5e-4cf4-477c-aab3-21c454e6a382', 'sca5e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca5e-1', 'PROCESSING'),

('sca51e-4cf4-477c-aab3-21c454e6a382', 'sca5e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '2',
'multiple-sca5e-2', null),

('sca6e-4cf4-477c-aab3-21c454e6a382', 'sca6e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'multiple-sca6e-1', 'PROCESSING'),

('sca61e-4cf4-477c-aab3-21c454e6a382', 'sca6e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '2',
'multiple-sca6e-2', null);


INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES
('sca1e-be55-4f0b-a989-8eb0b0071fb4', 'sca1e-4cf4-477c-aab3-21c454e6a380', 'Payload', 3),
('sca2e-be55-4f0b-a989-8eb0b0071fb4', 'sca2e-4cf4-477c-aab3-21c454e6a380', 'Payload', 5),
('sca3e-be55-4f0b-a989-8eb0b0071fb4', 'sca3e-4cf4-477c-aab3-21c454e6a380', 'Payload', 6),
('sca4e-be55-4f0b-a989-8eb0b0071fb4', 'sca4e-4cf4-477c-aab3-21c454e6a380', 'Payload', 3),
('sca5e-be55-4f0b-a989-8eb0b0071fb4', 'sca5e-4cf4-477c-aab3-21c454e6a380', 'Payload', 3),
('sca6e-be55-4f0b-a989-8eb0b0071fb4', 'sca6e-4cf4-477c-aab3-21c454e6a380', 'Payload', 3),
('scale-info-4f0b-a989-8eb0b0071fb4','values-4cf4-477c-aab3-21c454e6a380', 'Aspect1', 3),
('scale-info-4f0b-a989-8eb0b0071fb5','values-4cf4-477c-aab3-21c454e6a380', 'Aspect2', 3);

