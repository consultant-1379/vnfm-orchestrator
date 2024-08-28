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
    VALUES ('97ba1047-01b8-4536-bb95-9f8d7e3797ab', 'from-delete_identifier', 'Failed instantiate',
            '35803ca6-53cd-4a2f-8df3-971676fecf3a', 'Ericsson', 'Test CNF', '1.0',
            '2.0', '4f06fcc6-9979-4731-89be-926bb028f600', 'NOT_INSTANTIATED', 'test-cluster',
            'namespace', '7e125210-7280-4807-8ce6-49729f5ec72c', NULL, NULL, NULL, FALSE,
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
    VALUES ('7e125210-7280-4807-8ce6-49729f5ec72k', '97ba1047-01b8-4536-bb95-9f8d7e3797ab', 'FAILED',
            '2021-05-25 10:12:49.823', '2021-05-25 10:12:49.800', NULL, 'INSTANTIATE', FALSE, NULL,
            FALSE, 'FORCEFUL', NULL, NULL, '1.0', 'Test CNF', NULL, NULL, NULL, NULL,
            '35803ca6-53cd-4a2f-8df3-971676fecf3a', '35803ca6-53cd-4a2f-8df3-971676fecf3a', FALSE,
            NULL, FALSE, '500', '2021-05-25 10:22:49.800', NULL, FALSE, FALSE, NULL);

INSERT INTO vnfinstance_namespace_details(id, vnf_id, namespace, cluster_server, namespace_deletion_in_progess)
 VALUES
('38', '97ba1047-01b8-4536-bb95-9f8d7e3797ab', 'test-namespace', 'test-cluster', false);