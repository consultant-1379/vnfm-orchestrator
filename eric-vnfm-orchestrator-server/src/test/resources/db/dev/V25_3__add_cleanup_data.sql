INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('clean-4cf4-477c-aab3-21c454e6a380', 'cleanup-charts', 'vnfInstanceDescription',
'clean-4cf4-477c-aab3-21c454e6a380', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'cleanup1-474f-4673-91ee-761fd83991f9', 'cleanup-cluster', 'cleanup');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('cleanup1-474f-4673-91ee-761fd83991f9', 'clean-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('c1eans-4cf4-477c-aab3-21c454e6a381', 'clean-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-7.77.7.tgz', '1',
'cleanup-charts-1', 'FAILED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('00000-4cf4-477c-aab3-21c454e6a380', 'failing-charts', 'vnfInstanceDescription',
'00000-4cf4-477c-aab3-21c454e6a380', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '00000000-474f-4673-91ee-761fd83991f9', 'cleanup-cluster');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('00000000-474f-4673-91ee-761fd83991f9', '00000-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('000000-4cf4-477c-aab3-21c454e6a381', '00000-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-1.11.1.tgz', '1',
'failing-charts-1', 'FAILED');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('11111-4cf4-477c-aab3-21c454e6a380', 'cleaningup-charts', 'vnfInstanceDescription',
'clean-4cf4-477c-aab3-21c454e6a380', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 '11111111-474f-4673-91ee-761fd83991f9', 'cleanup-cluster', 'cleanup-1');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('11111111-474f-4673-91ee-761fd83991f9', '11111-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('111111-4cf4-477c-aab3-21c454e6a381', '11111-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '1',
'cleaningup-charts-1', 'FAILED');

--Cleanup Test Data 1 with invalid cluster error
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('45xc7s4q-4cf4-477c-aab3-21c454e6a380', 'failed-instantiate-invalid-cluster-1', 'vnfInstanceDescription',
'00000-4cf4-477c-aab3-21c454e6a380', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'zxcos98w-474f-4673-91ee-761fd83991f9', 'bad-cluster+');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('zxcos98w-474f-4673-91ee-761fd83991f9', '45xc7s4q-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL',
 '{"detail":"[{\"message\":\"cluster config not present, please add the config file using ''add cluster config rest api'' and then use this parameter\"}]","status":422}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('1xcos98w-474f-4673-91ee-761fd83991f9', '45xc7s4q-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL',
 '{"detail":"[{\"message\":\"cluster config not present, please add the config file using ''add cluster config rest api'' and then use this parameter\"}]","status":422}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('asd1234er-4cf4-477c-aab3-21c454e6a381', '45xc7s4q-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.1.tgz', '1',
'invalid-cluster-chart-cleanup', 'FAILED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('bsd1234er-4cf4-477c-aab3-21c454e6a381', '45xc7s4q-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '2',
'invalid-cluster-chart-cleanup', null);

--Cleanup Test Data 2 with invalid cluster error
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('81f11d1e-4cf4-477c-aab3-21c454e6a380', 'failed-instantiate-invalid-cluster-2', 'vnfInstanceDescription',
'00000-4cf4-477c-aab3-21c454e6a380', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'asdqwe12-474f-4673-91ee-761fd83991f9', 'bad-cluster+');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('asdqwe12-474f-4673-91ee-761fd83991f9', '81f11d1e-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL',
 '{"detail":"[{\"message\":\"cluster config not present, please add the config file using ''add cluster config rest api'' and then use this parameter\"}]","status":422}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('loi8sur5-474f-4673-91ee-761fd83991f9', '81f11d1e-4cf4-477c-aab3-21c454e6a380', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL',
 '{"detail":"[{\"message\":\"cluster config not present, please add the config file using ''add cluster config rest api'' and then use this parameter\"}]","status":422}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('ndk12ks8-4cf4-477c-aab3-21c454e6a381', '81f11d1e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.1.tgz', '1',
'invalid-cluster-chart-cleanup', 'FAILED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('oirue87c-4cf4-477c-aab3-21c454e6a381', '81f11d1e-4cf4-477c-aab3-21c454e6a380',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '2',
'invalid-cluster-chart-cleanup', 'FAILED');
