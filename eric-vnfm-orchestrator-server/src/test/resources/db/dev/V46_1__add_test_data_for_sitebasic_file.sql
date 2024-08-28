-- VALID SITEBASIC FILE
UPDATE app_vnf_instance
SET sitebasic_file = '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<Nodes>\r\n   <Node>\r\n      <nodeFdn>VPP00001</nodeFdn>\r\n         <certType>OAM</certType>\r\n      <enrollmentMode>CMPv2_INITIAL</enrollmentMode>\r\n   </Node>\r\n</Nodes>\r\n\r\n'
WHERE vnf_id = 'kxnam34q-7065-49b1-831c-d687130c6123';

-- ADD VALID FORMAT SITEBASIC FILE
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name)
VALUES ('alksjvcn1-7065-49b1-831c-d687130c6123', 'save-site-basic-file', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001',
'NOT_INSTANTIATED', 'cluster-1');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('lkfir938-d093-8273-8472-0192857dmfjd', 'alksjvcn1-7065-49b1-831c-d687130c6123',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'instantiate-valid-sitebasic-1', 'PROCESSING');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('iwld94373-d093-8273-8472-0192857dmfjd', 'alksjvcn1-7065-49b1-831c-d687130c6123',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'instantiate-valid-sitebasic-2', null);

-- ADD INVALID FORMAT SITEBASIC FILE
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name)
VALUES ('oldi87ed-7065-49b1-831c-d687130c6123', 'invalid-site-basic-file', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001',
'NOT_INSTANTIATED', 'cluster-2');