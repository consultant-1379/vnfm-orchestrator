-- healCompletedTestWithCrds
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('vnfi100e-4cf4-477c-aab3-21c454e6666', 'heal-operation', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation',
        'oo104bc1-474f-4673-91ee-656fd8366666', 'healoperation', '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300","commandTimeOut":"300"}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc10134c-07e7-41d5-9324-bbb717b6101', 'vnfi100e-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'crd-operation-1', 'COMPLETED', 'CRD');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('hc10234c-07e7-41d5-9324-bbb717b6102', 'vnfi100e-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'heal-operation-1', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo101bc1-474f-4673-91ee-656fd8366666', 'vnfi100e-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo102bc1-474f-4673-91ee-656fd8366666', 'vnfi100e-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.824', '2020-08-03 12:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"400"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 3
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo104bc1-474f-4673-91ee-656fd8366666', 'vnfi100e-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"latest"}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');




-- SET OSS NODE PROTOCOL FILE FOR HEAL COMPLETED TEST
UPDATE app_vnf_instance
SET oss_node_protocol_file = '<?xml version=\"1.0\" encoding=\"UTF-8\"?> <hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">'
WHERE vnf_id = 'vnfi100e-4cf4-477c-aab3-21c454e6666';


-- healCompletedTestWithCNFRestoreAndSkipCrd test
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, oss_node_protocol_file, sensitive_info, namespace, add_node_oss_topology, instantiation_level)
VALUES ('vnfi200q-4cf4-477c-aab3-21c454e7777', 'instantiate-with-otp-3', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-3',
        'oo202u8f-474f-4673-91ee-656fd837777', 'oss node protocol xml content as string', '{"kms-unseal-key":"test-value"}',
        'heal-operation-otp-200','{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"true","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}', 'dummy_instantiation_level');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc201jo98-07e7-41d5-9324-bbb717b7777', 'vnfi200q-4cf4-477c-aab3-21c454e7777',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation-otp-200', 'COMPLETED', 'CRD');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('hc202jo98-07e7-41d5-9324-bbb717b7777', 'vnfi200q-4cf4-477c-aab3-21c454e7777',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'heal-operation-otp-200', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('oo2017y6-474f-4673-91ee-656fd837777', 'vnfi200q-4cf4-477c-aab3-21c454e7777', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation-otp-200","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('oo202u8f-474f-4673-91ee-656fd837777', 'vnfi200q-4cf4-477c-aab3-21c454e7777', 'PROCESSING',
        '2020-08-04 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"latest", "additionalParams":{"ipVersion": "ipv6", "restore.backupFileReference": "sftp://users@14BCP04/my-backup", "restore.password": "password"}}', 'FALSE',
        'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');