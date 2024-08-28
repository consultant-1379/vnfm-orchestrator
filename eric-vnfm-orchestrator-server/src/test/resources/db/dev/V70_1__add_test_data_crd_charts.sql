INSERT INTO app_cluster_config_file
(id,
 config_file_name,
 config_file_status,
 config_file_description,
 cluster_server,
 crd_namespace)
VALUES
    ('cf2ab9eb-770e-478a-8945-4d71158a8102',
     'crdcluster.config',
     'NOT_IN_USE',
     'test file',
     'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla10',
     'multi-cluster-crd-ns');

-- CRDTesting - testCrdNamespaceForExternalCluster
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
                             vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, bro_endpoint_url)
VALUES ('afbc35b1-e510-47bd-89ae-b1f20f7f3b09', 'crd-single-cnf', 'vnfInstanceDescription', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
        'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED',
        'fc5f503f-3eb6-43b8-b019-9ad353e73ba7', 'crdcluster', 'cnf-with-crd-ns', 'false', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state,
                                     state_entered_time, start_time, grant_id, lifecycle_operation_type, automatic_invocation,
                                     operation_params,
                                     cancel_pending, cancel_mode, error)
VALUES ('fc5f503f-3eb6-43b8-b019-9ad353e73ba7', 'afbc35b1-e510-47bd-89ae-b1f20f7f3b09', 'PROCESSING',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE','{"instantiationLevelId":"123","additionalParams":{"applicationTimeOut":"500","commandTimeOut":"500"}}',
        'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('ac963638-f8ec-49b6-9c49-fdad79283ecd', 'afbc35b1-e510-47bd-89ae-b1f20f7f3b09',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-5.3.1.tgz', '1',
        'crd-single-cnf-1', 'COMPLETED', 'eric-certm', '5.3.1', 'CRD');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_name, helm_chart_version, helm_chart_type)
VALUES ('49479ec7-7416-4d7c-b3a2-b2d81ee6ca2c', 'afbc35b1-e510-47bd-89ae-b1f20f7f3b09',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz', '2',
        'crd-single-cnf-2', 'COMPLETED', 'spider-app', '2.74.8', 'CNF');

INSERT INTO vnfinstance_namespace_details(id, vnf_id, namespace, cluster_server, namespace_deletion_in_progess)
VALUES ('b814f58d-d55d-442c-b383-21d63625d3a1', 'afbc35b1-e510-47bd-89ae-b1f20f7f3b09',
        'cnf-with-crd-ns', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla10', false);