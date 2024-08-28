INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('wf1ce-4cf4-477c-aab3-21c454e6a374', 'messaging-integration', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-474f-4673-91ee-761fd83991k4', 'messaging-integration', 'messaging-integration');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('wm8fcbc8-474f-4673-91ee-761fd83991k4', 'wf1ce-4cf4-477c-aab3-21c454e6a374', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('w1f1ce-4cf4-477c-aab3-21c454e6a387', 'wf1ce-4cf4-477c-aab3-21c454e6a374',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-integration-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('w2f1ce-4cf4-477c-aab3-21c454e6a388', 'wf1ce-4cf4-477c-aab3-21c454e6a374',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-integration-2');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('rf2ce-4cf4-477c-aab3-21c454e6a374', 'delete-namespace2', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-474f-4673-91ee-761fd83991k4', 'delete-namespace2', 'not-found');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm2fcbc8-474f-4673-91ee-761fd83991k4', 'rf2ce-4cf4-477c-aab3-21c454e6a374', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', '{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r2f1ce-4cf4-477c-aab3-21c454e6a387', 'rf2ce-4cf4-477c-aab3-21c454e6a374',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'delete-namespace2-1', 'PROCESSING');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r21f1ce-4cf4-477c-aab3-21c454e6a388', 'rf2ce-4cf4-477c-aab3-21c454e6a374',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'delete-namespace2-2','PROCESSING');

