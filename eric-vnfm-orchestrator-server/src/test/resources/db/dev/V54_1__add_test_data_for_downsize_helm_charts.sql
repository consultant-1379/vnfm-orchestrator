-- DownsizeOperationTest - testDeletePvcOperationSuccessForAllCharts
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources,
temp_instance)
VALUES ('wf1ce-rd14-477c-vnf0-downsize0100', 'multiple-charts-downsize', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'wm8fcbc8-rd1f-4673-oper-downsize0001', 'multiple-charts', 'multiple-charts-downsize', 'true',
 '{"vnfInstanceId":"wf1ce-rd14-477c-vnf0-downsize0100","vnfInstanceName":"multiple-charts-downsize","vnfInstanceDescription":"vnfInstanceDescription","vnfDescriptorId":"d3def1ce-4cf4-477c-aab3-21cb04e6a379",' ||
 '"vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9392468011745350001","instantiationState":"INSTANTIATED",' ||
 '"clusterName":"multiple-charts","namespace":"multiple-charts-downsize","helmCharts":[{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz","priority":1,' ||
 '"releaseName":"multiple-charts-downsize-1","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0101"},' ||
 '{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz","priority":2,"releaseName":"multiple-charts-downsize-2","revisionNumber":null,"state":"COMPLETED",' ||
 '"retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0102"}],"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0001",' ||
 '"allOperations":[{"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0001","operationState":"PROCESSING","grantId":null,"lifecycleOperationType":"CHANGE_VNFPKG","automaticInvocation":false,"operationParams":null,' ||
 '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":null,"vnfProductName":null,"combinedValuesFile":null,"combinedAdditionalParams":null,"resourceDetails":null,"scaleInfoEntities":null,' ||
 '"sourceVnfdId":null,"targetVnfdId":null,"deleteNodeFailed":false,"deleteNodeErrorMessage":null,"deleteNodeFinished":false,"setAlarmSupervisionErrorMessage":null,"cancelModeType":"FORCEFUL"}],"ossTopology":"{}",' ||
 '"instantiateOssTopology":"{}","addNodeOssTopology":"{}","addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"disableAlarmSupervisionPythonFile":null,"enableAlarmSupervisionPythonFile":null,' ||
 '"combinedValuesFile":null,"combinedAdditionalParams":null,"policies":null,"resourceDetails":null,"scaleInfoEntity":[],"manoControlledScaling":null,"tempInstance":null,"overrideGlobalRegistry":true,"metadata":null,' ||
 '"alarmSupervisionStatus":null,"cleanUpResources":true,"sitebasicFile":null,"healSupported":null,"allStringValuesAsArray":["wf1ce-rd14-477c-vnf0-downsize0100","vnfInstanceDescription","multiple-charts-downsize"]}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, expired_application_time)
VALUES ('wm8fcbc8-rd1f-4673-oper-downsize0001', 'wf1ce-rd14-477c-vnf0-downsize0100', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', null, 'FALSE', 'FORCEFUL', null, CURRENT_TIMESTAMP);

INSERT INTO helm_chart(id, vnf_id,
helm_chart_url,
priority, release_name, state, downsize_state)
VALUES
('wf1ce-rd14-477c-helm-downsize0101', 'wf1ce-rd14-477c-vnf0-downsize0100',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz',
 '1', 'multiple-charts-downsize-1', 'COMPLETED', 'PROCESSING'),
('wf1ce-rd14-477c-helm-downsize0102', 'wf1ce-rd14-477c-vnf0-downsize0100',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz',
'2', 'multiple-charts-downsize-2', 'COMPLETED', 'PROCESSING');



-- DownsizeOperationTest - testDownsizeOperationPassFirstChartFailSecondChart
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description,
vnfd_id, vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources,
temp_instance)
VALUES ('wf1ce-rd14-477c-vnf0-downsize0200', 'multiple-charts-downsize-fail-second', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'9392468011745350001', 'INSTANTIATED', 'wm8fcbc8-rd1f-4673-oper-downsize0002', 'multiple-charts', 'multiple-charts-downsize-fail-second', 'true',
 '{"vnfInstanceId":"wf1ce-rd14-477c-vnf0-downsize0200","vnfInstanceName":"multiple-charts-downsize-fail-second","vnfInstanceDescription":"vnfInstanceDescription","vnfDescriptorId":"d3def1ce-4cf4-477c-aab3-21cb04e6a379",' ||
 '"vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9392468011745350001","instantiationState":"INSTANTIATED",' ||
 '"clusterName":"multiple-charts","namespace":"multiple-charts-downsize-fail-second","helmCharts":[{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz","priority":1,' ||
 '"releaseName":"multiple-charts-downsize-fail-second-1","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0201"},' ||
 '{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz","priority":2,"releaseName":"multiple-charts-downsize-fail-second-2","revisionNumber":null,"state":"COMPLETED",' ||
 '"retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0202"}],"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0002",' ||
 '"allOperations":[{"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0002","operationState":"PROCESSING","grantId":null,"lifecycleOperationType":"CHANGE_VNFPKG","automaticInvocation":false,"operationParams":null,' ||
 '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":null,"vnfProductName":null,"combinedValuesFile":null,"combinedAdditionalParams":null,"resourceDetails":null,"scaleInfoEntities":null,' ||
 '"sourceVnfdId":null,"targetVnfdId":null,"deleteNodeFailed":false,"deleteNodeErrorMessage":null,"deleteNodeFinished":false,"setAlarmSupervisionErrorMessage":null,"cancelModeType":"FORCEFUL"}],"ossTopology":"{}",' ||
 '"instantiateOssTopology":"{}","addNodeOssTopology":"{}","addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"disableAlarmSupervisionPythonFile":null,"enableAlarmSupervisionPythonFile":null,' ||
 '"combinedValuesFile":null,"combinedAdditionalParams":null,"policies":null,"resourceDetails":null,"scaleInfoEntity":[],"manoControlledScaling":null,"tempInstance":null,"overrideGlobalRegistry":true,"metadata":null,' ||
 '"alarmSupervisionStatus":null,"cleanUpResources":true,"sitebasicFile":null,"healSupported":null,"allStringValuesAsArray":["wf1ce-rd14-477c-vnf0-downsize0200","vnfInstanceDescription","multiple-charts-downsize-fail-second"]}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, expired_application_time)
VALUES ('wm8fcbc8-rd1f-4673-oper-downsize0002', 'wf1ce-rd14-477c-vnf0-downsize0200', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', null, 'FALSE', 'FORCEFUL', null, CURRENT_TIMESTAMP);

INSERT INTO helm_chart(id, vnf_id,
helm_chart_url,
priority, release_name, state, downsize_state)
VALUES
('wf1ce-rd14-477c-helm-downsize0201', 'wf1ce-rd14-477c-vnf0-downsize0200',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz',
 '1', 'multiple-charts-downsize-fail-second-1', 'COMPLETED', 'PROCESSING'),
('wf1ce-rd14-477c-helm-downsize0202', 'wf1ce-rd14-477c-vnf0-downsize0200',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz',
'2', 'multiple-charts-downsize-fail-second-2', 'COMPLETED', 'PROCESSING');



-- DownsizeOperationTest - testDownsizeOperationFailFirstChartPassSecondChart
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description,
vnfd_id, vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources,
temp_instance)
VALUES ('wf1ce-rd14-477c-vnf0-downsize0300', 'multiple-charts-downsize-fail-first', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'9392468011745350001', 'INSTANTIATED', 'wm8fcbc8-rd1f-4673-oper-downsize0003', 'multiple-charts', 'multiple-charts-downsize-fail-first', 'true',
 '{"vnfInstanceId":"wf1ce-rd14-477c-vnf0-downsize0300","vnfInstanceName":"multiple-charts-downsize-fail-first","vnfInstanceDescription":"vnfInstanceDescription","vnfDescriptorId":"d3def1ce-4cf4-477c-aab3-21cb04e6a379",' ||
 '"vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9392468011745350001","instantiationState":"INSTANTIATED",' ||
 '"clusterName":"multiple-charts","namespace":"multiple-charts-downsize-fail-first","helmCharts":[{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz","priority":1,' ||
 '"releaseName":"multiple-charts-downsize-fail-first-1","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0301"},' ||
 '{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz","priority":2,"releaseName":"multiple-charts-downsize-fail-first-2","revisionNumber":null,"state":"COMPLETED",' ||
 '"retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0302"}],"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0003",' ||
 '"allOperations":[{"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0003","operationState":"PROCESSING","grantId":null,"lifecycleOperationType":"CHANGE_VNFPKG","automaticInvocation":false,"operationParams":null,' ||
 '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":null,"vnfProductName":null,"combinedValuesFile":null,"combinedAdditionalParams":null,"resourceDetails":null,"scaleInfoEntities":null,' ||
 '"sourceVnfdId":null,"targetVnfdId":null,"deleteNodeFailed":false,"deleteNodeErrorMessage":null,"deleteNodeFinished":false,"setAlarmSupervisionErrorMessage":null,"cancelModeType":"FORCEFUL"}],"ossTopology":"{}",' ||
 '"instantiateOssTopology":"{}","addNodeOssTopology":"{}","addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"disableAlarmSupervisionPythonFile":null,"enableAlarmSupervisionPythonFile":null,' ||
 '"combinedValuesFile":null,"combinedAdditionalParams":null,"policies":null,"resourceDetails":null,"scaleInfoEntity":[],"manoControlledScaling":null,"tempInstance":null,"overrideGlobalRegistry":true,"metadata":null,' ||
 '"alarmSupervisionStatus":null,"cleanUpResources":true,"sitebasicFile":null,"healSupported":null,"allStringValuesAsArray":["wf1ce-rd14-477c-vnf0-downsize0300","vnfInstanceDescription","multiple-charts-downsize-fail-first"]}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, expired_application_time)
VALUES ('wm8fcbc8-rd1f-4673-oper-downsize0003', 'wf1ce-rd14-477c-vnf0-downsize0300', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', null, 'FALSE', 'FORCEFUL', null, CURRENT_TIMESTAMP);

INSERT INTO helm_chart(id, vnf_id,
helm_chart_url,
priority, release_name, state, downsize_state)
VALUES
('wf1ce-rd14-477c-helm-downsize0301', 'wf1ce-rd14-477c-vnf0-downsize0300',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz',
 '1', 'multiple-charts-downsize-fail-first-1', 'COMPLETED', 'PROCESSING'),
('wf1ce-rd14-477c-helm-downsize0302', 'wf1ce-rd14-477c-vnf0-downsize0300',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz',
'2', 'multiple-charts-downsize-fail-first-2', 'COMPLETED', 'PROCESSING');



-- DownsizeOperationTest - testDownsizeOperationPassBothFailUpgrade
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description,
vnfd_id, vnf_provider, vnf_product_name, vnf_software_version, vnfd_version,
vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, clean_up_resources,
temp_instance)
VALUES ('wf1ce-rd14-477c-vnf0-downsize0400', 'multiple-charts-downsize-fail-upgrade', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
'9392468011745350001', 'INSTANTIATED', 'wm8fcbc8-rd1f-4673-oper-downsize0004', 'multiple-charts', 'multiple-charts-downsize-fail-upgrade', 'true',
 '{"vnfInstanceId":"wf1ce-rd14-477c-vnf0-downsize0400","vnfInstanceName":"multiple-charts-downsize-fail-upgrade","vnfInstanceDescription":"vnfInstanceDescription","vnfDescriptorId":"d3def1ce-4cf4-477c-aab3-21cb04e6a379",' ||
 '"vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"9392468011745350001","instantiationState":"INSTANTIATED",' ||
 '"clusterName":"multiple-charts","namespace":"multiple-charts-downsize-fail-upgrade","helmCharts":[{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz","priority":1,' ||
 '"releaseName":"multiple-charts-downsize-fail-upgrade-1","revisionNumber":null,"state":"COMPLETED","retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0401"},' ||
 '{"helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz","priority":2,"releaseName":"multiple-charts-downsize-fail-upgrade-2","revisionNumber":null,"state":"COMPLETED",' ||
 '"retryCount":0,"deletePvcState":null,"id":"wf1ce-rd14-477c-helm-downsize0402"}],"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0004",' ||
 '"allOperations":[{"operationOccurrenceId":"wm8fcbc8-rd1f-4673-oper-downsize0004","operationState":"PROCESSING","grantId":null,"lifecycleOperationType":"CHANGE_VNFPKG","automaticInvocation":false,"operationParams":null,' ||
 '"cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":null,"vnfProductName":null,"combinedValuesFile":null,"combinedAdditionalParams":null,"resourceDetails":null,"scaleInfoEntities":null,' ||
 '"sourceVnfdId":null,"targetVnfdId":null,"deleteNodeFailed":false,"deleteNodeErrorMessage":null,"deleteNodeFinished":false,"setAlarmSupervisionErrorMessage":null,"cancelModeType":"FORCEFUL"}],"ossTopology":"{}",' ||
 '"instantiateOssTopology":"{}","addNodeOssTopology":"{}","addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"disableAlarmSupervisionPythonFile":null,"enableAlarmSupervisionPythonFile":null,' ||
 '"combinedValuesFile":null,"combinedAdditionalParams":null,"policies":null,"resourceDetails":null,"scaleInfoEntity":[],"manoControlledScaling":null,"tempInstance":null,"overrideGlobalRegistry":true,"metadata":null,' ||
 '"alarmSupervisionStatus":null,"cleanUpResources":true,"sitebasicFile":null,"healSupported":null,"allStringValuesAsArray":["wf1ce-rd14-477c-vnf0-downsize0400","vnfInstanceDescription","multiple-charts-downsize-fail-upgrade"]}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, expired_application_time)
VALUES ('wm8fcbc8-rd1f-4673-oper-downsize0004', 'wf1ce-rd14-477c-vnf0-downsize0400', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', null, 'FALSE', 'FORCEFUL', null, CURRENT_TIMESTAMP);

INSERT INTO helm_chart(id, vnf_id,
helm_chart_url,
priority, release_name, state, downsize_state)
VALUES
('wf1ce-rd14-477c-helm-downsize0401', 'wf1ce-rd14-477c-vnf0-downsize0400',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz',
 '1', 'multiple-charts-downsize-fail-upgrade-1', 'COMPLETED', 'PROCESSING'),
('wf1ce-rd14-477c-helm-downsize0402', 'wf1ce-rd14-477c-vnf0-downsize0400',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.8.tgz',
'2', 'multiple-charts-downsize-fail-upgrade-2', 'COMPLETED', 'PROCESSING');