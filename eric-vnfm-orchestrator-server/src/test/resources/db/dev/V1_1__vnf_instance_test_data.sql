INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, helm_chart_url, namespace)
VALUES ('d3def1ce-4cf4-477c-aab3-21c454e6a379', 'my-release-name', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 'testupgrade');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('e3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-name', 'vnfInstanceDescription', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'c08fcbc8-474f-4673-91ee-761fd83991e6');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('f3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'mycluster',
 'd08fcbc8-474f-4673-91ee-761fd83991e6', 'test');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('g3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'f08fcbc8-474f-4673-91ee-761fd83991e6');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('g3def1ce-4cf4-477c-aab3-21c454e6a390', 'my-release-name-granting-success', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a379', 'NOT_INSTANTIATED', 'f08fcbc8-474f-4673-91ee-761fd83991e6');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('g3def1ce-4cf4-477c-aab3-21c454e6a391', 'my-release-name-granting-failed-forbidden', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a379', 'NOT_INSTANTIATED', 'f08fcbc8-474f-4673-91ee-761fd83991e6');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('g3def1ce-4cf4-477c-aab3-21c454e6a392', 'my-release-name-granting-failed-unavailable', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a379', 'NOT_INSTANTIATED', 'f08fcbc8-474f-4673-91ee-761fd83991e6');

  INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('ggdef1ce-4cf4-477c-aab3-21c454e6a389', 'my-release-name-1', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'f08fcbc8-474f-4673-91ee-761fd83991e6');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('h3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-BAD-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'g08fcbc8-474f-4673-91ee-761fd83991e6');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('i3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-BAD-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'h08fcbc8-474f-4673-91ee-761fd83641e6');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('k3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-BAD-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-9',
 'l08fcbc8-474f-4673-91ee-761fd83641e6', 'test');

 INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, namespace)
VALUES ('l3def1ce-4cf4-477c-aab3-21c454e6a389', 'my-BAD-release-name', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'clusterName-10',
 'n08fcbc8-474f-4673-91ee-761fd83641e6', 'test');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider, vnf_product_name, vnf_software_version,
vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, helm_chart_url, namespace)
VALUES ('d3def1ce-4cf4-477c-aab3-214jx84e6a379', 'my-BAD-release-name', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb1236a379', 'Ericsson',
 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'vmConnectionInfo-1',
 'm08fcbc8-474f-4673-91ee-761fd83991e6', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 'testUpgrade');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider, vnf_product_name, vnf_software_version,
vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, helm_chart_url)
VALUES ('p3def1ce-4cf4-477c-aab3-214jx84e6a379', 'my-BAD-release-name', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb1236a379', 'Ericsson',
 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'vmConnectionInfo-2',
 'p08fcbc8-474f-4673-91ee-761fd83991e6', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider, vnf_product_name, vnf_software_version,
vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, helm_chart_url)
VALUES ('t3def1ce-4cf4-477c-aab3-214jx84e6a379', 'my-BAD-release-name', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb1236a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '168759c2-0c0e-4fe7-93ee-5a299669e5c5', 'INSTANTIATED', 'vmConnectionInfo-3',
 'p08fcbc8-474f-4673-91ee-761fd83991e6', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, helm_chart_url)
VALUES ('r3def1ce-4cf4-477c-aab3-21c454e6a379', 'my-release-name', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, helm_chart_url)
VALUES ('s3def1ce-4cf4-477c-aab3-21c454e6a379', 'my-release-name', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('b08fcbc8-474f-4673-91ee-761fd83991e6', 'd3def1ce-4cf4-477c-aab3-21c454e6a379', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('c08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('d08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('f08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('g08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE',
'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('h08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE',
'{"flavourId":"string","instantiationLevelId":"string","extVirtualLinks":[{"id":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","extCps":[{"cpdId":"string","cpConfig":[{"cpInstanceId":"string","linkPortId":"string","cpProtocolData":[{"layerProtocol":"IP_OVER_ETHERNET","ipOverEthernet":{"macAddress":"string","ipAddresses":[{"type":"IPV4","fixedAddresses":["string"],"numDynamicAddresses":0,"addressRange":{"minAddress":"string","maxAddress":"string"},"subnetId":"string"}]}}]}]}],"extLinkPorts":[{"id":"string","resourceHandle":{"vimConnectionId":"string","resourceProviderId":"string","resourceId":"string","vimLevelResourceType":"string"}}]}],"extManagedVirtualLinks":[{"id":"string","virtualLinkDescId":"string","vimConnectionId":"string","resourceProviderId":"string","resourceId":"string"}],"vimConnectionInfo":[{"id":"string","vimId":"string","vimType":"string","interfaceInfo":{},"accessInfo":{},"extra":{}}],"localizationLanguage":"string","additionalParams":{}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('l08fcbc8-474f-4673-91ee-761fd83641e6', 'k3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ "type": "about:blank", "title": "Onboarding Overloaded", "status": 503, "detail": "Onboarding service not available", "instance": "" }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('n08fcbc8-474f-4673-91ee-761fd83641e6', 'l3def1ce-4cf4-477c-aab3-21c454e6a389', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ "type": "about:blank", "title": "Onboarding Overloaded", "status": 503, "detail": "Onboarding service not available", "instance": "" }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('m08fcbc8-474f-4673-91ee-761fd83991e6', 'e3def1ce-4cf4-477c-aab3-21c454e6a389', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ type = { about:blank }title = { Internal Server Error } status = { 500 } detail = { Hang tight while we grab the latest from your chart repositories... ...Skip local chart repository ...Successfully got an update from the "adp-am" chart repository ...Successfully got an update from the "stable" chart repository Update Complete. Happy Helming! Error: a release named my-release-name already exists. Run: helm ls --all test1; to check the status of the release Or run: helm del --purge test1; to deleteFile it}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('p08fcbc8-474f-4673-91ee-761fd83991e6', 'p3def1ce-4cf4-477c-aab3-214jx84e6a379', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ "type": "about:blank", "title":
"Onboarding Overloaded", "status": 503, "detail": "Onboarding service not available", "instance": "" }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('q08fcbc8-474f-4673-91ee-761fd83991e6', 'p3def1ce-4cf4-477c-aab3-214jx84e6a379', 'FAILED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', null, 'FALSE', 'FORCEFUL', '{ "type": "about:blank", "title":
"Onboarding Overloaded", "status": 503, "detail": "Onboarding service not available", "instance": "" }');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('r08fcbc8-474f-4673-91ee-761fd83991e6', 't3def1ce-4cf4-477c-aab3-214jx84e6a379', 'STARTING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('s08fcbc8-474f-4673-91ee-761fd83991e6', 's3def1ce-4cf4-477c-aab3-21c454e6a379', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);
