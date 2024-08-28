INSERT INTO app_vnf_instance (
        vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
        vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id,
        instantiation_state, cluster_name, namespace, current_life_cycle_operation_id,
        oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
        combined_values_file, combined_additional_params, policies, resource_details,
        mano_controlled_scaling, temp_instance, override_global_registry, metadata,
        alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file,
        oss_node_protocol_file, sensitive_info, bro_endpoint_url,
        vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, supported_operations)
    VALUES ('97ba1047-01b8-4536-bb95-9f8d7e3797fc', 'paged-not_instantiated', 'Failed instantiate',
            '35803ca6-53cd-4a2f-8df3-971676fecf3a', 'Ericsson', 'Test CNF', '1.0',
            '2.0', '4f06fcc6-9979-4731-89be-926bb028f600', 'NOT_INSTANTIATED', 'paged-cluster',
            'paged-namespace', '7e125210-7280-4807-8ce6-49729f5ec72c', NULL, NULL, NULL, FALSE,
            NULL, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, NULL, TRUE, FALSE, NULL, NULL,
            NULL, NULL, NULL, NULL, 'eric-crd', '[
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

INSERT INTO app_lifecycle_operations (
        operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
        start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params,
        cancel_pending, cancel_mode, error, values_file_params, vnf_software_version,
        vnf_product_name, combined_values_file, combined_additional_params, resource_details,
        scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed,
        delete_node_error_message, delete_node_finished, application_timeout,
        expired_application_time, set_alarm_supervision_error_message,
        downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern)
    VALUES ('7e125210-7280-4807-8ce6-49729f5ec72c', '97ba1047-01b8-4536-bb95-9f8d7e3797fc', 'FAILED',
            '2021-05-25 10:12:49.823', '2021-05-25 10:12:49.800', NULL, 'INSTANTIATE', FALSE, NULL,
            FALSE, 'FORCEFUL', NULL, NULL, '1.0', 'Test CNF', NULL, NULL, NULL, NULL,
            '35803ca6-53cd-4a2f-8df3-971676fecf3a', '35803ca6-53cd-4a2f-8df3-971676fecf3a', FALSE,
            NULL, FALSE, '500', '2021-05-25 10:22:49.800', NULL, FALSE, FALSE, NULL);

INSERT INTO app_vnf_instance (
    vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
    vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id,
    instantiation_state, cluster_name, namespace, current_life_cycle_operation_id,
    oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
    combined_values_file, combined_additional_params, policies, resource_details,
    mano_controlled_scaling, temp_instance, override_global_registry, metadata,
    alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file,
    oss_node_protocol_file, sensitive_info, bro_endpoint_url,
    vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, supported_operations)
VALUES ('0f1eb86e-b31d-48bb-833f-741106bb1884', 'paged-instantiated', 'Completed instantiate',
        '35803ca6-53cd-4a2f-8df3-971676fecf3a', 'Ericsson', 'Test CNF', '1.0',
        '2.0', '4f06fcc6-9979-4731-89be-926bb028f600', 'INSTANTIATED', 'paged-cluster',
        'paged-namespace', '834a9b28-4351-48a7-b0f3-65d095f97f3f', NULL, NULL, NULL, FALSE,
        NULL, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, NULL, TRUE, FALSE, NULL, NULL,
        NULL, NULL, NULL, NULL, 'eric-crd', '[
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

INSERT INTO app_lifecycle_operations (
    operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
    start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params,
    cancel_pending, cancel_mode, error, values_file_params, vnf_software_version,
    vnf_product_name, combined_values_file, combined_additional_params, resource_details,
    scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed,
    delete_node_error_message, delete_node_finished, application_timeout,
    expired_application_time, set_alarm_supervision_error_message,
    downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern)
VALUES ('834a9b28-4351-48a7-b0f3-65d095f97f3f', '0f1eb86e-b31d-48bb-833f-741106bb1884', 'COMPLETED',
        '2021-05-25 10:18:49.823', '2021-05-25 10:18:47.800', NULL, 'INSTANTIATE', FALSE, NULL,
        FALSE, 'FORCEFUL', NULL, NULL, '1.0', 'Test CNF', NULL, NULL, NULL, NULL,
        '35803ca6-53cd-4a2f-8df3-971676fecf3a', '35803ca6-53cd-4a2f-8df3-971676fecf3a', FALSE,
        NULL, FALSE, '500', '2021-05-25 10:32:49.800', NULL, FALSE, FALSE, NULL);

INSERT INTO app_vnf_instance (
    vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
    vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id,
    instantiation_state, cluster_name, namespace, current_life_cycle_operation_id,
    oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
    combined_values_file, combined_additional_params, policies, resource_details,
    mano_controlled_scaling, temp_instance, override_global_registry, metadata,
    alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file,
    oss_node_protocol_file, sensitive_info, bro_endpoint_url,
    vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, supported_operations)
VALUES ('b0a33436-1609-476c-83cd-ec474e5a828b', 'paged-upgraded', 'Completed upgrade',
        '4de429b6-f432-4e58-bd5d-27dbc2ffcae6', 'Ericsson', 'Test CNF', '1.0',
        '2.0', '4f06fcc6-9979-4731-89be-926bb028f600', 'INSTANTIATED', 'paged-cluster',
        'paged-namespace', '468d35c5-7ab3-41d8-a5e0-b3ca5028bc4c', NULL, NULL, NULL, FALSE,
        NULL, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, NULL, TRUE, FALSE, NULL, NULL,
        NULL, NULL, NULL, NULL, 'eric-crd', '[
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

INSERT INTO app_lifecycle_operations (
    operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
    start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params,
    cancel_pending, cancel_mode, error, values_file_params, vnf_software_version,
    vnf_product_name, combined_values_file, combined_additional_params, resource_details,
    scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed,
    delete_node_error_message, delete_node_finished, application_timeout,
    expired_application_time, set_alarm_supervision_error_message,
    downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern)
VALUES ('3c712991-84c8-47e7-83f9-a7a3e97eae75', 'b0a33436-1609-476c-83cd-ec474e5a828b', 'COMPLETED',
        '2021-05-25 10:18:49.823', '2021-05-25 10:18:47.800', NULL, 'INSTANTIATE', FALSE, NULL,
        FALSE, 'FORCEFUL', NULL, NULL, '1.0', 'Test CNF', NULL, NULL, NULL, NULL,
        '35803ca6-53cd-4a2f-8df3-971676fecf3a', '35803ca6-53cd-4a2f-8df3-971676fecf3a', FALSE,
        NULL, FALSE, '500', '2021-05-25 10:32:49.800', NULL, FALSE, FALSE, NULL);

INSERT INTO app_lifecycle_operations (
    operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
    start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params,
    cancel_pending, cancel_mode, error, values_file_params, vnf_software_version,
    vnf_product_name, combined_values_file, combined_additional_params, resource_details,
    scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed,
    delete_node_error_message, delete_node_finished, application_timeout,
    expired_application_time, set_alarm_supervision_error_message,
    downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern)
VALUES ('468d35c5-7ab3-41d8-a5e0-b3ca5028bc4c', 'b0a33436-1609-476c-83cd-ec474e5a828b', 'COMPLETED',
        '2021-05-25 10:41:49.823', '2021-05-25 10:38:47.800', NULL, 'MODIFY_INFO', FALSE, NULL,
        FALSE, 'FORCEFUL', NULL, NULL, '1.0', 'Test CNF', NULL, NULL, NULL, NULL,
        '35803ca6-53cd-4a2f-8df3-971676fecf3a', '4de429b6-f432-4e58-bd5d-27dbc2ffcae6', FALSE,
        NULL, FALSE, '500', '2021-05-25 10:32:49.800', NULL, FALSE, FALSE, NULL);

INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
    VALUES ('468d35c5-7ab3-41d8-a5e0-b3ca5028bc4c', 'UPGRADE', NULL);

INSERT INTO app_vnf_instance (
    vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
    vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id,
    instantiation_state, cluster_name, namespace, current_life_cycle_operation_id,
    oss_topology, instantiate_oss_topology, add_node_oss_topology, added_to_oss,
    combined_values_file, combined_additional_params, policies, resource_details,
    mano_controlled_scaling, temp_instance, override_global_registry, metadata,
    alarm_supervision_status, clean_up_resources, is_heal_supported, sitebasic_file,
    oss_node_protocol_file, sensitive_info, bro_endpoint_url,
    vnf_info_modifiable_attributes_extensions, instantiation_level, crd_namespace, supported_operations)
VALUES ('b6f2ffb4-d1f3-4c01-afb8-7791a77150f6', 'instance_with_only_modify_info', 'this instance should be filtered',
        'multi-chart-etsi-rel4-5fcb086597', 'Ericsson', 'spider-app-multi-a-etsi-tosca-rel4', '1.0.34s',
        '1.0.34', 'fa424fef-2a91-467b-9775-c36cbba6c29e', 'NOT_INSTANTIATED', NULL,
        NULL, 'd2cd692d-eaf4-4ca6-8156-3b8d2a5a89fd', NULL, NULL, NULL, FALSE,
        NULL, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, NULL, TRUE, FALSE, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, '[
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

INSERT INTO app_lifecycle_operations (
    operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
    start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params,
    cancel_pending, cancel_mode, error, values_file_params, vnf_software_version,
    vnf_product_name, combined_values_file, combined_additional_params, resource_details,
    scale_info_entities, source_vnfd_id, target_vnfd_id, delete_node_failed,
    delete_node_error_message, delete_node_finished, application_timeout,
    expired_application_time, set_alarm_supervision_error_message,
    downsize_allowed, is_auto_rollback_allowed, rollback_failure_pattern)
VALUES ('d2cd692d-eaf4-4ca6-8156-3b8d2a5a89fd', 'b6f2ffb4-d1f3-4c01-afb8-7791a77150f6', 'COMPLETED',
        '2024-01-24 8:52:13.823', '2024-01-24 8:52:12.823', NULL, 'MODIFY_INFO', FALSE, NULL,
        FALSE, NULL, NULL, NULL, '1.0.34s', 'spider-app-multi-a-etsi-tosca-rel4', NULL, NULL, NULL, NULL,
        'multi-chart-etsi-rel4-5fcb086597', NUll, FALSE,
        NULL, FALSE, '3600', '2024-01-24 9:54:12.800', NULL, FALSE, FALSE, NULL);