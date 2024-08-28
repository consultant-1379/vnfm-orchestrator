INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, clean_up_resources)
VALUES ('instCrd-4cf4-477c-aab3-123456789101', 'vnf-instance-name', 'vnf-instance-description','d3def1ce-4cf4-477c-aab3-123456789101',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'd3def1ce-4cf4-477c-aab3-123456789101', 'NOT_INSTANTIATED','instCrd-1234-1234-1234-123456789010', 'test-messaging', true);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, expired_application_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params, cancel_pending, cancel_mode, error)
VALUES ('operCrd-4cf4-477c-aab3-123456789101', 'instCrd-4cf4-477c-aab3-123456789101', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE',
null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-1234-477c-aab3-123456789101', 'instCrd-4cf4-477c-aab3-123456789101',
'crd-package1-1.0.0.tgz', 'crd-package1', '1.0.0', 'CRD', 1, 'crd-inst-crd1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state)
VALUES ('instCrd-1235-477c-aab3-123456789101', 'instCrd-4cf4-477c-aab3-123456789101',
'crd-package2-1.0.0.tgz', 'crd-package2', '1.0.0', 'CRD', 2, 'crd-inst-crd2', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name)
VALUES ('instCrd-1236-477c-aab3-123456789101', 'instCrd-4cf4-477c-aab3-123456789101',
'sample-helm1.tgz', 'sample-helm1', '1.0.0', 'CNF', 3, 'crd-inst-CNF');