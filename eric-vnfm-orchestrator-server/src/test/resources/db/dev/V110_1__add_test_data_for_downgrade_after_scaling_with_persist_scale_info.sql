INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, resource_details, is_heal_supported, supported_operations,
                             combined_additional_params)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120006', 'my-release-name-granting-upgrade-after-scale-with-persist-scale-info', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zz-ccvp-scale', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432940', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        '{"eric-pm-bulk-reporter":1}',
        true, '[
      {
        "operationName": "instantiate",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "terminate",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "heal",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "change_package",
        "supported": true,
        "errorMessage": null
      },
      {
        "operationName": "scale",
        "supported": true,
        "errorMessage": null
      }
    ]',
    '{"helmNoHooks":false, "disableOpenapiValidation":true}');

UPDATE app_vnf_instance SET vnfd_id = 'single-chart-527c-arel4-5fcb086597zs' WHERE vnf_id = '186dc69a-0c2f-11ed-861d-0242ac120003';

-- Lifecycle operation (Upgrade)
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, source_vnfd_id, target_vnfd_id)
VALUES ('b744d48d-ec2f-446f-8d65-02832b534b73', '186dc69a-0c2f-11ed-861d-0242ac120006', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'CHANGE_VNFPKG', 'FALSE', '{"additionalParams": {"applicationTimeOut": 100}}', 'FALSE', 'FORCEFUL', null,
'single-chart-c-rel4-545379754e31-ccvp-scale', 'single-chart-527c-arel4-5fcb086597zz-ccvp-scale');
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, source_vnfd_id, target_vnfd_id)
VALUES ('b744d48d-ec2f-446f-8d65-02832b534b74', '186dc69a-0c2f-11ed-861d-0242ac120006', 'COMPLETED',
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null, 'INSTANTIATE', 'FALSE', '{"additionalParams": {"applicationTimeOut": 300}}', 'FALSE', 'FORCEFUL', null,
'single-chart-c-rel4-545379754e31-ccvp-scale', 'single-chart-c-rel4-545379754e31-ccvp-scale');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('b744d48d-ec2f-446f-8d65-02832b534b73','UPGRADE','downgrade-cf4-477c-aab3-21c454e6a389');

--CNF_charts
INSERT INTO helm_chart(id, vnf_id, helm_chart_name, helm_chart_url, priority, release_name, state, helm_chart_artifact_key, replica_details)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120006a', '186dc69a-0c2f-11ed-861d-0242ac120006', 'spider-app-2.74.7',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', '4',
        'my-release-name-granting-upgrade-after-scale-with-persist-scale-info-1', 'COMPLETED', 'helm_package2',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":"eric-pm-bulk-reporter.autoscaling.minReplicas","minReplicasCount":null,
        "maxReplicasParameterName":"eric-pm-bulk-reporter.autoscaling.maxReplicas","maxReplicasCount":null,
        "scalingParameterName":"eric-pm-bulk-reporter.replicaCount","currentReplicaCount":1,
        "autoScalingEnabledParameterName":"eric-pm-bulk-reporter.autoscaling.engine.enabled","autoScalingEnabledValue":false}}');

INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count, life_cycle_operation_id)
  VALUES ('8cf641a9-3ea2-474c-be4c-0d9434b3183f', 'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 4,
  'my-release-name-granting-upgrade-after-scale-with-persist-scale-info-1', 'COMPLETED',
  '2', 1, 'b744d48d-ec2f-446f-8d65-02832b534b74');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES ('186dc69a-0c2f-si1-861d-0242ac120006a', '186dc69a-0c2f-11ed-861d-0242ac120006', 'Aspect1', 0);