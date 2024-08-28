UPDATE app_vnf_instance SET policies = '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},
"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}'
WHERE vnf_id in ('g3def1ce-4cf4-477c-aab3-21c454e6a390', 'g3def1ce-4cf4-477c-aab3-21c454e6a391',
'g3def1ce-4cf4-477c-aab3-21c454e6a392', 'g3def1ce-4cf4-477c-aab3-21c454e6a399');

UPDATE app_vnf_instance SET instantiation_level = 'instantiation_level_1'
WHERE vnf_id in ('g3def1ce-4cf4-477c-aab3-21c454e6a396', 'g3def1ce-4cf4-477c-aab3-21c454e6a397',
'g3def1ce-4cf4-477c-aab3-21c454e6a398', 'g3def1ce-4cf4-477c-aab3-21c454e6a400', 'g3def1ce-4cf4-477c-aab3-21c454e6a393',
'g3def1ce-4cf4-477c-aab3-21c454e6a394', 'g3def1ce-4cf4-477c-aab3-21c454e6a395', '186dc69a-0c2f-11ed-861d-0242ac120002',
'186dc8db-0c2f-11ed-861d-0242ac120002', '186dc9fc-0c2f-11ed-861d-0242ac120002', '186dc3fd-0c2f-11ed-861d-0242ac120002',
'186dc6de-0c2f-11ed-861d-0242ac120002', '186dccff-0c2f-11ed-861d-0242ac120002',
'186dc69a-0c2f-11ed-861d-0242ac120003', '186dc69a-0c2f-11ed-861d-0242ac120004', '186dc69a-0c2f-11ed-861d-0242ac120005',
'186dc69a-0c2f-11ed-861d-0242ac120006', '186dc69a-0c2f-11ed-861d-0242ac120007', '186dc69a-0c2f-11ed-861d-0242ac120008');

UPDATE helm_chart SET replica_details = '{"eric-pm-bulk-reporter":{"minReplicasParameterName":"eric-pm-bulk-reporter.autoscaling.minReplicas","minReplicasCount":null,
        "maxReplicasParameterName":"eric-pm-bulk-reporter.autoscaling.maxReplicas","maxReplicasCount":null,
        "scalingParameterName":"eric-pm-bulk-reporter.replicaCount","currentReplicaCount":1,
        "autoScalingEnabledParameterName":"eric-pm-bulk-reporter.autoscaling.engine.enabled","autoScalingEnabledValue":false}}'
WHERE id = '186dc69a-0c2f-11ed-861d-0242ac120002a';