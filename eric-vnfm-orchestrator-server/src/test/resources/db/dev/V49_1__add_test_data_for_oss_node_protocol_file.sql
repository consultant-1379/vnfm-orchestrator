-- SET OSS NODE PROTOCOL FILE FOR HEAL COMPLETED TEST
UPDATE app_vnf_instance
SET oss_node_protocol_file = '<?xml version=\"1.0\" encoding=\"UTF-8\"?> <hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">'
WHERE vnf_id = 'h1def1ce-4cf4-477c-aab3-21c454e6666';

-- ADD HEAL OPERATION INSTANCE TEST DATA WITH FOR OSS NODE PROTOCOL FILE - 1
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, add_node_oss_topology)
VALUES ('123po8ys-4cf4-477c-aab3-21c454e6666', 'instantiate-with-otp-1', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-1',
        'd25qxcs7e-474f-4673-91ee-656fd8366666',
        '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
           "networkElementType":{"type":"string","required":"true","default":"nodetype"},
           "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
           "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
           "networkElementUsername":{"type":"string","required":"false","default":"admin"},
           "networkElementPassword":{"type":"string","required":"false","default":"password"}}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('asdc43ed-e1d6-430d-b867-ab6ee872ef6c', '123po8ys-4cf4-477c-aab3-21c454e6666',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'values-yml-addit-params');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('kji8u73t-474f-4673-91ee-656fd8366666', '123po8ys-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation-otp-1","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION INSTANCE TEST DATA WITH FOR OSS NODE PROTOCOL FILE - 2
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                              cluster_name, add_node_oss_topology)
VALUES ('ok98uyh6-4cf4-477c-aab3-21c454e6666', 'instantiate-with-otp-2', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-2',
        '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
           "networkElementType":{"type":"string","required":"true","default":"nodetype"},
           "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
           "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
           "networkElementUsername":{"type":"string","required":"false","default":"admin"},
           "networkElementPassword":{"type":"string","required":"false","default":"password"}}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('kg5y6t7-e1d6-430d-b867-ab6ee872ef6c', 'ok98uyh6-4cf4-477c-aab3-21c454e6666',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'values-yml-addit-params');

-- ADD HEAL OPERATION INSTANCE TEST DATA WITH FOR OSS NODE PROTOCOL FILE - 3
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, oss_node_protocol_file, namespace)
VALUES ('o9i99iuq-4cf4-477c-aab3-21c454e6666', 'instantiate-with-otp-3', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-3',
        'vf7vju8f-474f-4673-91ee-656fd8366666', 'oss node protocol xml content as string', 'heal-operation-otp-3');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h987ujo98-07e7-41d5-9324-bbb717b6666', 'o9i99iuq-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation-otp-3', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('omju77y6-474f-4673-91ee-656fd8366666', 'o9i99iuq-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation-otp-3","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('vf7vju8f-474f-4673-91ee-656fd8366666', 'o9i99iuq-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-04 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"latest", "additionalParams":{"ipVersion": "ipv6"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION INSTANCE TEST DATA WITH FOR OSS NODE PROTOCOL FILE - 4
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id)
VALUES ('jifuje87-4cf4-477c-aab3-21c454e6666', 'instantiate-with-otp-4', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-4',
        'oki98ec3-474f-4673-91ee-656fd8366666');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('jh7ytgbv-07e7-41d5-9324-bbb717b6666', 'jifuje87-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation-otp-4', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('oim8uj76-474f-4673-91ee-656fd8366666', 'jifuje87-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation-otp-4","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('oki98ec3-474f-4673-91ee-656fd8366666', 'jifuje87-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-04 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"Full Restore", "additionalParams":{"ipVersion": "ipv4"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');