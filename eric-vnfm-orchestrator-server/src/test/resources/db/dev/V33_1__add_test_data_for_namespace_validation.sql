INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, namespace, cluster_name)
VALUES ('e3def1ce-4cf4-477c-ahb3-61c454e6a344', 'namespace-validation', 'vnfInstanceDescription', '719243a2-6518-42fe-957b-9e1e4536746b', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
'namespace-validate', 'cluster-validate');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, namespace, cluster_name)
VALUES ('t3def1ce-4cf4-477c-ahb3-61c454e6a344', 'namespace-validation-1', 'vnfInstanceDescription', '719243a2-6518-42fe-957b-9e1e4536746b', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
'namespace-validate1', 'cluster-validate1.config');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, namespace, cluster_name)
VALUES ('a4d879fb-b4d6-4d2d-bfcd-a51af941c326', 'namespace-validation-2', 'vnfInstanceDescription', '719243a2-6518-42fe-957b-9e1e4536746b', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'm08fcbc8-474f-4673-91ee-761fd83991e6',
'', 'cluster-validate');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('f3b6b68d-a577-47e0-b33e-fd082e6acdea', 'a4d879fb-b4d6-4d2d-bfcd-a51af941c326', 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 1, 'namespace-validation');
