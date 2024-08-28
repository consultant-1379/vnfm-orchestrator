INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, clean_up_resources, temp_instance)
VALUES ('rollback1Crd-4cf4-477c-aab3-123456789101', 'rollback-with-crd', 'Test rollback VNF with CRDs','d3def1ce-4cf4-477c-aab3-123456789101',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'd3def1ce-4cf4-477c-aab3-123456789101', 'INSTANTIATED', 'rollback1Crd-1234-1234-1234-123456789010', 'test-messaging', true, '{ "vnfInstanceId": "rollback1Crd-4cf4-477c-aab3-123456789101", "vnfInstanceName": "rollback-with-crd", "vnfDescriptorId": "d3def1ce-4cf4-477c-aab3-123456789101", "vnfPackageId": "d3def1ce-4cf4-477c-aab3-123456789101", "instantiationState": "INSTANTIATED", "clusterName": "test-messaging", "operationOccurrenceId": "rollback1Crd-1234-1234-1234-123456789010", "helmCharts": [ { "helmChartUrl": "crd-package1-1.0.0.tgz", "releaseName": "crd-inst-crd1", "helmChartType": "CRD", "state": "COMPLETED", "priority": 1 }, { "helmChartUrl": "crd-package2-1.0.0.tgz", "releaseName": "crd-inst-crd2", "helmChartType": "CRD", "state": "COMPLETED", "priority": 2 }, { "helmChartUrl": "sample-helm1.tgz", "releaseName": "crd-inst-cnf", "helmChartType": "CNF", "state": "ROLLING_BACK", "priority": 3 } ] }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, expired_application_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params, cancel_pending, cancel_mode, error)
VALUES ('rollback1Crd-1234-1234-1234-123456789010', 'rollback1Crd-4cf4-477c-aab3-123456789101', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE',
null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-1237-477c-aab3-123456789101', 'rollback1Crd-4cf4-477c-aab3-123456789101',
'crd-package1-1.0.0.tgz', 'crd-package1', '1.0.0', 'CRD', 1, 'crd-inst-crd1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-1238-477c-aab3-123456789101', 'rollback1Crd-4cf4-477c-aab3-123456789101',
'crd-package2-1.0.0.tgz', 'crd-package2', '1.0.0', 'CRD', 2, 'crd-inst-crd2', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-1239-477c-aab3-123456789101', 'rollback1Crd-4cf4-477c-aab3-123456789101',
'sample-helm1.tgz', 'sample-helm1', '1.0.0', 'CNF', 3, 'crd-inst-cnf', 'ROLLING_BACK');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, clean_up_resources, temp_instance)
VALUES ('rollback2Crd-4cf4-477c-aab3-123456789101', 'rollback-with-crd', 'Test rollback VNF with CRDs','d3def1ce-4cf4-477c-aab3-123456789101',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'd3def1ce-4cf4-477c-aab3-123456789101', 'INSTANTIATED', 'rollback2Crd-1234-1234-1234-123456789010', 'test-messaging', true, '{ "vnfInstanceId": "rollback2Crd-4cf4-477c-aab3-123456789101", "vnfInstanceName": "rollback-with-crd", "vnfDescriptorId": "d3def1ce-4cf4-477c-aab3-123456789101", "vnfPackageId": "d3def1ce-4cf4-477c-aab3-123456789101", "instantiationState": "INSTANTIATED", "clusterName": "test-messaging", "operationOccurrenceId": "rollback2Crd-1234-1234-1234-123456789010", "helmCharts": [ { "helmChartUrl": "crd-package1-1.0.0.tgz", "releaseName": "crd-inst-crd1", "helmChartType": "CRD", "state": "COMPLETED", "priority": 1 }, { "helmChartUrl": "crd-package2-1.0.0.tgz", "releaseName": "crd-inst-crd2", "helmChartType": "CRD", "state": "COMPLETED", "priority": 2 }, { "helmChartUrl": "sample-helm1.tgz", "releaseName": "crd-inst-cnf", "helmChartType": "CNF", "state": "ROLLING_BACK", "priority": 3 } ] }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, expired_application_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params, cancel_pending, cancel_mode, error)
VALUES ('rollback2Crd-1234-1234-1234-123456789010', 'rollback2Crd-4cf4-477c-aab3-123456789101', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE',
null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-12310-477c-aab3-123456789101', 'rollback2Crd-4cf4-477c-aab3-123456789101',
'crd-package1-1.0.0.tgz', 'crd-package1', '1.0.0', 'CRD', 1, 'crd-inst-crd1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-12311-477c-aab3-123456789101', 'rollback2Crd-4cf4-477c-aab3-123456789101',
'crd-package2-1.0.0.tgz', 'crd-package2', '1.0.0', 'CRD', 2, 'crd-inst-crd2', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-12312-477c-aab3-123456789101', 'rollback2Crd-4cf4-477c-aab3-123456789101',
'sample-helm1.tgz', 'sample-helm1', '1.0.0', 'CNF', 3, 'crd-inst-cnf', 'ROLLING_BACK');