INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, is_heal_supported)
VALUES ('bd7a1f49-6a3c', 'msg-inst-failed', 'vnfInstanceDescription',
'rrdef1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'default-heal-1',
 'rr8fcbc8-474f-4673-91ee-761fd83991e6', 'test-kms-1', true);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('713ec68f', 'bd7a1f49-6a3c', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);