-- Data for testRollbackFailureForSingleChartWhenUpdateUsageStateThrowError
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace)
VALUES ('18cd2cf8-1fa9-46aa-91b1-fe0ee23af3e3', 'rollback-single-failed', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 '342fg9d-b9e4-4c68-8685-76c90d890pty', 'multiple-charts', 'testrollback');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('342fg9d-b9e4-4c68-8685-76c90d890pty', '18cd2cf8-1fa9-46aa-91b1-fe0ee23af3e3', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('abcd9c78-da5c-4f57-92f2-3e3602660000', '18cd2cf8-1fa9-46aa-91b1-fe0ee23af3e3',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-single-failed-2', 'COMPLETED');

UPDATE app_vnf_instance
SET temp_instance = ' {"vnfInstanceId":"3820edc8d-807a-4f16-a675-b6599defb085","vnfInstanceName":"rollback-single-failed-2","vnfInstanceDescription":null,"vnfDescriptorId":"multi-chart-569d-xyz3-5g15f7h497","vnfProviderName":"Ericsson","vnfProductName":"2CHART-VNF","vnfSoftwareVersion":"1.55 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"b42fa36f-eb29-4d71-9ad4-89303d2e1f93","instantiationState":"INSTANTIATED","clusterName":"default","namespace":"testrollback","helmCharts":[{"id":null,"helmChartUrl":"http://eric-lcm-helm-chart-registry.default:8080/onboarded/charts/test-scale-chart-0.1.1.tgz","priority":1,"releaseName":"rollback-single-failed-2","state":"COMPLETED"}],"operationOccurrenceId":"64bdf09d-b9e4-4c68-8685-76c90d3d39b9","allOperations":null,"ossTopology":"{}","instantiateOssTopology":"{}","addNodeOssTopology":null,"addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":"{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false},\"tls\":{\"dced\":{\"enabled\":false}}},\"eric-pm-server\":{\"server\":{\"ingress\":{\"enabled\":false},\"persistentVolume\":{\"storageClass\":\"erikube-rbd\"}}},\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2.rontgen010.seli.gic.ericsson.se\"}},\"pm-testapp\":{\"ingress\":{\"domain\":\"rontgen010.seli.gic.ericsson.se\"}},\"tags\":{\"all\":false,\"pm\":true}}","combinedAdditionalParams":"{}","policies":"{\"allScalingAspects\":{\"ScalingAspects1\":{\"type\":\"tosca.policies.nfv.ScalingAspects\",\"properties\":{\"aspects\":{\"Aspect4\":{\"name\":\"Aspect4\",\"description\":\"Scale level 0-6 maps to 4-10 for test-cnf VNFC instances, maps to 3-9 for test-cnf-vnfc1 VNFC instances, maps to 5-11 for test-cnf-vnfc2 VNFC instances, maps to 1-7 for test-cnf-vnfc3 VNFC instances and maps to 2-8 for test-cnf-vnfc4 VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":6,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas4\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect4\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\",\"test-cnf-vnfc3\",\"test-cnf-vnfc2\",\"test-cnf-vnfc4\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}}}},\"Aspect1\":{\"name\":\"Aspect1\",\"description\":\"Scale level 0-10 maps to 1-41 for test-cnf-vnfc3 VNFC instances and also maps to 5-45 for test-cnf-vnfc2VNFC instances (4 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas2\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"test-cnf-vnfc3\",\"test-cnf-vnfc2\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}}}},\"Aspect2\":{\"name\":\"Aspect2\",\"description\":\"Scale level 0-7 maps to 6-28 for test-cnf-vnfc4 VNFC instances and maps to 5-27 for test-cnf-vnfc3 VNFC instances (4 instance in first scale level, 1 instance in second scale level, 9 instance in third scale level and 3 instance in all the next scale levels)\\n\",\"max_scale_level\":7,\"step_deltas\":[\"delta_1\",\"delta_2\",\"delta_3\",\"delta_4\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect2\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},\"delta_2\":{\"number_of_instances\":1},\"delta_3\":{\"number_of_instances\":9},\"delta_4\":{\"number_of_instances\":3}}},\"targets\":[\"test-cnf-vnfc4\",\"test-cnf-vnfc3\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]}}}}},\"Aspect3\":{\"name\":\"Aspect3\",\"description\":\"Scale level 0-12 maps to 4-28 for test-cnf VNFC instances and also maps to 3-27 for test-cnf-vnfc1 VNFC instances (2 instance per scale step)\\n\",\"max_scale_level\":12,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":2}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]}}}}}}}}},\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}},\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas2\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"test-cnf-vnfc3\",\"test-cnf-vnfc2\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}},\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect2\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},\"delta_2\":{\"number_of_instances\":1},\"delta_3\":{\"number_of_instances\":9},\"delta_4\":{\"number_of_instances\":3}}},\"targets\":[\"test-cnf-vnfc4\",\"test-cnf-vnfc3\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]}}},\"Payload_ScalingAspectDeltas4\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect4\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\",\"test-cnf-vnfc3\",\"test-cnf-vnfc2\",\"test-cnf-vnfc4\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}},\"Payload_ScalingAspectDeltas3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":2}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]}}}}}","resourceDetails":"{\"test-cnf-vnfc2\":1,\"test-cnf-vnfc3\":1,\"test-cnf-vnfc4\":1,\"test-cnf\":1,\"eric-pm-bulk-reporter\":1,\"test-cnf-vnfc1\":1}","scaleInfoEntity":[{"scaleInfoId":null,"aspectId":"Aspect4","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect2","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect3","scaleLevel":0}],"manoControlledScaling":false,"tempInstance":null,"allStringValuesAsArray":["a20edc8d-807a-4f16-a675-b6599defb085",null,"rollback-single-failed-2"]}'
WHERE vnf_id = '18cd2cf8-1fa9-46aa-91b1-fe0ee23af3e3';



-- Data for testRollbackSuccessForSingleChartWhenUpdateUsageStateThrowError
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, current_life_cycle_operation_id, cluster_name, namespace, policies)
VALUES ('4a9a2d13-e9ae-4ba8-a51c-a8079bc7c659', 'rollback-single-success', 'vnfInstanceDescription',
'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME',
 '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'INSTANTIATED',
 'fa88b87a-bf7c-43ac-8b18-1f6494c124a7', 'multiple-charts', 'testrollback', '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1"]},"Payload_2":{"name":"Payload_2","description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":5,"step_deltas":["delta_2"]}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4}}},"targets":["PL__scaled_vm","CL_scaled_vm"]}}}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES ('fa88b87a-bf7c-43ac-8b18-1f6494c124a7', '4a9a2d13-e9ae-4ba8-a51c-a8079bc7c659', 'PROCESSING',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_PACKAGE_INFO', 'FALSE', null, 'FALSE', 'FORCEFUL', null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state)
VALUES ('528c0405-4683-4301-8307-1ac2528a5bff', '4a9a2d13-e9ae-4ba8-a51c-a8079bc7c659',
'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
'rollback-single-success-2', 'COMPLETED');

UPDATE app_vnf_instance
SET temp_instance = ' {"vnfInstanceId":"4a9a2d13-e9ae-4ba8-a51c-a8079bc7c659","vnfInstanceName":"rollback-single-success","vnfInstanceDescription":null,"vnfDescriptorId":"multi-chart-569d-xyz3-5g15f7h497","vnfProviderName":"Ericsson","vnfProductName":"2CHART-VNF","vnfSoftwareVersion":"1.55 (CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"b42fa36f-eb29-4d71-9ad4-89303d2e1f93","instantiationState":"INSTANTIATED","clusterName":"default","namespace":"testrollback","helmCharts":[{"id":null,"helmChartUrl":"http://eric-lcm-helm-chart-registry.default:8080/onboarded/charts/test-scale-chart-0.1.1.tgz","priority":1,"releaseName":"rollback-single-success-2","state":null}],"operationOccurrenceId":"fa88b87a-bf7c-43ac-8b18-1f6494c124a7","allOperations":null,"ossTopology":"{}","instantiateOssTopology":"{}","addNodeOssTopology":null,"addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":"{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false},\"tls\":{\"dced\":{\"enabled\":false}}},\"eric-pm-server\":{\"server\":{\"ingress\":{\"enabled\":false},\"persistentVolume\":{\"storageClass\":\"erikube-rbd\"}}},\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2.rontgen010.seli.gic.ericsson.se\"}},\"pm-testapp\":{\"ingress\":{\"domain\":\"rontgen010.seli.gic.ericsson.se\"}},\"tags\":{\"all\":false,\"pm\":true}}","combinedAdditionalParams":"{}","policies":"{\"allScalingAspects\":{\"ScalingAspects1\":{\"type\":\"tosca.policies.nfv.ScalingAspects\",\"properties\":{\"aspects\":{\"Aspect4\":{\"name\":\"Aspect4\",\"description\":\"Scale level 0-6 maps to 4-10 for test-cnf VNFC instances, maps to 3-9 for test-cnf-vnfc1 VNFC instances, maps to 5-11 for test-cnf-vnfc2 VNFC instances, maps to 1-7 for test-cnf-vnfc3 VNFC instances and maps to 2-8 for test-cnf-vnfc4 VNFC instances (1 instance per scale step)\\n\",\"max_scale_level\":6,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas4\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect4\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\",\"test-cnf-vnfc3\",\"test-cnf-vnfc2\",\"test-cnf-vnfc4\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}}}},\"Aspect1\":{\"name\":\"Aspect1\",\"description\":\"Scale level 0-10 maps to 1-41 for test-cnf-vnfc3 VNFC instances and also maps to 5-45 for test-cnf-vnfc2VNFC instances (4 instance per scale step)\\n\",\"max_scale_level\":10,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas2\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"test-cnf-vnfc3\",\"test-cnf-vnfc2\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}}}},\"Aspect2\":{\"name\":\"Aspect2\",\"description\":\"Scale level 0-7 maps to 6-28 for test-cnf-vnfc4 VNFC instances and maps to 5-27 for test-cnf-vnfc3 VNFC instances (4 instance in first scale level, 1 instance in second scale level, 9 instance in third scale level and 3 instance in all the next scale levels)\\n\",\"max_scale_level\":7,\"step_deltas\":[\"delta_1\",\"delta_2\",\"delta_3\",\"delta_4\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect2\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},\"delta_2\":{\"number_of_instances\":1},\"delta_3\":{\"number_of_instances\":9},\"delta_4\":{\"number_of_instances\":3}}},\"targets\":[\"test-cnf-vnfc4\",\"test-cnf-vnfc3\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]}}}}},\"Aspect3\":{\"name\":\"Aspect3\",\"description\":\"Scale level 0-12 maps to 4-28 for test-cnf VNFC instances and also maps to 3-27 for test-cnf-vnfc1 VNFC instances (2 instance per scale step)\\n\",\"max_scale_level\":12,\"step_deltas\":[\"delta_1\"],\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":2}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]}}}}}}}}},\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}},\"allScalingAspectDelta\":{\"Payload_ScalingAspectDeltas2\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect1\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4}}},\"targets\":[\"test-cnf-vnfc3\",\"test-cnf-vnfc2\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}},\"Payload_ScalingAspectDeltas1\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect2\",\"deltas\":{\"delta_1\":{\"number_of_instances\":4},\"delta_2\":{\"number_of_instances\":1},\"delta_3\":{\"number_of_instances\":9},\"delta_4\":{\"number_of_instances\":3}}},\"targets\":[\"test-cnf-vnfc4\",\"test-cnf-vnfc3\"],\"allInitialDelta\":{\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]}}},\"Payload_ScalingAspectDeltas4\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect4\",\"deltas\":{\"delta_1\":{\"number_of_instances\":1}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\",\"test-cnf-vnfc3\",\"test-cnf-vnfc2\",\"test-cnf-vnfc4\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc3\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc3\"]},\"vnfc4\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc4\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]},\"vnfc2\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc2\"]}}},\"Payload_ScalingAspectDeltas3\":{\"type\":\"tosca.policies.nfv.VduScalingAspectDeltas\",\"properties\":{\"aspect\":\"Aspect3\",\"deltas\":{\"delta_1\":{\"number_of_instances\":2}}},\"targets\":[\"test-cnf\",\"test-cnf-vnfc1\"],\"allInitialDelta\":{\"vnfc1.test-cnf\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf\"]},\"vnfc1\":{\"type\":\"tosca.policies.nfv.VduInitialDelta\",\"properties\":{\"initial_delta\":{\"number_of_instances\":1}},\"targets\":[\"test-cnf-vnfc1\"]}}}}}","resourceDetails":"{\"test-cnf-vnfc2\":1,\"test-cnf-vnfc3\":1,\"test-cnf-vnfc4\":1,\"test-cnf\":1,\"eric-pm-bulk-reporter\":1,\"test-cnf-vnfc1\":1}","scaleInfoEntity":[{"scaleInfoId":null,"aspectId":"Aspect4","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect1","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect2","scaleLevel":0},{"scaleInfoId":null,"aspectId":"Aspect3","scaleLevel":0}],"manoControlledScaling":false,"tempInstance":null,"allStringValuesAsArray":["fb75f150-74cd-11ea-bc55-0242ac130003",null,"rollback-single-success"]}'
WHERE vnf_id = '4a9a2d13-e9ae-4ba8-a51c-a8079bc7c659' ;
