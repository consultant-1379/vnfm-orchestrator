INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology, add_node_oss_topology, added_to_oss)
VALUES ('41def1ce-4cf4-477c-aab3-21c454e43af', 'vnf-test-add-node', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'clusterName-16',
 'q08fcbc8-474f-4673-91ee-761fd83641e6', 'test-add-node',
 '{"managedElementId":{"type":"string","required":"true","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"}}',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'false'
 );

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('87ebcbc8-474f-4673-91ee-761fd83641e6', '41def1ce-4cf4-477c-aab3-21c454e43af', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'INSTANTIATE', 'FALSE', '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE',
'FORCEFUL',
null);
