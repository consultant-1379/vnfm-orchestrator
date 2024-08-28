------------Upgrade without granting rel4 package to rel4 package with mixed VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70a-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-with-mixed-vdus', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70a-0c2f-11ed-crd1-0242ac120002a', '3569c70a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70a-0c2f-11ed-crd2-0242ac120002a', '3569c70a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70a-0c2f-11ed-crd3-0242ac120002a', '3569c70a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70a-0c2f-11ed-861d-0242ac120002a', '3569c70a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-with-mixed-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('0472fusa-nmc8-mdvu-93dk-94mf845m204md', '3569c70a-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel4 package to rel4 package with non scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70b-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-with-non-scalable-vdus', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70b-0c2f-11ed-crd1-0242ac120002a', '3569c70b-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70b-0c2f-11ed-crd2-0242ac120002a', '3569c70b-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70b-0c2f-11ed-crd3-0242ac120002a', '3569c70b-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70b-0c2f-11ed-861d-0242ac120002a', '3569c70b-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-with-non-scalable-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('0472fusa-nmc8-mdvu-93dk-786324jkdkd', '3569c70b-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel3 package with mixed VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70c-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-with-mixed-vdus', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70c-0c2f-11ed-crd1-0242ac120002a', '3569c70c-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70c-0c2f-11ed-crd2-0242ac120002a', '3569c70c-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70c-0c2f-11ed-crd3-0242ac120002a', '3569c70c-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70c-0c2f-11ed-861d-0242ac120002a', '3569c70c-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-with-mixed-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('flklkjfgj94-nmc8-mdvu-93dk-jfoioiu4309', '3569c70c-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel3 package with non scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70d-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-with-non-scalable-vdus', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70d-0c2f-11ed-crd1-0242ac120002a', '3569c70d-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70d-0c2f-11ed-crd2-0242ac120002a', '3569c70d-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70d-0c2f-11ed-crd3-0242ac120002a', '3569c70d-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70d-0c2f-11ed-861d-0242ac120002a', '3569c70d-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-with-non-scalable-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('msli28slsd-nmc8-mdvu-93dk-786324jkdkd', '3569c70d-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel4 package to rel3 package with mixed VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70e-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-mixed-vdus', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70e-0c2f-11ed-crd1-0242ac120002a', '3569c70e-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70e-0c2f-11ed-crd2-0242ac120002a', '3569c70e-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70e-0c2f-11ed-crd3-0242ac120002a', '3569c70e-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70e-0c2f-11ed-861d-0242ac120002a', '3569c70e-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-mixed-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('ioosfjwe980-nmc8-mdvu-93dk-098flekje', '3569c70e-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel4 package to rel3 package with non scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70f-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-non-scalable-vdus', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70f-0c2f-11ed-crd1-0242ac120002a', '3569c70f-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70f-0c2f-11ed-crd2-0242ac120002a', '3569c70f-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70f-0c2f-11ed-crd3-0242ac120002a', '3569c70f-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70f-0c2f-11ed-861d-0242ac120002a', '3569c70f-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-non-scalable-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('oiopu349p34oiu-nmc8-mdvu-93dk-mnmbajs8793', '3569c70f-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel4 package with mixed VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70g-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-mixed-vdus', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70g-0c2f-11ed-crd1-0242ac120002a', '3569c70g-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70g-0c2f-11ed-crd2-0242ac120002a', '3569c70g-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70g-0c2f-11ed-crd3-0242ac120002a', '3569c70g-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70g-0c2f-11ed-861d-0242ac120002a', '3569c70g-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-mixed-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('ioiudfn975-nmc8-mdvu-93dk-ndy8772mdw9009', '3569c70g-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel4 package with non scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70h-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-non-scalable-vdus', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70h-0c2f-11ed-crd1-0242ac120002a', '3569c70h-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70h-0c2f-11ed-crd2-0242ac120002a', '3569c70h-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70h-0c2f-11ed-crd3-0242ac120002a', '3569c70h-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70h-0c2f-11ed-861d-0242ac120002a', '3569c70h-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-non-scalable-vdus-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('98724jkdklh-nmc8-mdvu-93dk-ncxhdjhg844', '3569c70h-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel3 package with all scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70i-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-success', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70i-0c2f-11ed-crd1-0242ac120002a', '3569c70i-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70i-0c2f-11ed-crd2-0242ac120002a', '3569c70i-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70i-0c2f-11ed-crd3-0242ac120002a', '3569c70i-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70i-0c2f-11ed-861d-0242ac120002a', '3569c70i-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-success-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('ciusf869sdf-nmc8-mdvu-93dk-096jmnbdsmiu', '3569c70i-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel4 package to rel3 package with all scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70j-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-success', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70j-0c2f-11ed-crd1-0242ac120002a', '3569c70j-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70j-0c2f-11ed-crd2-0242ac120002a', '3569c70j-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70j-0c2f-11ed-crd3-0242ac120002a', '3569c70j-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70j-0c2f-11ed-861d-0242ac120002a', '3569c70j-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel4-to-rel3-success-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('09jjdopfg-nmc8-mdvu-93dk-9ifsdfjp9df', '3569c70j-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel3 package to rel4 package with all scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70k-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-success', 'vnfInstanceDescription',
        'multi-chart-477c-aab3-2b04e6a363', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-pkgId4e6a400', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, false, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70k-0c2f-11ed-crd1-0242ac120002a', '3569c70k-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70k-0c2f-11ed-crd2-0242ac120002a', '3569c70k-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70k-0c2f-11ed-crd3-0242ac120002a', '3569c70k-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70k-0c2f-11ed-861d-0242ac120002a', '3569c70k-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-rel3-to-rel4-success-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('ahkjd090kd949-nmc8-mdvu-93dk-m720sjdp494', '3569c70k-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'multi-chart-477c-aab3-2b04e6a363', 'multi-chart-477c-aab3-2b04e6a363', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');

------------Upgrade without granting rel4 package to rel4 package with all scalable VDUs
-- CNF instance
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, is_rel4, supported_operations)
VALUES ('3569c70l-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-disabled-upgrade-success', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true, true, '[
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

-- Helm charts
-- CRD
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
VALUES ('3569c70L-0c2f-11ed-crd1-0242ac120002a', '3569c70l-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('3569c70l-0c2f-11ed-crd2-0242ac120002a', '3569c70l-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('3569c70l-0c2f-11ed-crd3-0242ac120002a', '3569c70l-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD');

-- CNF
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, replica_details)
VALUES ('3569c70l-0c2f-11ed-861d-0242ac120002a', '3569c70l-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-disabled-upgrade-success-1', 'COMPLETED',
        '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null, "minReplicasCount":null,"maxReplicasParameterName":null,
         "maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount", "currentReplicaCount":1,
         "autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}');

-- Completed Instantiate LCM operation
INSERT INTO app_lifecycle_operations (operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
cancel_mode, error, values_file_params, vnf_software_version, vnf_product_name, expired_application_time,
combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id, resource_details,
scale_info_entities, delete_node_failed, delete_node_error_message, delete_node_finished,
set_alarm_supervision_error_message, application_timeout, downsize_allowed, is_auto_rollback_allowed,
rollback_failure_pattern, instantiation_level, vnf_info_modifiable_attributes_extensions, rollback_pattern, username)
VALUES('04lklfjdf-nmc8-mdvu-93dk-jsefe78sjd', '3569c70l-0c2f-11ed-861d-0242ac120002', 'COMPLETED', '2022-10-12 11:37:54.187494',
       '2022-10-12 14:37:39.276360', null, 'INSTANTIATE', false,
       '{"additionalParams": {"applicationTimeOut":360,"skipVerification":false}}',
       false, null, null, '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112}}',
       '1.0.2s', 'basic-app-a', '2022-10-12 15:39:39.407496', '{"helmNoHooks":false,"manoControlledScaling":false,"disableOpenapiValidation":true}',
       '{"vnfc5":{"replicaCount":115},"vnfc3":{"replicaCount":113},"eric-pm-bulk-reporter":{"replicaCount":116},"vnfc1":{"replicaCount":111},"vnfc2":{"replicaCount":112},"applicationTimeOut":360,"skipVerification":false}',
       'single-chart-527c-arel4-5fcb086597zz', 'single-chart-527c-arel4-5fcb086597zz', '{"busybox":1}', '[{"scaleInfoId":"4579edf9-8f98-45c9-b5ce-a5d90e00f0df","aspectId":"Aspect1","scaleLevel":0}]',
       false, null, false, null, '360', false, false, null, 'instantiation_level_1', '{}', null, 'vnfm');
