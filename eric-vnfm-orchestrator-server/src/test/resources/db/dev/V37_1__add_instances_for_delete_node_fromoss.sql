INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, namespace, added_to_oss, cluster_name)
VALUES ('4241e63e-334b-4ee9-aa5f-155507dfcfe8', 'delete-node-success', 'vnfInstanceDescription',
'9242a55f-1315-4752-ac57-393d2cc8b7be', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cd0139e2-f0f8-4253-ba73-536a4510c3d7',
'delete-node-success', 'true', 'cluster-a'),
('2fe76a38-dca1-4f1a-97bc-11ffc353afbf', 'delete-node-failure', 'vnfInstanceDescription',
'27b2c795-29b7-490f-a4df-cfcee721c679', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', '9b15e586-3248-4b98-8372-2eba7f479f00',
'delete-node-failure', 'true', 'cluster-b'),
('5038f7b4-3514-4f8b-95ba-9c65a2393b08', 'delete-node-not-added', 'vnfInstanceDescription',
'59a24800-94fc-453e-8e5b-17b2733a5ec7', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'a650a83a-8c04-45fe-8543-9cbfa59a2cd1',
'delete-node-not-added-1', 'false', 'cluster-c');

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('11111111-334b-4ee9-aa5f-155507dfcfe8', '4241e63e-334b-4ee9-aa5f-155507dfcfe8',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '1',
'terminate-and-delete-node-successful-1', 'COMPLETED'),
('11111111-dca1-4f1a-97bc-11ffc353afbf', '2fe76a38-dca1-4f1a-97bc-11ffc353afbf',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '1',
'terminate-and-delete-node-failure-1', 'COMPLETED'),
('11111111-3514-4f8b-95ba-9c65a2393b08', '5038f7b4-3514-4f8b-95ba-9c65a2393b08',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.22.2.tgz', '1',
'terminate-and-node-not-added-1', 'COMPLETED');
