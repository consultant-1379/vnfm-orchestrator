-- BackupsTesting - testBroUrlNotProvided
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-backup001', 'snapshot-test-fail', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot001', 'snapshot', 'snapshot-test', 'true', null, 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot001', 'wf1ce-rd45-477c-vnf0-backup001', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot001', 'wf1ce-rd45-477c-vnf0-backup001',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-fail-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot002', 'wf1ce-rd45-477c-vnf0-backup001',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-fail-2', 'COMPLETED');

-- BackupsTesting - testInvalidBroUrlProvided
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, combined_values_file, bro_endpoint_url, is_heal_supported,
supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-snapshot002', 'snapshot-test-invalid', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot002', 'snapshot', 'snapshot-test', 'true', '{"bro_endpoint_url":"invalid-bro-url:8080"}', null, 'false',
 '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot002', 'wf1ce-rd45-477c-vnf0-snapshot002', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot003', 'wf1ce-rd45-477c-vnf0-snapshot002',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-invalid-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot004', 'wf1ce-rd45-477c-vnf0-snapshot002',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-invalid-2', 'COMPLETED');

-- BackupsTesting - testValidBroUrlProvided
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, combined_values_file, bro_endpoint_url, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-backup003', 'snapshot-test-valid', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot003', 'snapshot', 'snapshot-test', 'true', '{"bro_endpoint_url":"http://snapshot-bro.test"}',
 'http://snapshot-bro.test', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot003', 'wf1ce-rd45-477c-vnf0-backup003', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot005', 'wf1ce-rd45-477c-vnf0-snapshot002',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-valid-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot006', 'wf1ce-rd45-477c-vnf0-snapshot002',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-valid-2', 'COMPLETED');

-- BackupsTesting - operation Not Completed
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-snapshot004', 'snapshot-test-processing', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot004', 'snapshot', 'snapshot-test', 'true', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot004', 'wf1ce-rd45-477c-vnf0-snapshot004', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_name, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot007', 'wf1ce-rd45-477c-vnf0-snapshot004', 'spider-app-2.74.7',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-fail-1', 'PROCESSING');

INSERT INTO helm_chart(id, vnf_id, helm_chart_name, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot008', 'wf1ce-rd45-477c-vnf0-snapshot004', 'spider-app-2.74.8',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-fail-2', 'PROCESSING');

-- BackupsTesting - testBroUrlGettingSaved
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-snapshot005', 'snapshot-test-bro-save', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot005', 'snapshot', 'snapshot-test', 'true', null, 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot005', 'wf1ce-rd45-477c-vnf0-snapshot005', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot009', 'wf1ce-rd45-477c-vnf0-snapshot005',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-bro-save-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot010', 'wf1ce-rd45-477c-vnf0-snapshot005',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-bro-save-2', 'COMPLETED');

-- BackupsTesting - testBroUrlNotGettingSaved
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-snapshot006', 'snapshot-test-no-bro-saved', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-snapshot006', 'snapshot', 'snapshot-test', 'true', null, 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-snapshot006', 'wf1ce-rd45-477c-vnf0-snapshot006', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot011', 'wf1ce-rd45-477c-vnf0-snapshot006',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'snapshot-test-no-bro-saved-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-snapshot012', 'wf1ce-rd45-477c-vnf0-snapshot006',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'snapshot-test-no-bro-saved-2', 'COMPLETED');

-- BackupsTesting - Operation Completed state
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url, is_heal_supported, supported_operations)
VALUES ('wf1ce-rd45-477c-vnf0-backup007', 'backup-test-completed-state', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd45-4673-oper-backup007', 'backup', 'backup-test', 'true', 'http://snapshot-bro.test', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('wm8fcbc8-rd45-4673-oper-backup007', 'wf1ce-rd45-477c-vnf0-backup007', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE',
'{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-backup012', 'wf1ce-rd45-477c-vnf0-backup007',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'backup-test-completed-state-1', 'COMPLETED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('w1f1ce-rd41-helm-backup013', 'wf1ce-rd45-477c-vnf0-backup007',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'backup-test-completed-state-2', 'COMPLETED');

-------------------------------------------------------------------------------------
-- Add instance for encryption tests - 1
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, is_heal_supported, supported_operations)
VALUES ('crypto1-rd45-477c-vnf0-judc7', 'encryption-test-1', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'encr-rd45-4673-cxca-12wqq1', 'hall914.config', 'encryption-info-test-ns-1', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation)
VALUES ('crypto1lc-rd45-4673-oper-fgrte', 'crypto1-rd45-477c-vnf0-judc7', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE');

-- Add instance for encryption tests - 2
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, is_heal_supported, supported_operations)
VALUES ('crypto2-rd45-477c-vnf0-judc7', 'encryption-test-2', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
'encr-rd45-4673-cxca-12wqq1', 'hall914.config', 'encryption-info-test-ns-2', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation)
VALUES ('crypto2lc-rd45-4673-oper-fgrte', 'crypto2-rd45-477c-vnf0-judc7', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE');

-- Add instance for encryption tests - 3
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, combined_values_file, is_heal_supported, supported_operations)
VALUES ('crypto3-rd45-477c-vnf0-judc7', 'encryption-test-3', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
'encr-rd45-4673-cxca-12wqq1', 'hall914.config','encryption-info-test-ns-3', '{"test-key-3":"test-value-3"}', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation, combined_values_file)
VALUES ('crypto3lc-rd45-4673-oper-fgrte', 'crypto3-rd45-477c-vnf0-judc7', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', '{"test-key-3":"test-value-3"}');

-- BackupsTesting - operation Not Completed
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, is_heal_supported, supported_operations)
VALUES ('wfrd1-rd45-477c-vnf0-snapshot004', 'test-invalid-bro', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8frd1-rd45-4673-oper-snapshot004', 'snapshot', 'invalid-bro', 'true', 'false', '[{"operationName": "change_package","supported": true,"errorMessage": null}]');

 INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
 state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation, combined_values_file)
 VALUES ('wm8frd1-rd45-4673-oper-snapshot004', 'wfrd1-rd45-477c-vnf0-snapshot004', 'COMPLETED',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"test-key-3":"test-value-3"}');
