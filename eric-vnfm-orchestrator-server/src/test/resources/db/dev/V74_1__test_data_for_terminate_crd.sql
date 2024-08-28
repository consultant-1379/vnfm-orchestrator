--testSendRequestToWfsAndShouldSkipCrdCharts

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('rf1ce-4cf4-477c-aab3-21c454e6a375', 'delete-namespace', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'oo8fcbc8-474f-4673-91ee-761fd83991kk', 'delete-namespace', 'delete-namespace');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('oo8fcbc8-474f-4673-91ee-761fd83991kk', 'rf1ce-4cf4-477c-aab3-21c454e6a375', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', '{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":true,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a370', 'rf1ce-4cf4-477c-aab3-21c454e6a375',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '1',
'delete-namespace-0','PROCESSING', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a371', 'rf1ce-4cf4-477c-aab3-21c454e6a375',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'delete-namespace-1','PROCESSING', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a372', 'rf1ce-4cf4-477c-aab3-21c454e6a375',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '3',
'delete-namespace-2','PROCESSING', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc21ce-4cf4-477c-aab3-21c454e6a373', 'rf1ce-4cf4-477c-aab3-21c454e6a375',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '4',
'delete-namespace-3', 'PROCESSING', 'CNF');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('hc31ce-4cf4-477c-aab3-21c454e6a374', 'rf1ce-4cf4-477c-aab3-21c454e6a375',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '5',
'delete-namespace-4','PROCESSING');

--TerminateRequestHandlerTest
--LifecycleRequestHandlerTest
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('rf1ce00-4cf4-477c-aab3-21c454e6a375000', 'workflow-routine-namespace', 'vnfInstanceDescription',
        'd3def1ce-4cf4-477c-aab3-21cb04e6a3790000io', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81Y00)', 'cxp9025898_4r81eix', '9392468011745359999', 'INSTANTIATED',
        'oo8fcbc8-474f-4673-91ee-700099881ii', 'workflow-routine-namespace', 'workflow-routine-namespace');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES ('oo8fcbc8-474f-4673-91ee-700099881ii', 'rf1ce00-4cf4-477c-aab3-21c454e6a375000', 'PROCESSING',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'TERMINATE', 'FALSE', '{"terminationType":"FORCEFUL","additionalParams":{"cleanUpResources":false,"pvcTimeOut":"500","applicationTimeOut":"500","commandTimeOut":"500","deleteIdentifier":true}}', 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a370776', 'rf1ce00-4cf4-477c-aab3-21c454e6a375000',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '1',
        'workflow-routine-namespace-0','PROCESSING', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a370777', 'rf1ce00-4cf4-477c-aab3-21c454e6a375000',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
        'workflow-routine-namespace-1','PROCESSING', 'CNF');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type)
VALUES ('hc11ce-4cf4-477c-aab3-21c454e6a370778', 'rf1ce00-4cf4-477c-aab3-21c454e6a375000',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '3',
        'workflow-routine-namespace-3','PROCESSING', 'CNF');