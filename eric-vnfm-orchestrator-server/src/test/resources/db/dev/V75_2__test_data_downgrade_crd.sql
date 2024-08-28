-- DownsizeOperationTest - testDeletePvcOperationSuccessForAllCharts
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources,
temp_instance)
VALUES ('wf1ce-rd14-477c-crd0-downsize0100', 'multiple-charts-downsize', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd1f-4674-crd-downsize0001', 'multiple-charts', 'multiple-charts-downsize', 'true',
 '{"vnfInstanceId":"wf1ce-rd14-477c-crd0-downsize0100","vnfInstanceName":"multiple-charts-downsize","vnfInstanceDescription":"vnfInstanceDescription","vnfDescriptorId":"d3def1ce-4cf4-477c-aab3-21cb04e6a379",' ||
 '"vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9392468011745350001","instantiationState":"INSTANTIATED",' ||
 '"clusterName":"multiple-charts","namespace":"multiple-charts-downsize","helmCharts":[{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-sec-sip-crd-1.1.0.tgz","priority":1,"releaseName":"multiple-charts-downsize-1","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-crd0-downsize0101", "helmChartType":"CRD"},{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-crd-1.1.0.tgz","priority":2,"releaseName":"multiple-charts-downsize-2","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-crd1-downsize0102","helmChartType":"CRD"},{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz","priority":3,"releaseName":"multiple-charts-downsize-3","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-cnf0-downsize0102","helmChartType":"CNF"}],"operationOccurrenceId":"wm8fcbc8-rd1f-4674-crd-downsize0001",' ||
 '"allOperations":[{"operationOccurrenceId":"wm8fcbc8-rd1f-4674-crd-downsize0001","operationState":"PROCESSING","grantId":null,"lifecycleOperationType":"CHANGE_VNFPKG","automaticInvocation":false,"operationParams":null,' ||
 '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":null,"vnfProductName":null,"combinedValuesFile":null,"combinedAdditionalParams":null,"resourceDetails":null,"scaleInfoEntities":null,' ||
 '"sourceVnfdId":null,"targetVnfdId":null,"deleteNodeFailed":false,"deleteNodeErrorMessage":null,"deleteNodeFinished":false,"setAlarmSupervisionErrorMessage":null,"cancelModeType":"FORCEFUL"}],"ossTopology":"{}",' ||
 '"instantiateOssTopology":"{}","addNodeOssTopology":"{}","addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"disableAlarmSupervisionPythonFile":null,"enableAlarmSupervisionPythonFile":null,' ||
 '"combinedValuesFile":null,"combinedAdditionalParams":null,"policies":null,"resourceDetails":null,"scaleInfoEntity":[],"manoControlledScaling":null,"tempInstance":null,"overrideGlobalRegistry":true,"metadata":null,' ||
 '"alarmSupervisionStatus":null,"cleanUpResources":true,"sitebasicFile":null,"healSupported":null,"allStringValuesAsArray":["wf1ce-rd14-477c-crd0-downsize0100","vnfInstanceDescription","multiple-charts-downsize"]}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, expired_application_time, downsize_allowed)
VALUES ('wm8fcbc8-rd1f-4674-crd-downsize0001', 'wf1ce-rd14-477c-crd0-downsize0100', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', null, 'FALSE', 'FORCEFUL', null, CURRENT_TIMESTAMP, true);

INSERT INTO helm_chart(id, vnf_id,
helm_chart_url,
priority, release_name, state, downsize_state, helm_chart_type)
VALUES
('wf1ce-rd14-477c-crd0-downsize0101', 'wf1ce-rd14-477c-crd0-downsize0100',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-sec-sip-crd-1.1.0.tgz',
 '1', 'multiple-charts-downsize-1', null, null, 'CRD'),
('wf1ce-rd14-477c-crd1-downsize0102', 'wf1ce-rd14-477c-crd0-downsize0100',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/eric-certm-crd-1.1.0.tgz',
'2', 'multiple-charts-downsize-2', null, null, 'CRD'),
('wf1ce-rd14-477c-cnf0-downsize0102', 'wf1ce-rd14-477c-crd0-downsize0100',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz',
'3', 'multiple-charts-downsize-3', null, null, 'CNF');