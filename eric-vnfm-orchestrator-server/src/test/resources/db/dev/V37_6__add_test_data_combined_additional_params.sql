INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace)
VALUES ('41def1ce-4cf4-477c-aab3-21c454e7777', 'upgrade-fail-1', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'clusterName-1266',
        '87ebcbc8-474f-4673-91ee-656fd8377777', 'upgrade-fail-1');


INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name)
VALUES ('0337b34c-07e7-41d5-9324-bbb717b7777', '41def1ce-4cf4-477c-aab3-21c454e7777',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'upgrade-fail-1');

UPDATE APP_VNF_INSTANCE
SET temp_instance = '{"vnfInstanceId":"41def1ce-4cf4-477c-aab3-21c454e7777","vnfInstanceName":"upgrade-fail-1","vnfInstanceDescription":"",' ||
                    '"vnfDescriptorId":"e3def1ce-4236-477c-abb3-21c454e6a645","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME",' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9308748274545350001",' ||
                    '"instantiationState":"INSTANTIATED","clusterName":"default","namespace":"upgrade-fail-1",' ||
                    '"helmCharts":[{"id":"0337b34c-07e7-41d5-9324-bbb717b7777","helmChartUrl":"sample-helm1.tgz","priority":1,"releaseName":"upgrade-fail-1","state":"COMPLETED"}],' ||
                    '"operationOccurrenceId":"521ecc62-420d-49bd-aa7d-705dd926e6e1","allOperation":[{"operationOccurrenceId":"924f7503-b8f9-45ee-8915-45312771f389",' ||
                    '"operationState":"COMPLETED","grantId":null,"lifecycleOperationType":"INSTANTIATE","automaticInvocation":false,' ||
                    '"operationParams":"{\"flavourId\":null,\"instantiationLevelId\":null,\"clusterName\":null,\"extVirtualLinks\":null,\"extManagedVirtualLinks\":null,' ||
                    '\"localizationLanguage\":null,\"additionalParams\":{\"namespace\":\"my-namespace\"}}","cancelPending":false,"error":null,"valuesFileParams":null,' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null},' ||
                    '{"operationOccurrenceId":"334a3ce0-ec57-44c5-a397-8ab34e3f367a","operationState":"STARTING","grantId":null,"lifecycleOperationType":' ||
                    '"CHANGE_PACKAGE_INFO","automaticInvocation":false,"operationParams":"{\"vnfdId\":\"3d02c5c9-7a9b-48da-8ceb-46fcc83f584c\",\"additionalParams\":{}}",' ||
                    '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)",' ||
                    '"vnfProductName":"SGSN-MME","cancelModeType":null}],"ossTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}",' ||
                    '"instantiateOssTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","addNodeOssTopology":null,"addedToOss":false,' ||
                    '"combinedValuesFile":null,"combinedAdditionalParams":"{\"eric-pc-up-data-plane.networkInstances[3].link.ipv6\": \"2a01:04c8:f401:0010::ff/64\",\"eric-pc-up-data-plane.networkInstances[3].link.ipv4\": \"10.251.0.101/28\",\"eric-pc-up-data-plane.networkInstances[2].interfaces[0]\": \"if-cp\",\"eric-pc-up-data-plane.applications[18].name\": \"ff_ee_freeratedsrv_sightcall\",\"eric-pc-up-data-plane.networkInstances[2].name\": \"SxN4\",\"eric-pc-up-data-plane.applications[10].name\": \"ff_ee_portal_url\",\"eric-cnom-server.service.loadBalancerIP\": \"10.128.28.83\",\"eric-pc-up-pfcp-endpoint.networkInstances[0].link.ipv4\": \"10.251.0.53/28\",\"eric-pc-up-data-plane.interfaces[2].ipv4\": \"193.36.80.4\",\"eric-pc-up-data-plane.interfaces[3].name\": \"if-access-s5u-n9\",\"eric-pc-up-data-plane.applications[23].name\": \"ff_generic_nhs_https\",\"eric-pc-up-data-plane.applications[8].name\": \"ff_ee_portal_ip_lab\",\"eric-pc-up-data-plane.applications[17].name\": \"ff_ee_freeratedsrv_qisda\",\"eric-fh-snmp-alarm-provider.service.annotations.metallb\\\\.universe\\\\.tf\\\\/allow-shared-ip\": \"fm-pm\",\"eric-pc-up-data-plane.applications[11].name\": \"ff_ee_portal_url_lab\",\"eric-pc-up-data-plane.networkInstances[3].name\": \"SGiN6\",\"eric-pc-up-data-plane.applications[1].name\": \"ff_ee_dns\",\"eric-pc-up-data-plane.networkInstances[2].link.ipv4\": \"10.251.0.53/28\",\"eric-pc-up-data-plane.applications[16].name\": \"ff_ee_freeratedsrv_ota\",\"eric-pc-up-data-plane.networkInstances[0].vlan\": \"1009\",\"eric-pc-up-data-plane.interfaces[2].type\": \"core\",\"eric-pc-up-data-plane.interfaces[4].name\": \"if-core-sgi-n6\",\"eric-pc-up-data-plane.networkInstances[0].gateway.ipv4\": \"10.251.0.65\",\"eric-pm-bulk-reporter.service.annotations.\\\"metallb\\\\.universe\\\\.tf\\\\/allow-shared-ip\\\"\": \"fm-pm\",\"eric-pc-up-data-plane.interfaces[1].name\": \"if-access-slu-n3\"}","policies":null,"resourceDetails":"",' ||
                    '"scaleInfoEntity":null,"manoControlledScaling":null}'
WHERE vnf_id = '41def1ce-4cf4-477c-aab3-21c454e7777';


INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('87ebcbc8-474f-4673-91ee-656fd8377777', '41def1ce-4cf4-477c-aab3-21c454e7777', 'STARTING',
        '2020-10-01 12:12:49.823', '2020-10-01 12:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500",
        "commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a646');
