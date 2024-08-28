INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology, instantiate_oss_topology, added_to_oss,
combined_additional_params, combined_values_file)
VALUES ('70def1ce-4cf4-477c-aab3-21c454e6a389', 'vnf-combined-1', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-14',
 '708fcbc8-474f-4673-91ee-761fd83641e6', 'test',
 '{"managedElementId":{"type":"string","required":"true","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"}}',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'false', '{}',
 '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}'
 );

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('708fcbc8-474f-4673-91ee-761fd83641e6', '30def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology, instantiate_oss_topology, added_to_oss,
combined_additional_params, combined_values_file)
VALUES ('31def1ce-4cf4-477c-aab3-21c454e6a389', 'vnf-combined-2', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-15',
 '918fcbc8-474f-4673-91ee-761fd83641e6', 'test',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId-2"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"}}',
 '{"networkElementType":{"type":"string","required":"false","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'true', '{}',
 '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}'
 );

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('918fcbc8-474f-4673-91ee-761fd83641e6', '31def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology, add_node_oss_topology, added_to_oss,
combined_additional_params, combined_values_file)
VALUES ('50def1ce-4cf4-477c-aab3-21c454e6a389', 'vnf-combined-3', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-15',
 '318fcbc8-474f-4673-91ee-761fd83641e6', 'test',
 '{"managedElementId":{"type":"string","required":"true","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"}}',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'true','{}',
 '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}'
 );

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('318fcbc8-474f-4673-91ee-761fd83641e6', '50def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace, oss_topology, add_node_oss_topology, added_to_oss,
combined_additional_params, combined_values_file)
VALUES ('61def1ce-4cf4-477c-aab3-21c454e6a389', 'vnf-combined-4', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-16',
 '818fcbc8-474f-4673-91ee-761fd83641e6', 'test',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId-2"},
 "networkElementType":{"type":"string","required":"false","default":"nodetype"}}',
 '{"networkElementType":{"type":"string","required":"false","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'true', '{}',
 '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}'
 );

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('818fcbc8-474f-4673-91ee-761fd83641e6', '61def1ce-4cf4-477c-aab3-21c454e6a389', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null,
'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);
