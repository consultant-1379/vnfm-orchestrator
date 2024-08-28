INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, policies)
VALUES ('fb75f150-74cd-11ea-bc55-0242ac130003', 'rollback-single-success', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '23f70380-74ce-11ea-bc55-0242ac130003', 'multiple-charts', 'testrollback', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('23f70380-74ce-11ea-bc55-0242ac130003', 'fb75f150-74cd-11ea-bc55-0242ac130003', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('49ebdffc-74ce-11ea-bc55-0242ac130003', 'fb75f150-74cd-11ea-bc55-0242ac130003',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-single-success-1', 'COMPLETED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('3f082e17-bf79-45e7-8582-2b5abf20b9b1', 'rollback-multiple-success', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '0dd3755c-25ca-4672-90af-c0d89550dc97', 'multipleChartsFailed', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('0dd3755c-25ca-4672-90af-c0d89550dc97', '3f082e17-bf79-45e7-8582-2b5abf20b9b1', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

insert into app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
values ('54d3755c-25ca-4672-90af-c0d89550dc54', '3f082e17-bf79-45e7-8582-2b5abf20b9b1', 'COMPLETED',
current_timestamp, current_timestamp, null, 'INSTANTIATE', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('94031754-0cd1-4b3d-a358-a4d80439bc49', '3f082e17-bf79-45e7-8582-2b5abf20b9b1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-multiple-success-1', 'PROCESSING');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('24d014c0-945f-4472-926b-a4c6e9efc2f9', '3f082e17-bf79-45e7-8582-2b5abf20b9b1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'rollback-multiple-success-2', 'COMPLETED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('a20edc8d-807a-4f16-a675-b6599defb085', 'rollback-single-failed', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '64bdf09d-b9e4-4c68-8685-76c90d3d39b9', 'multiple-charts', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('64bdf09d-b9e4-4c68-8685-76c90d3d39b9', 'a20edc8d-807a-4f16-a675-b6599defb085', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('bccc9c78-da5c-4f57-92f2-3e3602667870', 'a20edc8d-807a-4f16-a675-b6599defb085',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-single-failed-1', 'COMPLETED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('926cd1b1-a60e-4bf0-bbaf-6d4069d23dfd', 'rollback-multiple-failure', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '695d82df-6e60-452c-8127-2bf486e3f607', 'multipleChartsFailed', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('695d82df-6e60-452c-8127-2bf486e3f607', '926cd1b1-a60e-4bf0-bbaf-6d4069d23dfd', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

insert into app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
values ('805d82df-6e60-452c-8127-2bf486e3f680', '926cd1b1-a60e-4bf0-bbaf-6d4069d23dfd', 'COMPLETED',
current_timestamp, current_timestamp, null, 'INSTANTIATE', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('23c45423-03b9-4055-94d6-8f86fe80dae2', '926cd1b1-a60e-4bf0-bbaf-6d4069d23dfd',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-multiple-failure-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('0b66fd89-9f66-45b4-a79f-f5a7f1f6e555', '926cd1b1-a60e-4bf0-bbaf-6d4069d23dfd',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'rollback-multiple-failure-2', 'PROCESSING');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('838d556b-89c9-470b-875c-f7c715ef4f17', 'rollback-mixed-failure', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'bb26cacc-bcae-411b-98bf-75175e66a555', 'multipleChartsFailed', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('bb26cacc-bcae-411b-98bf-75175e66a555', '838d556b-89c9-470b-875c-f7c715ef4f17', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('fdbdbffa-2bc5-4fae-a457-129ba079c9ec', '838d556b-89c9-470b-875c-f7c715ef4f17',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-mixed-failure-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('644bea0c-8ae1-4519-86c5-49b61e2fd80c', '838d556b-89c9-470b-875c-f7c715ef4f17',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'rollback-mixed-failure-2', 'PROCESSING');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, policies)
VALUES ('adff854f-7dc2-4e04-8287-cbed94bd202b', 'upgrade-failed-single', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '51e01da5-7ab6-4784-923a-61081a71818f', 'multipleChartsFailed', 'testrollback',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('51e01da5-7ab6-4784-923a-61081a71818f', 'adff854f-7dc2-4e04-8287-cbed94bd202b', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('63d5e807-57d8-4880-946f-d8443f285820', 'adff854f-7dc2-4e04-8287-cbed94bd202b',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'upgrade-failed-single-1', 'COMPLETED');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('ca39cc3f-de9c-4c32-9c1b-e731e5ef53aa', 'upgrade-failed-rollback', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'c2921594-4b51-4969-b84a-ed5743633d99', 'multipleChartsFailed', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('c2921594-4b51-4969-b84a-ed5743633d99', 'ca39cc3f-de9c-4c32-9c1b-e731e5ef53aa', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', '{"vnfdId": "test"}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('66ce6e36-7193-4493-88fe-d1e4cbcf717d', 'ca39cc3f-de9c-4c32-9c1b-e731e5ef53aa',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'upgrade-failed-rollback-1', 'COMPLETED');
