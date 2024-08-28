INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             current_life_cycle_operation_id, cluster_name, namespace, combined_additional_params,  policies, resource_details)
VALUES
('sca9e-4cf4-477c-aab3-21c454e6a380', 'multiple-sca9e', 'vnfInstanceDescription',
 'd3def1ce-4cf4-477c-aab3-21cb04e6a379', 'Ericsson', 'SGSN-MME', '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08',
 '9392468011745350001', 'INSTANTIATED', 'sca9e-6a0e-443c-9b0c-4da9d9c2ab71', 'multiplesca1e', 'testscale', '{"Payload_InitialDelta.replicaCount":3,"Payload_InitialDelta_1.replicaCount":1}',
 '{"allScalingAspects":{"ScalingAspects":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Payload":{"name":"Payload", "description":"Scale level 0-29 maps to 1-30 Payload VNFC instances (1 instance per scale step)\n","max_scale_level":10,"step_deltas":["delta_1","delta_2","delta_3"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}}}}},"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Payload","deltas":{"delta_1":{"number_of_instances":4},"delta_2":{"number_of_instances":2},"delta_3":{"number_of_instances":7}}},"targets":["PL__scaled_vm","TL_scaled_vm"],"allInitialDelta":{"Payload_InitialDelta":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["PL__scaled_vm","TL_scaled_vm"]}}}}}','{"PL__scaled_vm": 9, "TL_scaled_vm": 9}');

INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
                                     error)
VALUES
('sca9e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca9e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL',
 null);

INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, helm_chart_version, helm_chart_type, priority, release_name, state, replica_details)
VALUES
('sca9e-4cf4-477c-aab3-21c454e6a382', 'sca9e-4cf4-477c-aab3-21c454e6a380',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/CRD-2.74.7.tgz', 'crd-scale-package1', '1.0.0', 'CRD', 1,
 'multiple-sca9e-1', 'COMPLETED', '{"PL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"PL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"PL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":true,"minReplicasParameterName":"PL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"PL__scaled_vm.minReplica","maxReplicasCount":3},"TL_scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"TL_scaled_vm.replicaCount"},"CL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"CL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"CL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":false,"minReplicasParameterName":"CL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"CL__scaled_vm.minReplica","maxReplicasCount":3}}'),

('sca91e-4cf4-477c-aab3-21c454e6a382', 'sca9e-4cf4-477c-aab3-21c454e6a380',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', 'cnf-scale-package1', '1.0.0', 'CNF', 2,
 'multiple-sca9e-2', 'COMPLETED', '{"PL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"PL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"PL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":true,"minReplicasParameterName":"PL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"PL__scaled_vm.minReplica","maxReplicasCount":3},"TL_scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"TL_scaled_vm.replicaCount"},"CL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"CL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"CL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":false,"minReplicasParameterName":"CL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"CL__scaled_vm.minReplica","maxReplicasCount":3}}'),

('sca92e-4cf4-477c-aab3-21c454e6a382', 'sca9e-4cf4-477c-aab3-21c454e6a380',
 'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/test-cnf-2.74.7.tgz', 'cnf-scale-package2', '1.0.0', 'CNF', 3,
 'multiple-sca9e-3', 'COMPLETED', '{"PL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"PL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"PL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":true,"minReplicasParameterName":"PL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"PL__scaled_vm.minReplica","maxReplicasCount":3},"TL_scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"TL_scaled_vm.replicaCount"},"CL__scaled_vm":{"currentReplicaCount":28,"scalingParameterName":"CL__scaled_vm.replicaCount","autoScalingEnabledParameterName":"CL__scaled_vm.autoScalingEnabled","autoScalingEnabledValue":false,"minReplicasParameterName":"CL__scaled_vm.maxReplica","minReplicasCount":1,"maxReplicasParameterName":"CL__scaled_vm.minReplica","maxReplicasCount":3}}');

INSERT INTO scale_info(scale_info_id, vnf_instance_id, aspect_id, scale_level)
VALUES
('sca9e-be55-4f0b-a989-8eb0b0071fb4', 'sca9e-4cf4-477c-aab3-21c454e6a380', 'Payload', 3);