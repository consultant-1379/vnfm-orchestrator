INSERT INTO app_cluster_config_file
(id,
 verification_namespace_uid,
 config_file_name,
 config_file_status,
 config_file_description,
 cluster_server,
 crd_namespace,
 config_file)
VALUES ('patchCluster',
        null,
        'patchCluster.config',
        'NOT_IN_USE',
        'patch cluster config success',
        'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/updateCluster',
        'eric-crd-ns',
        'apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: https://test.cluster.ericsson.se:443
  name: kubernetes
contexts:
- context:
    cluster: kubernetes
    user: kubernetes-admin
  name: kubernetes-admin@kubernetes
current-context: kubernetes-admin@kubernetes
kind: Config
preferences: {}
users:
- name: kubernetes-admin
  user:
    client-certificate-data: LS0tLS1CRUdJTiBDR
    client-key-data: LS0tLS1CRUdJTiBSU0EgUFJJ'),
       ('patchClusterInvalidUUID',
        'patchClusterInvalidUUID',
        'patchClusterInvalidUUID.config',
        'NOT_IN_USE',
        'patch cluster config success',
        'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/updateCluster',
        'eric-crd-ns',
        'apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: https://test.cluster.ericsson.se:443
  name: kubernetes
contexts:
- context:
    cluster: kubernetes
    user: kubernetes-admin
  name: kubernetes-admin@kubernetes
current-context: kubernetes-admin@kubernetes
kind: Config
preferences: {}
users:
- name: kubernetes-admin
  user:
    client-certificate-data: LS0tLS1CRUdJTiBDR
    client-key-data: LS0tLS1CRUdJTiBSU0EgUFJJ'),
       ('patchClusterMissingNamespaces',
        null,
        'patchClusterMissingNamespaces.config',
        'NOT_IN_USE',
        'patch cluster config success',
        'https://gevalia.rnd.gic.ericsson.se/k8s/clusters/updateCluster',
        'eric-crd-ns',
        'apiVersion: v1
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: https://test.cluster.ericsson.se:443
  name: kubernetes
contexts:
- context:
    cluster: kubernetes
    user: kubernetes-admin
  name: kubernetes-admin@kubernetes
current-context: kubernetes-admin@kubernetes
kind: Config
preferences: {}
users:
- name: kubernetes-admin
  user:
    client-certificate-data: LS0tLS1CRUdJTiBDR
    client-key-data: LS0tLS1CRUdJTiBSU0EgUFJJ');

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id,
                             vnf_provider, vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources, combined_values_file, bro_endpoint_url)
VALUES ('patchClusterMissingNamespaces', 'patchClusterMissingNamespaces', 'vnfInstanceDescription', 'patchClusterMissingNamespaces',
        'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'patchClusterMissingNamespaces', 'patchClusterMissingNamespaces', 'INSTANTIATED',
        'patchClusterMissingNamespaces', 'patchClusterMissingNamespaces', 'patchClusterMissingNamespaces', 'true',
        '{"bro_endpoint_url":"invalid-bro-url:8080"}', null);
