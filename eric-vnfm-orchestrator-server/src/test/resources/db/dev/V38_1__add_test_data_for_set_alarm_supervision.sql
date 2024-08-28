INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id,
add_node_oss_topology, added_to_oss)
VALUES ('kxnam34q-7065-49b1-831c-d687130c6123', 'enable-alarm-supervision-initial', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1',
 'aoekg9t6-b16d-45fb-acb2-f2c631cb19ed',
 '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"true","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}',
 true);

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
 vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id,
 add_node_oss_topology, added_to_oss, alarm_supervision_status)
 VALUES ('cl9u5rf7-7065-49b1-831c-d687130c6123', 'enable-alarm-supervision-with-on', 'vnfInstanceDescription',
 'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
  '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1',
  'j7h6tr5a-b16d-45fb-acb2-f2c631cb19ed',
  '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
  "networkElementType":{"type":"string","required":"true","default":"nodetype"},
  "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
  "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
  "networkElementUsername":{"type":"string","required":"false","default":"admin"},
  "networkElementPassword":{"type":"string","required":"false","default":"password"}}',
  true, 'on');



INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('aoekg9t6-b16d-45fb-acb2-f2c631cb19ed', 'kxnam34q-7065-49b1-831c-d687130c6123', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('j7h6tr5a-b16d-45fb-acb2-f2c631cb19ed', 'cl9u5rf7-7065-49b1-831c-d687130c6123', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);