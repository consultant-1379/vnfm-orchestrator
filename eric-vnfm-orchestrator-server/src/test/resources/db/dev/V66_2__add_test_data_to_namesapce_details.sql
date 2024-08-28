UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1' WHERE  config_file_name='cleanup-cluster.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla1' WHERE  config_file_name='my-cluster.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla2' WHERE  config_file_name='messaging-integration.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla3' WHERE  config_file_name='cluster-validate.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla4' WHERE  config_file_name='mycluster-123.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla5' WHERE  config_file_name='testinstantiate.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla6' WHERE  config_file_name='instantiate-with-otp-2.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla7' WHERE  config_file_name='downsize-namespace-multi.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla8' WHERE  config_file_name='config.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla9' WHERE  config_file_name='test-downsize.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla9' WHERE  config_file_name='cluster-g.config';
UPDATE "public"."app_cluster_config_file" SET "cluster_server"='https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla11' WHERE  config_file_name='instantiate-1266.config';


UPDATE "public"."app_vnf_instance" SET "namespace"='heal-namespace' WHERE  vnf_id='9095c4ee-7a5e-41ad-8898-f679b33eae0f';
UPDATE "public"."app_vnf_instance" SET "namespace"='heal-namespace1' WHERE  vnf_id='dbdc2622-9026-4d44-8795-b0f923c710ff';
UPDATE "public"."app_vnf_instance" SET "namespace"='heal-namespace2' WHERE  vnf_id='69f5b897-daea-4f93-bc11-2b14c728ed7c';
UPDATE "public"."app_vnf_instance" SET "namespace"='heal-namespace3' WHERE  vnf_id='ok98uyh6-4cf4-477c-aab3-21c454e6666';
UPDATE "public"."app_vnf_instance" SET "namespace"='heal-namespace4' WHERE  vnf_id='3de67785-66e4-48c2-a97a-1500dfd66cb1';


INSERT INTO vnfinstance_namespace_details(id, vnf_id, namespace, cluster_server, namespace_deletion_in_progess)
 VALUES
('1', 'rf2ce-4cf4-477c-aab3-21c454e6a374', 'not-found', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla6', true),
('10', 'wf1ce-4cf4-477c-aab3-21c454e6a380', 'delete-pvc-node', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla14', true),
('17', 'rf7ce-4cf4-477c-aab3-21c454e6a379', 'delete-pvc-node', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla16', true),
('18', 'rf7c14-4cf4-477c-aab3-21c454e6a379', 'namespace-deletion', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla6', true),
('23', '64failed-4cf4-477c-aab3-21c454e6666', 'failed-downgrade', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla10', true),
('25', '3f4becee-27b5-11ed-a261-0242ac120002', 'failed-heal', 'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/bla20', true);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id)
VALUES ('r8def1ce-4cf4-477c-ahb3-61c454e6a344', 'deletion_in_progress', 'vnfInstanceDescription', '999243a2-6518-42fe-957b-9e1e4536746b', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'n23fcbc8-474f-4673-91ee-761fd83991e6');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state)
VALUES ('r9def1ce-4cf4-477c-ahb3-61c454e6a344', 'deletion_in_progress', 'vnfInstanceDescription', '999243a2-6518-42fe-957b-9e1e4536746b', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('h9b6b68d-a577-47e0-b33e-fd082e6acdea', 'r9def1ce-4cf4-477c-ahb3-61c454e6a344',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 1, 'multiple-cnf-same-ns');
