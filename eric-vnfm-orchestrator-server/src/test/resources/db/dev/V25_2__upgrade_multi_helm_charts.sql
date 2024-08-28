INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, policies)
VALUES ('13143285-ced7-4e67-a55b-8236c88de4ea', 'msg-multi-chart-chg', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 '521ecc62-420d-49bd-aa7d-705dd926e6e1', 'test', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('fead314a-8e45-4481-acc8-181a8ef901dd', 'msg-multi-chart-chg-1st-fail', 'vnfInstanceDescription',
'0f314b00-1ee3-4ea1-8e5d-5450e4f68ff5', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
 '8f8f558f-ace7-40ec-bd66-8945756dde6b', 'test'),
 ('55cfe48a-9c2f-4b26-92c5-8dc004f2a5e3', 'msg-multi-chart-chg-2nd-fail', 'vnfInstanceDescription',
 '0f314b00-1ee3-4ea1-8e5d-5450e4f68ff5', 'Ericsson', 'SGSN-MME',
  '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'default66',
  'cc03f37e-e692-4bc9-9b52-33f2e17c6dce', 'test');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES
('3ea468f9-2fec-4209-8948-a6be1d98930c', '13143285-ced7-4e67-a55b-8236c88de4ea', 'Payload', 3),
('2c901417-d245-4f2d-8150-0e1df2836c8d', '13143285-ced7-4e67-a55b-8236c88de4ea', 'Payload_2', 4);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('521ecc62-420d-49bd-aa7d-705dd926e6e1', '13143285-ced7-4e67-a55b-8236c88de4ea', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null),
('0aa939a4-6acf-45b0-8e2f-ddf59257909e', '13143285-ced7-4e67-a55b-8236c88de4ea', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null),
('8f8f558f-ace7-40ec-bd66-8945756dde6b', 'fead314a-8e45-4481-acc8-181a8ef901dd', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null),
('cc03f37e-e692-4bc9-9b52-33f2e17c6dce', '55cfe48a-9c2f-4b26-92c5-8dc004f2a5e3', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('30018630-3fb6-46d2-8055-ab0e7f413c47', '13143285-ced7-4e67-a55b-8236c88de4ea',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-multi-chart-chg-1'),
('dc400a71-c03d-4d4c-8bf6-02536db8a2ff', '13143285-ced7-4e67-a55b-8236c88de4ea',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'msg-multi-chart-chg-2'),
('4390849e-5b62-4e70-a23c-3de2451306f9', 'fead314a-8e45-4481-acc8-181a8ef901dd',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-multi-chart-chg-1st-fails-1'),
('f2aa00b2-cbde-46a3-ab8a-985e698ff6fd', 'fead314a-8e45-4481-acc8-181a8ef901dd',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'msg-multi-chart-chg-1st-fails-2'),
('b73371e5-55bf-471e-8f56-6cc15b6b2010', '55cfe48a-9c2f-4b26-92c5-8dc004f2a5e3',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'msg-multi-chart-chg-2nd-fails-1'),
('ac62db44-277d-448d-8d55-838b2a1dcff3', '55cfe48a-9c2f-4b26-92c5-8dc004f2a5e3',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'msg-multi-chart-chg-2nd-fails-2');;
