UPDATE helm_chart SET replica_details = '{"eric-pm-bulk-reporter":{"minReplicasParameterName":null,
"minReplicasCount":null,"maxReplicasParameterName":null,
"maxReplicasCount":null,"scalingParameterName":"eric-pm-bulk-reporter.replicaCount",
"currentReplicaCount":1,"autoScalingEnabledParameterName":null,"autoScalingEnabledValue":false}}'
WHERE vnf_id IN ('186dccff-0c2f-11ed-861d-0242ac120002', '186dc69a-0c2f-11ed-861d-0242ac120002', '186dc8db-0c2f-11ed-861d-0242ac120002',
'186dc3fd-0c2f-11ed-861d-0242ac120002', '186dc6de-0c2f-11ed-861d-0242ac120002', '186dc9fc-0c2f-11ed-861d-0242ac120002',
'186dc69a-0c2f-11ed-861d-0242ac120003', '186dc69a-0c2f-11ed-861d-0242ac120004', '186dc69a-0c2f-11ed-861d-0242ac120005',
'186dc69a-0c2f-11ed-861d-0242ac120006', '186dc69a-0c2f-11ed-861d-0242ac120007', '186dc69a-0c2f-11ed-861d-0242ac120008',
'g3def1ce-4cf4-477c-aab3-21c454e6a396', 'g3def1ce-4cf4-477c-aab3-21c454e6a397', 'g3def1ce-4cf4-477c-aab3-21c454e6a398',
'g3def1ce-4cf4-477c-aab3-21c454e6a394', 'g3def1ce-4cf4-477c-aab3-21c454e6a393', 'g3def1ce-4cf4-477c-aab3-21c454e6a395',
'g3def1ce-4cf4-477c-aab3-21c454e6a400', 'g3def1ce-4cf4-477c-aab3-21c454e6a777')
AND helm_chart_type = 'CNF';

--// '186dc9fc-0c2f-11ed-861d-0242ac120002', '186dc6de-0c2f-11ed-861d-0242ac120002' spider-app-2.74.7.tgz