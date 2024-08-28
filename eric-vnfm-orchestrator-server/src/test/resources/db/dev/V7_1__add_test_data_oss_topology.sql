INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology)
VALUES ('o9def1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-1', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName',
 'o08fcbc8-474f-4673-91ee-761fd83641e6', 'test', '{"snmpSecurityLevel":{"type":"string","required":"false"},"axeNodeInterfaceAIp":{"type":"string","required":"false"}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('o98fcbc8-474f-4673-91ee-761fd83641e6', 'o9def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology)
VALUES ('08def1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-2', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-13',
 'q08fcbc8-474f-4673-91ee-761fd83641e6', 'test', '{"snmpSecurityLevel":{"type":"string","required":"false"},"axeNodeInterfaceAIp":{"type":"string","required":"false"}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('o88fcbc8-474f-4673-91ee-761fd83641e6', '08def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);
