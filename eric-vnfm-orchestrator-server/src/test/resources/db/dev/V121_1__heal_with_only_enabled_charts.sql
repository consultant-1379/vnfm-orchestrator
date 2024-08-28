-- testHealOperationShouldHaveEnabledChartsOnly
INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
                             vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
                             current_life_cycle_operation_id, namespace, is_heal_supported, combined_additional_params,
--                             oss_node_protocol_file,
                             combined_values_file, supported_operations)
VALUES ('h10bcbc1-474f-4673-91ee-656fd8388888', 'heal-operation', 'vnfInstanceDescription',
        'h20bcbc1-474f-4673-91ee-656fd8388888', 'Ericsson', 'SGSN-MME',
        '1.20 (CXS104389_R81E08)', 'cxp1115898_4r81e08', '9308748274545350001', 'INSTANTIATED', 'heal-operation',
        'oo104bc1-474f-4673-91ee-656fd8366668',
        'healoperation', true,
--        '<?xml version=\"1.0\" encoding=\"UTF-8\"?> <hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">'
        '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300","commandTimeOut":"300"}',
        '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}',
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
                "operationName": "change_current_package",
                "supported": true,
                "errorMessage": null
              },
              {
                "operationName": "scale",
                "supported": true,
                "errorMessage": null
              }
            ]');

--helm chart (1)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type, is_chart_enabled)
VALUES ('hc101jo98-07e7-41d5-9324-bbb717b7777', 'h10bcbc1-474f-4673-91ee-656fd8388888',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '1',
        'heal-operation-otp-200', 'COMPLETED', 'CNF', true);

--helm chart (2)
INSERT INTO helm_chart(id, vnf_id, helm_chart_url, priority, release_name, state, helm_chart_type, is_chart_enabled)
VALUES ('hc100jo98-07e7-41d5-9324-bbb717b7777', 'h10bcbc1-474f-4673-91ee-656fd8388888',
        'https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.7.tgz', '0',
        'heal-operation-otp-200', 'COMPLETED', 'CNF', false);

--lifecycle operation - 1
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo101bc1-474f-4673-91ee-656fd8366665', 'h10bcbc1-474f-4673-91ee-656fd8388888', 'COMPLETED',
        '2020-08-03 12:12:49.823', '2020-08-03 12:13:49.823', 'INSTANTIATE', 'FALSE',
        '{"additionalParams":{"namespace": "heal-operation","applicationTimeOut":"500","commandTimeOut":"500"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');


--lifecycle operation - 2
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo102bc1-474f-4673-91ee-656fd8366667', 'h10bcbc1-474f-4673-91ee-656fd8388888', 'COMPLETED',
        '2020-08-03 12:12:49.824', '2020-08-03 12:13:49.823', 'CHANGE_PACKAGE_INFO', 'FALSE',
        '{"additionalParams":{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"500","commandTimeOut":"400"}}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');

--lifecycle operation - 3
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
                                     start_time, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending,
                                     cancel_mode, error, target_vnfd_id)
VALUES ('oo104bc1-474f-4673-91ee-656fd8366668', 'h10bcbc1-474f-4673-91ee-656fd8388888', 'PROCESSING',
        '2020-08-03 12:12:49.825', '2020-08-03 12:13:49.823', 'HEAL', 'FALSE',
        '{"cause":"latest"}', 'FALSE', 'FORCEFUL', null, 'e3def1ce-4236-477c-abb3-21c454e6a645');