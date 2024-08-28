-- CRDTesting - successfulResponseForValidInstantiationReqWithCrdFromRoutedWFS
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url)
VALUES ('9845971235-as49-4c24-8796-6e5afa2535g1', 'crd-multi-cnf', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 '9845971235-as49-4c24-8796-6e5afa2535g1', 'hall914.config', 'cnf-with-crd-ns', 'false', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('2254229d-19d1-48ae-9e77-97ca6174c1ha', '9845971235-as49-4c24-8796-6e5afa2535g1', 'STARTING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE','{"instantiationLevelId":"123","additionalParams":{"applicationTimeOut":"500","commandTimeOut":"500"}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('er544ty6-r3e2-47c7-925e-9821a05b1ba2', '9845971235-as49-4c24-8796-6e5afa2535g1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-5.3.1.tgz', '1',
'crd-single-cnf-1', 'COMPLETED', 'eric-certm', '1.3.1', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('we125wq3-f4c4-47c7-925e-9821a05b1ba4', '9845971235-as49-4c24-8796-6e5afa2535g1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'crd-single-cnf-2', 'COMPLETED', 'spider-app', '2.7.0', 'CNF');


-- CRDTesting - failureResponseForInvalidInstantiationReqWithCrdFromRoutedWFS
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url)
VALUES ('89883055-as49-4c24-8796-6e5afa2535g1', 'crd-multi-cnf', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
 '89883055-as49-4c24-8796-6e5afa2535g1', 'hall914.config', 'cnf-with-crd-ns', 'false', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
operation_params,
cancel_pending, cancel_mode, error)
VALUES ('7798569d-99d1-48ae-9e77-97ca6174c1ha', '89883055-as49-4c24-8796-6e5afa2535g1', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE','{"instantiationLevelId":"123","additionalParams":{"applicationTimeOut":"500","commandTimeOut":"500"}}',
'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('ab344ad5-f4c4-47c7-925e-9821a05b1ba2', '89883055-as49-4c24-8796-6e5afa2535g1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-5.3.1.tgz', '1',
'crd-single-cnf-1', 'COMPLETED', 'eric-certm', null, 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('cd344ad5-f4c4-47c7-925e-9821a05b1ba4', '89883055-as49-4c24-8796-6e5afa2535g1',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
'crd-single-cnf-2', 'COMPLETED', 'spider-app', '2.7.0', 'CNF');