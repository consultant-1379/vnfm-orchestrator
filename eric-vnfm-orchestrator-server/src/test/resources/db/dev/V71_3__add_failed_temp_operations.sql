INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, policies)
VALUES ('rb75f150-74cd-11ea-bc55-0242ac130003', 'rollback-single-success', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'rd3f70380-74ce-11ea-bc55-0242ac130003', 'multiple-charts', 'testrollback', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rd3f70380-74ce-11ea-bc55-0242ac130003', 'rb75f150-74cd-11ea-bc55-0242ac130003', 'FAILED_TEMP',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rdf70380-74ce-11ea-bc55-0242ac130003', 'fb75f150-74cd-11ea-bc55-0242ac130003', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);


