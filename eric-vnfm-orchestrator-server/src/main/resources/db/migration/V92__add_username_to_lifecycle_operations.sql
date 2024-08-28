ALTER TABLE app_lifecycle_operations ADD username VARCHAR DEFAULT '';

create or replace view lifecycle_operation_view as
 select o.operation_occurrence_id,
        o.vnf_instance_id,
        o.operation_state,
        o.state_entered_time,
        o.start_time,
        o.grant_id,
        o.lifecycle_operation_type,
        o.automatic_invocation,
        o.operation_params,
        o.cancel_pending,
        o.cancel_mode,
        o.error,
        o.values_file_params,
        o.vnf_software_version,
        o.vnf_product_name,
        o.expired_application_time,
        o.combined_additional_params,
        o.combined_values_file,
        o.source_vnfd_id,
        o.target_vnfd_id,
        o.resource_details,
        o.scale_info_entities,
        o.delete_node_failed,
        o.delete_node_error_message,
        o.delete_node_finished,
        o.set_alarm_supervision_error_message,
        o.application_timeout,
        o.downsize_allowed,
        o.is_auto_rollback_allowed,
        o.rollback_failure_pattern,
        v.vnf_instance_name,
        v.cluster_name,
        v.vnf_id,
        v.namespace,
        o.instantiation_level,
        o.vnf_info_modifiable_attributes_extensions,
        o.rollback_pattern,
        o.username
from app_lifecycle_operations o left outer join app_vnf_instance v
on v.vnf_id = o.vnf_instance_id order by o.state_entered_time desc;
