INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace)
VALUES ('cm3plh4q-ileo-m43t-j3q9-7k84rre970vt', 'my-instance-name', 'vnfInstanceDescription',
        'cm3plh4q-ileo-m43t-j3q9-7k84rre970vt', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'namespace-locked-cluster',
        'n3udq6td-d5rl-96ld-ubde-goc4i4e7x6mk', 'test');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error)
VALUES ('n3udq6td-d5rl-96ld-ubde-goc4i4e7x6mk', 'cm3plh4q-ileo-m43t-j3q9-7k84rre970vt', 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('k0m8codm-hacr-cxms-mbg6', 'cm3plh4q-ileo-m43t-j3q9-7k84rre970vt',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1',
        'my-instance-name', 'COMPLETED');

UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla11' WHERE config_file_name='namespace-locked-cluster.config';

INSERT INTO vnfinstance_namespace_details(id, vnf_id, namespace, cluster_server, namespace_deletion_in_progess)
VALUES ('r9ffcy-4cf4-477c-aab3-0y403491hgme', 'cm3plh4q-ileo-m43t-j3q9-7k84rre970vt', 'test', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla11', false);