-- SINGLE CHARTS
-- vnf instance for single chart
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace)
VALUES ('41def1ce-4cf4-477c-aab3-21c454e6666', 'vnf-test-1', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'NOT_INSTANTIATED', 'clusterName-1266',
        '87ebcbc8-474f-4673-91ee-656fd8366666', 'test');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('0337b34c-07e7-41d5-9324-bbb717b6666', '41def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'change-package-info-release-name', 'COMPLETED');

--Add temp_instance for single_chart test
UPDATE APP_VNF_INSTANCE
SET temp_instance = '{"vnfInstanceId":"41def1ce-4cf4-477c-aab3-21c454e6666","vnfInstanceName":"msg-multi-chart-chg","vnfInstanceDescription":"",' ||
                    '"vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME",' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9308748274545350001",' ||
                    '"instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test",' ||
                    '"helmCharts":[{"id":"0337b34c-07e7-5479-9324-bbb717b6666","helmChartUrl":"sample-helm1.tgz","priority":1,"releaseName":"end-to-end-1","state":"COMPLETED"}],' ||
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
                    '"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"",' ||
                    '"scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '41def1ce-4cf4-477c-aab3-21c454e6666';

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('87ebcbc8-474f-4673-91ee-656fd8366666', '41def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500",
        "commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, source_vnfd_id, target_vnfd_id)
VALUES ('87ebcbc8-474f-4673-91ee-667fd8366666', '41def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-04 13:12:49.823', '2020-08-04 13:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500","commandTimeOut":"500"}}',
        'FALSE', 'FORCEFUL', null, 'b3def1ce-4cf4-477c-aab3-21cb04e6a266', 'b3def1ce-4cf4-477c-aab3-it8104e6a380');

--helm chart history for helm chart 1
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                                life_cycle_operation_id)
VALUES ('8cf641a9-3ea2-474c-be4c-0d9434b31866',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
        'change-package-info-release-name', 'COMPLETED',
        '2', 1, '87ebcbc8-474f-4673-91ee-667fd8366666');

--change package operation details from 1 to 2 lifecycle operation
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('87ebcbc8-474f-4673-91ee-656fd8366666', 'DOWNGRADE', '87ebcbc8-474f-4673-91ee-667fd8366666');

-- MULTIPLE CHARTS: new vnf instance - helm chart - in the list of update
--vnf instance for multiple chart (2)
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace)
VALUES ('83dee1ce-8ab5-477c-aab3-21c454e6666', 'vnf-test-565', 'vnfInstanceDescription',
        '83dee1ce-8ab5-477c-aab3-215434e6666', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '6408748274545356324', 'NOT_INSTANTIATED', 'clusterName-1268',
        '87ebcbc8-474f-4673-91ee-645369384739', 'test2');

--helm chart 1 (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('0557b34c-3434-41d5-9324-bbb717b66665', '83dee1ce-8ab5-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'change-package-info-release-name2', 'COMPLETED');

--helm chart 2 (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('8a574803-07e7-41d5-9324-bbb717b5566', '83dee1ce-8ab5-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.4.tgz', '1',
        'change-package-info-release-name3', 'COMPLETED');

-- UPDATE APP_VNF_INSTANCE SET temp_instance
UPDATE APP_VNF_INSTANCE
SET temp_instance = '{"vnfInstanceId":"83dee1ce-8ab5-477c-aab3-21c454e6666","vnfInstanceName":"msg-multi-chart-chg","vnfInstanceDescription":"",' ||
                    '"vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME",' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"68aea56b-14e8-4850-b2a6-48f785042127",' ||
                    '"instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test","helmCharts":[{"id":"d516a17d-445c-4e21-817e-377d5f28b588",' ||
                    '"helmChartUrl":"sample-helm1.tgz","priority":1,"releaseName":"end-to-end-1","state":"COMPLETED"}],' ||
                    '"operationOccurrenceId":"521ecc62-420d-49bd-aa7d-705dd926e6e1",' ||
                    '"allOperation":[{"operationOccurrenceId":"924f7503-b8f9-45ee-8915-45312771f389","operationState":"COMPLETED","grantId":null,' ||
                    '"lifecycleOperationType":"INSTANTIATE","automaticInvocation":false,"operationParams":"{\"flavourId\":null,' ||
                    '\"instantiationLevelId\":null,\"clusterName\":null,\"extVirtualLinks\":null,\"extManagedVirtualLinks\":null,\"localizationLanguage\":null,' ||
                    '\"additionalParams\":{\"namespace\":\"my-namespace\"}}","cancelPending":false,"error":null,"valuesFileParams":null,' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null},' ||
                    '{"operationOccurrenceId":"334a3ce0-ec57-44c5-a397-8ab34e3f367a","operationState":"STARTING","grantId":null,' ||
                    '"lifecycleOperationType":"CHANGE_PACKAGE_INFO","automaticInvocation":false,"operationParams":"{\"vnfdId\":\"3d02c5c9-7a9b-48da-8ceb-46fcc83f584c\",' ||
                    '\"additionalParams\":{}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)",' ||
                    '"vnfProductName":"SGSN-MME","cancelModeType":null}],"ossTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}",' ||
                    '"instantiateOssTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","addNodeOssTopology":null,"addedToOss":false,' ||
                    '"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"",' ||
                    '"scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '83dee1ce-8ab5-477c-aab3-21c454e6666';

--lifecycle operation for multiple charts - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('87ebcbc8-474f-4673-91ee-645369384739', '83dee1ce-8ab5-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500",
        "commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation for multiple charts - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, source_vnfd_id, target_vnfd_id)
VALUES ('87ebcbc8-yy88-4673-91ee-667fd8347007', '83dee1ce-8ab5-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-04 13:12:49.823', '2020-08-04 13:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500","commandTimeOut":"500"}}',
        'FALSE', 'FORCEFUL', null, 'b3def1ce-4cf4-477c-aab3-21cb04e6a266', 'b3def1ce-4cf4-477c-aab3-it8104e6a380');

--helm chart history for helm chart 2 (2)
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                                life_cycle_operation_id)
VALUES ('87ebcbc8-5370-4673-91ee-645369384739',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
        'change-package-info-release-name3', 'COMPLETED',
        '2', 1, '87ebcbc8-yy88-4673-91ee-667fd8347007');

--helm chart history for helm chart 1 (2)
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count,
                                life_cycle_operation_id)
VALUES ('0557b34c-3434-41d5-9324-bbb717b66665',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 0,
        'change-package-info-release-name2', 'COMPLETED',
        '2', 1, '87ebcbc8-yy88-4673-91ee-667fd8347007');

--change package operation details for 5 and 6 lifecycle operation
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('87ebcbc8-474f-4673-91ee-645369384739', 'DOWNGRADE', '87ebcbc8-yy88-4673-91ee-667fd8347007');

--Failed operation
--Populate DB with data for downgrade during failed operation
--For DOWNGRADE here is used an UNFINISHED OPERATION with id '78finish-474f-4673-91ee-656fd8366666'

--Add VNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace)
VALUES ('64failed-4cf4-477c-aab3-21c454e6666', 'vnf-test-failed', 'vnfInstanceDescription',
        'e3failed-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'NOT_INSTANTIATED', 'clusterName-1266',
        '87ebcbc8-474f-4673-91ee-656fd8366666', 'test');

--ADD HELM_CHART
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('5777b34c-07e7-41d5-9324-bbb717b6666', '64failed-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'change-package-info-release-name', 'COMPLETED');

--ADD TEMP_INSTANCE
UPDATE APP_VNF_INSTANCE
SET temp_instance = '{"vnfInstanceId":"64failed-4cf4-477c-aab3-21c454e6666","vnfInstanceName":"msg-multi-chart-chg","vnfInstanceDescription":"",' ||
                    '"vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME",' ||
                    '"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9308748274545350001",' ||
                    '"instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test",' ||
                    '"helmCharts":[{"id":"32b7b34c-07e7-5479-9324-bbb717b6666","helmChartUrl":"sample-helm1.tgz","priority":1,' ||
                    '"releaseName":"end-to-end-1","state":"COMPLETED"}],' ||
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
                    '"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"",' ||
                    '"scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '64failed-4cf4-477c-aab3-21c454e6666';

--ADD LIFECYCLE OPERATION TO WHICH DOWNGRADE WOULD HAPPENED
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, source_vnfd_id, target_vnfd_id)
VALUES ('64ebcsec-474f-4673-91ee-667fd8366666', '64failed-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-04 13:12:49.823', '2020-08-04 13:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500","commandTimeOut":"500"}}',
        'FALSE', 'FORCEFUL', null, 'b3def1ce-4cf4-477c-aab3-21cb04e6a266', 'b3def1ce-4cf4-477c-aab3-it8104e6a380');

----ADD LIFECYCLE OPERATION WITH UNFINISHED OPERATION
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('78finish-474f-4673-91ee-656fd8366666', '64failed-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"instantiationLevelId":"123","additionalParams":{"addNodeToOSS": true,"applicationTimeOut":"500",
        "commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--ADD CHANGE PACKAGE DOWNGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('78finish-474f-4673-91ee-656fd8366666', 'DOWNGRADE', '64ebcsec-474f-4673-91ee-667fd8366666');

-- ADD INSTANTITAE_OPERATION_REQUEST TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('r3def1ce-4cf4-477c-aab3-21c454e6666', 'instantiate_request', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'NOT_INSTANTIATED', 'instantiate-1266',
        'r9ebcbc8-474f-4673-91ee-656fd8366666', 'testchangepackage', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r737b34c-07e7-41d5-9324-bbb717b6666', 'r3def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'instantiate_request-1', 'COMPLETED');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r837b34c-07e7-41d5-9324-bbb717b6666', 'r3def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'instantiate_request-2', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('r9ebcbc8-474f-4673-91ee-656fd8366666', 'r3def1ce-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'TERMINATE', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


-- ADD PERFORM INSTANTIATE REQUEST TEST ERROR
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('r5def1ce-4cf4-477c-aab3-21c454e6666', 'perform_instantiate', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'NOT_INSTANTIATED', 'instantiate-1266',
        null, 'testchangepackage_1', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r757b34c-07e7-41d5-9324-bbb717b6666', 'r5def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'perform_instantiate-1', 'PROCESSING');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('r857b34c-07e7-41d5-9324-bbb717b6666', 'r5def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'perform_instantiate-2', null);

-- ADD HEAL OPERATION TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('h1def1ce-4cf4-477c-aab3-21c454e6666', 'heal-operation', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation',
        'h13bcbc1-474f-4673-91ee-656fd8366666', 'healoperation', '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300","commandTimeOut":"300"}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h117b34c-07e7-41d5-9324-bbb717b6666', 'h1def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation-1', 'COMPLETED');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h127b34c-07e7-41d5-9324-bbb717b6666', 'h1def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'heal-operation-2', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h1fbcbc1-474f-4673-91ee-656fd8366666', 'h1def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h12bcbc1-474f-4673-91ee-656fd8366666', 'h1def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.824', '2020-08-03 12:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"400"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 3
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h14bcbc1-474f-4673-91ee-656fd8366666', 'h1def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.826', '2020-08-03 12:13:49.823', null, 'SCALE', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300","commandTimeOut":"300"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


--lifecycle operation - 4
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h13bcbc1-474f-4673-91ee-656fd8366666', 'h1def1ce-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.825', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"cause":"latest"}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION OPERATION IN PROGRESS TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('h2def1ce-4cf4-477c-aab3-21c454e6666', 'heal_operation', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation-1',
        'h24bcbc1-474f-4673-91ee-656fd8366666', 'healoperation', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h24bcbc1-474f-4673-91ee-656fd8366666', 'h2def1ce-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.826', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION UPDATE INSTANCE TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('h3def1ce-4cf4-477c-aab3-21c454e6666', 'heal_operation3', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation3',
        'h13bcbc1-474f-4673-91ee-656fd8366666', 'healoperation', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h317b34c-07e7-41d5-9324-bbb717b6666', 'h3def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal_operation3-1', 'COMPLETED');

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('h327b34c-07e7-41d5-9324-bbb717b6666', 'h3def1ce-4cf4-477c-aab3-21c454e6666',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'heal_operation3-2', 'COMPLETED');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h3fbcbc1-474f-4673-91ee-656fd8366666', 'h3def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION UPDATE INSTANCE TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('h4def1ce-4cf4-477c-aab3-21c454e6666', 'heal_operation4', 'vnfInstanceDescription',
        'e3def1ce-4236-477c-abb3-21c454e6a645', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation4',
        'h13bcbc1-474f-4673-91ee-656fd8366666', 'healoperation', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h4fbcbc1-474f-4673-91ee-656fd8366666', 'h4def1ce-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', null, 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

-- ADD HEAL OPERATION INSTANTIATED OPERATION IN NOT FOUND TEST DATA
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, combined_additional_params, combined_values_file)
VALUES ('h5def1ce-4cf4-477c-aab3-21c454e6666', 'heal_operation5', 'vnfInstanceDescription',
        'h14bcbc1-474f-4673-91ee-656fd8366666', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation-5',
        'h51bcbc1-474f-4673-91ee-656fd8366666', 'healoperation', '{}','{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}');

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h51bcbc1-474f-4673-91ee-656fd8366666', 'h5def1ce-4cf4-477c-aab3-21c454e6666', 'PROCESSING',
        '2020-08-03 12:12:49.826', '2020-08-03 12:13:49.823', null, 'HEAL', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode,
                                     error, target_vnfd_id)
VALUES ('h52bcbc1-474f-4673-91ee-656fd8366666', 'h5def1ce-4cf4-477c-aab3-21c454e6666', 'COMPLETED',
        '2020-08-03 12:12:49.826', '2020-08-03 12:13:49.823', null, 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, added_to_oss)
VALUES ('q9r77165-7065-49b1-831c-d687130c6123', 'heal-completes-not-added-to-oss', 'vnfInstanceDescription',
        'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1', '23ty78oi-b16d-45fb-acb2-f2c631cb19ed', false);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id,
                             add_node_oss_topology, added_to_oss)
VALUES ('lk817165-7065-49b1-831c-d687130c6123', 'heal-completes-failed-enable-alarm', 'vnfInstanceDescription',
        'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1', '78rxdqoi-b16d-45fb-acb2-f2c631cb19ed',
        '{"managedElementId":{"type":"string","required":"false","default":"elementId"},
        "networkElementType":{"type":"string","required":"true","default":"nodetype"},
        "networkElementVersion":{"type":"string","required":"false","default":"nodeVersion"},
        "nodeIpAddress":{"type":"string","required":"false","default":"my-ip"},
        "networkElementUsername":{"type":"string","required":"false","default":"admin"},
        "networkElementPassword":{"type":"string","required":"false","default":"password"}}',
        true);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, added_to_oss)
VALUES ('vb577165-7065-49b1-831c-d687130c6123', 'heal-completes-enable-alarm', 'vnfInstanceDescription',
        'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1', '45ty78oi-b16d-45fb-acb2-f2c631cb19ed', true);

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name, current_life_cycle_operation_id, added_to_oss)
VALUES ('po45mf0t-7065-49b1-831c-d687130c6123', 'heal-fails-restore-fail', 'vnfInstanceDescription',
        'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED', 'cluster-1', '39rf76re-b16d-45fb-acb2-f2c631cb19ed', true);



INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES ('23ty78oi-b16d-45fb-acb2-f2c631cb19ed', 'q9r77165-7065-49b1-831c-d687130c6123', 'COMPLETED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES ('45ty78oi-b16d-45fb-acb2-f2c631cb19ed', 'vb577165-7065-49b1-831c-d687130c6123', 'COMPLETED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES ('78rxdqoi-b16d-45fb-acb2-f2c631cb19ed', 'lk817165-7065-49b1-831c-d687130c6123', 'COMPLETED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES ('39rf76re-b16d-45fb-acb2-f2c631cb19ed', 'po45mf0t-7065-49b1-831c-d687130c6123', 'COMPLETED',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{}', 'FALSE', 'FORCEFUL', null);
