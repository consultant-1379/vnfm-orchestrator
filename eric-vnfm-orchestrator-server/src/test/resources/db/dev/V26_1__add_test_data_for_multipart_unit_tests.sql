INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('a1b1f1ce-4cf4-477c-aab3-21c454e6a389', 'successful-multipart-instantiate', 'Multipart: successful instantiate',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e7', 'my-cluster');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('a2b2f1ce-4cf4-477c-aab3-21c454e6a389', 'failed-multipart-instantiate', 'Multipart: failed instantiate',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e8', 'my-cluster');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('a3b3f1ce-4cf4-477c-aab3-21c454e6a389', 'successful-multipart-upgrade', 'Multipart: successful change package info',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e9', 'my-cluster');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('a4b4f1ce-4cf4-477c-aab3-21c454e6a389', 'failed-multipart-upgrade', 'Multipart: failed change package info',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e9', 'my-cluster');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, policies, resource_details)
VALUES ('a5b5f1ce-4cf4-477c-aab3-21c454e6a389', 'successful-scale', 'Successful scale request, json not multipart',
'def1ce-4cf4-477c-aab3-2b04e6a382',
'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e1', 'my-cluster',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload", "description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}',
 '{"PL__scaled_vm": 9, "TL_scaled_vm": 9}');

 INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES
('scale-a5b5-4f0b-a989-8eb0b0071fb4', 'a5b5f1ce-4cf4-477c-aab3-21c454e6a389', 'Payload', 3);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('a1b1df0b-e8d9-46cb-9cc5', 'a1b1f1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'successful-multipart-instantiate');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('a2b2df0b-e8d9-46cb-9cc5', 'a2b2f1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'failed-multipart-instantiate');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('a3b3df0b-e8d9-46cb-9cc5', 'a3b3f1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'successful-multipart-upgrade');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('a4b4df0b-e8d9-46cb-9cc5', 'a4b4f1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'failed-multipart-upgrade');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('a5b5df0b-e8d9-46cb-9cc5', 'a5b5f1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'successful-scale');