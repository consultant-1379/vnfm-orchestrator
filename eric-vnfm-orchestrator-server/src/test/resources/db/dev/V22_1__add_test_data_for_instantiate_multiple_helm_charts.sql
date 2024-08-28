INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf1ce-4cf4-477c-aab3-21c454e6a379', 'messaging-charts', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm8fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm8fcbc8-474f-4673-91ee-761fd83991e6', 'rf1ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r1f1ce-4cf4-477c-aab3-21c454e6a379', 'rf1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-charts-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r2f1ce-4cf4-477c-aab3-21c454e6a379', 'rf1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-charts-2');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('r2f1ce-4cf4-477c-aab3-21c454e6a379', 'messaging-fail', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm28fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm28fcbc8-474f-4673-91ee-761fd83991e6', 'r2f1ce-4cf4-477c-aab3-21c454e6a379', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r12f1ce-4cf4-477c-aab3-21c454e6a379', 'r2f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-fail-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r22f1ce-4cf4-477c-aab3-21c454e6a379', 'r2f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-fail-2');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('r3f1ce-4cf4-477c-aab3-21c454e6a379', 'charts', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm28fcbc8-474f-4673-91ee-761fd83991e6', 'test-messaging');


INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r13f1ce-4cf4-477c-aab3-21c454e6a379', 'r3f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'charts-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r23f1ce-4cf4-477c-aab3-21c454e6a379', 'r3f1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'charts-2');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf1ce-4cf4-477c-aab3-21c454e6a379200', 'messaging-charts-22', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm8fcbc8-474f-4673-91ee-761fd83991e6200', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('rm8fcbc8-474f-4673-91ee-761fd83991e6200', 'rf1ce-4cf4-477c-aab3-21c454e6a379200', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"flavourId":"string","instantiationLevelId":"string","extVirtualLinks":[{"id":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","extCps":[{"cpdId":"string","cpConfig":{"cpInstanceId":{"linkPortId":"string","cpProtocolData":[{"layerProtocol":"IP_OVER_ETHERNET","ipOverEthernet":{"macAddress":"string","ipAddresses":[{"type":"IPV4","fixedAddresses":["string"],"numDynamicAddresses":0,"addressRange":{"minAddress":"string","maxAddress":"string"},"subnetId":"string"}]}}]}}}],"extLinkPorts":[{"id":"string","resourceHandle":{"vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","vimLevelResourceType":"string"}}]}],"extManagedVirtualLinks":[{"id":"string","virtualLinkDescId":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string"}],"localizationLanguage":"string","additionalParams":{"ingress":"test.test","param1":"value1"}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r1f1ce-4cf4-477c-aab3-21c454e6a379200', 'rf1ce-4cf4-477c-aab3-21c454e6a379200',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-charts-22-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r2f1ce-4cf4-477c-aab3-21c454e6a379200', 'rf1ce-4cf4-477c-aab3-21c454e6a379200',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-charts-22-2');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('rf1ce-4cf4-477c-aab3-21c454e6a379300', 'messaging-charts-23', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 'rm8fcbc8-474f-4673-91ee-761fd83991e6300', 'test-messaging');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, values_file_params)
VALUES ('rm8fcbc8-474f-4673-91ee-761fd83991e6300', 'rf1ce-4cf4-477c-aab3-21c454e6a379300', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"flavourId":"string","instantiationLevelId":"string","extVirtualLinks":[{"id":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","extCps":[{"cpdId":"string","cpConfig":{"cpInstanceId":{"linkPortId":"string","cpProtocolData":[{"layerProtocol":"IP_OVER_ETHERNET","ipOverEthernet":{"macAddress":"string","ipAddresses":[{"type":"IPV4","fixedAddresses":["string"],"numDynamicAddresses":0,"addressRange":{"minAddress":"string","maxAddress":"string"},"subnetId":"string"}]}}]}}}],"extLinkPorts":[{"id":"string","resourceHandle":{"vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","vimLevelResourceType":"string"}}]}],"extManagedVirtualLinks":[{"id":"string","virtualLinkDescId":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string"}],"localizationLanguage":"string","additionalParams":{"ingress":"test.test","param1":"value1"}}', 'FALSE', 'FORCEFUL', null, '{"param2":"value2", "param3.parm": "value3", "param3.param4": "nalue3"}');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r1f1ce-4cf4-477c-aab3-21c454e6a379300', 'rf1ce-4cf4-477c-aab3-21c454e6a379300',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'messaging-charts-23-1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('r2f1ce-4cf4-477c-aab3-21c454e6a379300', 'rf1ce-4cf4-477c-aab3-21c454e6a379300',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'messaging-charts-23-2');
