INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('3a97df0b-e8d9-46cb-9cc4', 'f3def1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
'my-release-name', 'COMPLETED');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('f3def1ce-4cf4-477c-aab3', 'my-release', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'mycluster',
 'd08fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('3a97df0b-e8d9-46cb-9cc5', 'f3def1ce-4cf4-477c-aab3',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1', 'my-release-0');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('3a97df0b-e8d9-46cb-9cc6', 'f3def1ce-4cf4-477c-aab3',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '2', 'my-release-1');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('f3def1ce-4cf4-477c-aab90', 'my-release', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'mycluster123',
 'd08fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf5ce-4cf4-477c-aab3-21c454e6a379', 'clean-failed', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm5fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm5fcbc8-474f-4673-91ee-761fd83991e6', 'rf5ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"additionalParams":{"namespace":"jen-test",
"cleanUpResources":"true"}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r5hf1ce-4cf4-477c-aab3-21c454e6a379', 'rf5ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'clean-failed-1');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('r1f1ce-4cf4-477c-aab3-21c454e6a379', 'messaging-charts-fail', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm18fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm18fcbc8-474f-4673-91ee-761fd83991e6', 'r1f1ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r11f1ce-4cf4-477c-aab3-21c454e6a379', 'r1f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-charts-fail-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r21f1ce-4cf4-477c-aab3-21c454e6a379', 'r1f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-charts-Fail-2');
