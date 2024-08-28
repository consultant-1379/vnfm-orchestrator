INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state)
VALUES ('9583b4aa-1474-4aa2-b26e-5e18dbcae682', 'release-name-no-scale-mano', 'vnfInstanceDescription',
'9583b4aa-1474-4aa2-b26e-5e18dbcae682',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state)
VALUES ('47ef96d3-ec98-4a6b-a8d5-4fb9a01c854f', 'release-name-no-scale-mano-2', 'vnfInstanceDescription',
'47ef96d3-ec98-4a6b-a8d5-4fb9a01c854f',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('8ca2e32d-66ce-4757-bda9-0b427cace35c', 'release-name-with-scale-mano', 'vnfInstanceDescription',
'8ca2e32d-66ce-4757-bda9-0b427cace35c', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'hall914.config',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":null},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('8ca2e32d-66ce-4757-bda9-0b427cace31', 'release-name-with-scale-mano-4', 'vnfInstanceDescription',
'8ca2e32d-66ce-4757-bda9-0b427cace31', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'hall914.config',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":null},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');