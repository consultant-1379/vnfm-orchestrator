-------- VNF Instances -----------
INSERT INTO app_vnf_instance(vnf_id, namespace, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, supported_operations)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120dm1', 'dm-granting-upgrade-namespace', 'dm-granting-upgrade', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcsourcedm1', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadsourcedm1', 'INSTANTIATED',
        'granting-dm-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
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
    ]');

--------- Helm Charts (CRD) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcrdh1', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.0.0tgz',
        'eric-sec-sip-tls-crd', '1.0.0', 'crd_package1', '1',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcrdh2', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/eric-sec-certm-crd-2.0.0tgz',
        'eric-sec-certm-crd', '2.0.0', 'crd_package2', '2',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcrdh3', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/scale-crd-3.0.0.tgz',
        'scale-crd', '3.0.0', 'crd_package3', '3',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false);

--------- Helm Charts (CNF) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled, replica_details)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcnfh1', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/spider-app1-1.0.0.tgz',
        'spider-app1', '1.0.0', 'helm_package1', '4',
        'dm-granting-upgrade-1', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter1":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter1.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcnfh2', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/spider-app2-2.0.0.tgz',
        'spider-app2', '2.0.0', 'helm_package2', '5',
        'dm-granting-upgrade-2', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter2":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter2.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcnfh3', '186dc69a-0c2f-11ed-861d-0242ac120dm1',
        'https://localhost/onboarded/charts/spider-app3-3.0.0.tgz',
        'spider-app3', '3.0.0', 'helm_package3', '6',
        'dm-granting-upgrade-3', 'COMPLETED', 'CNF', true, '{"eric-pm-bulk-reporter3":{"minReplicasParameterName":null,"minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter3.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}');

---------- Lifecycle Operations ------------
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('254f9ab0-5c3d-4bb5-a602-9557c1sourceopdm1', '186dc69a-0c2f-11ed-861d-0242ac120dm1', 'COMPLETED', '2024-04-27 14:37:39.276360',
 '2024-04-27 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"eric-pm-bulk-reporter3":{"replicaCount":1}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"eric-pm-bulk-reporter":{"replicaCount":116},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"eric-pm-bulk-reporter3":1}',
 '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

-------------------------------------------------------------------------------------------
-------- VNF Instances -----------
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, supported_operations)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120dm2', 'dm-granting-upgrade', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcsourcedm1', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadsourcedm1', 'INSTANTIATED',
        'granting-dm-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
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
    ]');

--------- Helm Charts (CRD) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcrdh4', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.0.0tgz',
        'eric-sec-sip-tls-crd', '1.0.0', 'crd_package1', '1',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcrdh5', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/eric-sec-certm-crd-2.0.0tgz',
        'eric-sec-certm-crd', '2.0.0', 'crd_package2', '2',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcrdh6', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/scale-crd-3.0.0.tgz',
        'scale-crd', '3.0.0', 'crd_package3', '3',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', true);

--------- Helm Charts (CNF) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled, replica_details)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcnfh4', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/spider-app1-1.0.0.tgz',
        'spider-app1', '1.0.0', 'helm_package1', '4',
        'dm-granting-upgrade-1', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter1":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter1.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcnfh5', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/spider-app2-2.0.0.tgz',
        'spider-app2', '2.0.0', 'helm_package2', '5',
        'dm-granting-upgrade-2', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter2":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter2.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcnfh6', '186dc69a-0c2f-11ed-861d-0242ac120dm2',
        'https://localhost/onboarded/charts/spider-app3-3.0.0.tgz',
        'spider-app3', '3.0.0', 'helm_package3', '6',
        'dm-granting-upgrade-3', 'COMPLETED', 'CNF', true, '{"eric-pm-bulk-reporter3":{"minReplicasParameterName":null,"minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter3.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}');

---------- Lifecycle Operations ------------
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('254f9ab0-5c3d-4bb5-a602-9557c1sourceopdm2', '186dc69a-0c2f-11ed-861d-0242ac120dm2', 'COMPLETED', '2024-04-27 14:37:39.276360',
 '2024-04-27 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"eric-pm-bulk-reporter3":{"replicaCount":1}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"eric-pm-bulk-reporter":{"replicaCount":116},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"eric-pm-bulk-reporter3":1}',
 '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');

-------------------------------------------------------------------------------------------
-------- VNF Instances -----------
INSERT INTO app_vnf_instance(vnf_id, namespace, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, supported_operations)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120dm3', 'dm-granting-upgrade-namespace', 'dm-granting-upgrade', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcsourcedm1', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadsourcedm1', 'INSTANTIATED',
        'granting-dm-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
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
    ]');

--------- Helm Charts (CRD) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcrdh7', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/eric-sec-sip-tls-crd-1.0.0tgz',
        'eric-sec-sip-tls-crd', '1.0.0', 'crd_package1', '1',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcrdh8', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/eric-sec-certm-crd-2.0.0tgz',
        'eric-sec-certm-crd', '2.0.0', 'crd_package2', '2',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', false),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcrdh9', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/scale-crd-3.0.0.tgz',
        'scale-crd', '3.0.0', 'crd_package3', '3',
        'release-name-dm-granting-crd', 'COMPLETED', 'CRD', true);

--------- Helm Charts (CNF) ---------
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_artifact_key,
priority, release_name, state, helm_chart_type, is_chart_enabled, replica_details)
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120dmcnfh7', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/spider-app1-1.0.0.tgz',
        'spider-app1', '1.0.0', 'helm_package1', '4',
        'dm-granting-upgrade-1', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter1":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter1.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd2-861d-0242ac120dmcnfh8', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/spider-app2-2.0.0.tgz',
        'spider-app2', '2.0.0', 'helm_package2', '5',
        'dm-granting-upgrade-2', 'COMPLETED', 'CNF', false, '{"eric-pm-bulk-reporter2":{"minReplicasParameterName":null,
        "minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter2.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}'),
       ('186dc69a-0c2f-crd3-861d-0242ac120dmcnfh9', '186dc69a-0c2f-11ed-861d-0242ac120dm3',
        'https://localhost/onboarded/charts/spider-app3-3.0.0.tgz',
        'spider-app3', '3.0.0', 'helm_package3', '6',
        'dm-granting-upgrade-3', 'COMPLETED', 'CNF', true, '{"eric-pm-bulk-reporter3":{"minReplicasParameterName":null,"minReplicasCount":null,
        "maxReplicasParameterName":null,"maxReplicasCount":null,"scalingParameterName":
        "eric-pm-bulk-reporter3.replicaCount","currentReplicaCount":1,"autoScalingEnabledParameterName":null,
        "autoScalingEnabledValue":false}}');

---------- Lifecycle Operations ------------
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES
('254f9ab0-5c3d-4bb5-a602-9557c1sourceopdm3', '186dc69a-0c2f-11ed-861d-0242ac120dm3', 'COMPLETED', '2024-04-27 14:37:39.276360',
 '2024-04-27 14:37:39.276360', null, 'INSTANTIATE', false,
 '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
 false, null, null, '{"eric-pm-bulk-reporter3":{"replicaCount":1}}',
 '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
 '{"eric-pm-bulk-reporter":{"replicaCount":116},"applicationTimeOut":360,"skipVerification":false}',
 'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"eric-pm-bulk-reporter3":1}',
 '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
 false, null, false, null, '360', false, false, null, null, '{}', null, 'vnfm');