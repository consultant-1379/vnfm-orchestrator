INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state)
VALUES ('e3def1ce-4cf4-477c-aab3-21c454eabcd', 'release-name-instantiate-in-progress', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454e6abcd',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
VALUES ('if1ce-4cf4-477c-aab3-21c454e6a379', 'e3def1ce-4cf4-477c-aab3-21c454eabcd',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1');

INSERT INTO operations_in_progress(id, vnf_id, lifecycle_operation_type)
VALUES('f3def1ce-4cf4-477c-aab3-21c454eabcd', 'e3def1ce-4cf4-477c-aab3-21c454eabcd', 'INSTANTIATE');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, namespace, cluster_name)
VALUES ('e3def1ce-4cf4-477c-aab3-21c454efghi', 'release-name-instantiate-in-progress', 'vnfInstanceDescription',
'e3def1ce-4cf4-477c-aab3-21c454efghi',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'test', 'my-cluster');

INSERT INTO operations_in_progress(id, vnf_id, lifecycle_operation_type)
VALUES('844abd5d-ff25-48b5-b2aa-21ec3cc3aff4', 'e3def1ce-4cf4-477c-aab3-21c454efghi', 'TERMINATE');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority)
VALUES ('xy1ce-4cf4-477c-aab3-21c454e6a379', 'e3def1ce-4cf4-477c-aab3-21c454efghi',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1');
