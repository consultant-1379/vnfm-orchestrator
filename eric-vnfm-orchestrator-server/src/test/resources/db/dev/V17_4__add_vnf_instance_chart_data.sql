INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name)
VALUES ('ff1ce-4cf4-477c-aab3-21c454e6a379', 'multiple-charts', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6', 'my-cluster');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
VALUES ('gf1ce-4cf4-477c-aab3-21c454e6a379', 'ff1ce-4cf4-477c-aab3-21c454e6a379',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
VALUES ('hf1ce-4cf4-477c-aab3-21c454e6a379', 'h3def1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '1');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
VALUES ('3a97df0b-e8d9-46cb-9cc4-cc5ee475f9d7', 'h3def1ce-4cf4-477c-aab3-21c454e6a389',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz', '2');

 INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
 VALUES ('sf1ce-4cf4-477c-aab3-21c454e6a379', 'f3def1ce-4cf4-aab3-21c454e6a389',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1');
