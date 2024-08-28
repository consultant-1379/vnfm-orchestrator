INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state,
                             cluster_name, added_to_oss, policies, is_heal_supported, supported_operations)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-upgrade-success', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
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
    ]'),
       ('186dc8db-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-upgrade-failed-forbidden', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
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
    ]'),
       ('186dc9fc-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-downgrade-success', 'vnfInstanceDescription',
        'single-chart-c-rel4-545379754e30', 'Ericsson', 'SGSN-MME', '1.0.22.0s', '1.0.22.0', '43bf1225-81e2-46b4-rel42-cadea4432939',
        'INSTANTIATED', 'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true,
        '[
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
    ]'),
       ('186dc3fd-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-upgrade-failed-unavailable', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
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
    ]'),
       ('186dc6de-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-downgrade-failed-forbidden', 'vnfInstanceDescription',
        'single-chart-c-rel4-545379754e30', 'Ericsson', 'SGSN-MME', '1.0.22.0s', '1.0.22.0', '43bf1225-81e2-46b4-rel42-cadea4432939',
        'INSTANTIATED', 'granting-change-vnfpkg', false,
        '{"allScalingAspects":{"ScalingAspects1":{"type":"tosca.policies.nfv.ScalingAspects","properties":{"aspects":{"Aspect1":{"name":"Aspect1","description":"Scale level 0-10 maps to 1-11 for eric-pm-bulk-reporter\n","max_scale_level":10,"step_deltas":["delta_1"],"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}}}}}}},"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}},"allScalingAspectDelta":{"Payload_ScalingAspectDeltas1":{"type":"tosca.policies.nfv.VduScalingAspectDeltas","properties":{"aspect":"Aspect1","deltas":{"delta_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"],"allInitialDelta":{"eric-pm-bulk-reporter":{"type":"tosca.policies.nfv.VduInitialDelta","properties":{"initial_delta":{"number_of_instances":1}},"targets":["eric-pm-bulk-reporter"]}}}},"allVnfPackageChangePolicy":{},"allVduInstantiationLevels":{"vdu_1_instantiation_levels":{"type":"tosca.policies.nfv.VduInstantiationLevels","properties":{"instantiationLevels":{"instantiation_level_1":{"number_of_instances":1}},"levels":{"instantiation_level_1":{"number_of_instances":1}}},"targets":["eric-pm-bulk-reporter"]}},"allInstantiationLevels":{"InstantiationLevels":{"type":"tosca.policies.nfv.InstantiationLevels","properties":{"levels":{"instantiation_level_1":{"scale_info":{"Aspect1":{"scale_level":0}},"description":"eric-pm-bulk-reporter"}},"default_level":"instantiation_level_1"}}}}',
        true,
        '[
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
    ]'),
       ('186dccff-0c2f-11ed-861d-0242ac120002', 'my-release-name-granting-upgrade-failed-forbidden-no-rollback', 'vnfInstanceDescription',
        'single-chart-527c-arel4-5fcb086597zs', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '43bf1225-81e1-46b4-rel41-cadea4432939', 'INSTANTIATED',
        'granting-change-vnfpkg', false,
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

--CNF_charts
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state)
VALUES ('186dc69a-0c2f-11ed-861d-0242ac120002a', '186dc69a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-upgrade-success-1', 'COMPLETED'),
       ('186dc8db-0c2f-11ed-861d-0242ac120002b', '186dc8db-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-upgrade-failed-forbidden-1', 'COMPLETED'),
       ('186dc9fc-0c2f-11ed-861d-0242ac120002c', '186dc9fc-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-downgrade-success-1', 'COMPLETED'),
       ('186dc3fd-0c2f-11ed-861d-0242ac120002d', '186dc3fd-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-upgrade-failed-unavailable-1', 'COMPLETED'),
       ('186dc6de-0c2f-11ed-861d-0242ac120002e', '186dc6de-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-downgrade-failed-forbidden-1', 'COMPLETED'),
       ('186dccff-0c2f-11ed-861d-0242ac120002f', '186dccff-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/spider-app-2.74.7.tgz', 'spider-app-2.74.7',
        '4', 'my-release-name-granting-upgrade-failed-forbidden-no-rollback-1', 'COMPLETED');

--CRD_charts
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, helm_chart_name, priority, release_name, state, helm_chart_type)
--186dc69a-0c2f-11ed-861d-0242ac120002 success
VALUES ('186dc69a-0c2f-crd1-861d-0242ac120002a', '186dc69a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd',
        '1', 'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dc69a-0c2f-crd2-861d-0242ac120002a', '186dc69a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd',
        '2', 'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dc69a-0c2f-crd3-861d-0242ac120002a', '186dc69a-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd',
        '3', 'scale-crd', 'COMPLETED', 'CRD'),
--186dc8db-0c2f-11ed-861d-0242ac120002 failure
       ('186dc8db-0c2f-11ed-crd1-0242ac120002b', '186dc8db-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd', '1',
            'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dc8db-0c2f-11ed-crd2-0242ac120002b', '186dc8db-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd', '2',
            'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dc8db-0c2f-11ed-crd3-0242ac120002b', '186dc8db-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd', '3',
            'scale-crd', 'COMPLETED', 'CRD'),
--186dc9fc-0c2f-11ed-861d-0242ac120002 downgrade
       ('186dc9fc-0c2f-11ed-crd1-0242ac120002c', '186dc9fc-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd', '1',
            'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dc9fc-0c2f-11ed-crd2-0242ac120002c', '186dc9fc-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd', '2',
            'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dc9fc-0c2f-11ed-crd3-0242ac120002c', '186dc9fc-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd', '3',
            'scale-crd', 'COMPLETED', 'CRD'),
--186dc3fd-0c2f-11ed-861d-0242ac120002 unavailable
       ('186dc3fd-0c2f-11ed-crd1-0242ac120002d', '186dc3fd-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd', '1',
        'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dc3fd-0c2f-11ed-crd2-0242ac120002d', '186dc3fd-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd', '2',
        'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dc3fd-0c2f-11ed-crd3-0242ac120002d', '186dc3fd-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd', '3',
        'scale-crd', 'COMPLETED', 'CRD'),
--186dc6de-0c2f-11ed-861d-0242ac120002 downgrade failed forbidden
       ('186dc6de-0c2f-11ed-crd1-0242ac120002e', '186dc6de-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd', '1',
        'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dc6de-0c2f-11ed-crd2-0242ac120002e', '186dc6de-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd', '2',
        'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dc6de-0c2f-11ed-crd3-0242ac120002e', '186dc6de-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd', '3',
        'scale-crd', 'COMPLETED', 'CRD'),
--186dccff-0c2f-11ed-861d-0242ac120002 failed forbidden no rollback
       ('186dccff-0c2f-11ed-crd1-0242ac120002f', '186dccff-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-sip-tls-crd-2.3.0+32.tgz', 'eric-sec-sip-tls-crd', '1',
        'eric-sec-sip-tls-crd', 'COMPLETED', 'CRD'),
       ('186dccff-0c2f-11ed-crd2-0242ac120002f', '186dccff-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/eric-sec-certm-crd-3.2.1+35.tgz', 'eric-sec-certm-crd', '2',
        'eric-sec-certm-crd', 'COMPLETED', 'CRD'),
       ('186dccff-0c2f-11ed-crd3-0242ac120002f', '186dccff-0c2f-11ed-861d-0242ac120002',
        'https://helm-chart-registry/onboarded/charts/scale-crd-1.0.3.tgz', 'scale-crd', '3',
        'scale-crd', 'COMPLETED', 'CRD');