-- ADD HEAL OPERATION INSTANCE TEST DATA WITH RESTORE PARAMS TO HEAL VIA ADP BRO- 3
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, add_node_oss_topology)
VALUES ('o9i99iuq-4cf4-477c-aab3-21c454e1111', 'instantiate-with-heal', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'instantiate-with-otp-3',
        'vf7vju8f-474f-4673-91ee-656fd831111',
        'heal-operation','{"managedElementId":{"type":"string","required":"false","default":"elementId"},
 "networkElementType":{"type":"string","required":"true","default":"nodetype"},
 "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
 "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
 "networkElementUsername":{"type":"string","required":"false","default":"admin"},
 "networkElementPassword":{"type":"string","required":"false","default":"password"}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h987ujo98-07e7-41d5-9324-bbb717b1111', 'o9i99iuq-4cf4-477c-aab3-21c454e1111',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('omju77y6-474f-4673-91ee-656fd831111', 'o9i99iuq-4cf4-477c-aab3-21c454e1111', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('vf7vju8f-474f-4673-91ee-656fd831111', 'o9i99iuq-4cf4-477c-aab3-21c454e1111', 'PROCESSING',
        '2020-08-04 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"latest", "additionalParams":{"ipVersion": "ipv6", "restore.scope": "DEFAULT", "restore.backupName":
        "cnabackup", "day0.configuration.secretname":"restore-external-storage-secret", "day0.configuration.param1.key":"restore.externalStorageURL",
        "day0.configuration.param1.value" :"sftp://users@14BCP04/my-backup", "day0.configuration.param2.key":"restore.externalStorageCredentials",
        "day0.configuration.param2.value" : "password"}}', 'FALSE',
        'FORCEFUL', null,
        'e3def1ce-4236-477c-abb3-21c454e6a645');