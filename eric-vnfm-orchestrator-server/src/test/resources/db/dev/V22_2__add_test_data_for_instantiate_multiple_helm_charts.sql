INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf6ce-4cf4-477c-aab3-21c454e6a379', 'msg-failed', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm6fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm6fcbc8-474f-4673-91ee-761fd83991e6', 'rf6ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r6hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf6ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'msg-failed-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r6hf1ce-4cf4-477c-aab3-21c454e6a390', 'g3def1ce-4cf4-477c-aab3-21c454e6a390',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'test-granting');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r6hf1ce-4cf4-477c-aab3-21c454e6a391', 'g3def1ce-4cf4-477c-aab3-21c454e6a391',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'test-granting');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r6hf1ce-4cf4-477c-aab3-21c454e6a392', 'g3def1ce-4cf4-477c-aab3-21c454e6a392',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'test-granting');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf7ce-4cf4-477c-aab3-21c454e6a379', 'msg-failed-cleanup', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm7fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm7fcbc8-474f-4673-91ee-761fd83991e6', 'rf7ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"additionalParams":{"namespace":"jen-test",
"cleanUpResources":"true", "skipVerification":"true"}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r7hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf7ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'msg-failed-cleanup-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r71hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf7ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '2',
'msg-failed-cleanup-2');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf8ce-4cf4-477c-aab3-21c454e6a379', 'msg-terminate-cleanup', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm9fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm9fcbc8-474f-4673-91ee-761fd83991e6', 'rf8ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"additionalParams":{"namespace":"jen-test",
"cleanUpResources":"true"}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r8hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf8ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'msg-terminate-cleanup-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r81hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf8ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '2',
'msg-terminate-cleanup-2');
