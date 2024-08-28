INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('l4def1ce-4cf4-477c-aab3-21c454e6a389', 'my-BAD-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-4',
 'm18fcbc8-474f-4673-91ee-761fd83991e6', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('m18fcbc8-474f-4673-91ee-761fd83991e6', 'l4def1ce-4cf4-477c-aab3-21c454e6a389', 'FAILED',
'2012-09-17T19:47:52.69', '2012-09-17T19:49:52.69', null, 'UPGRADE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{
type = { about:blank }title = { Internal Server Error } status = { 500 } detail = {testing}');
