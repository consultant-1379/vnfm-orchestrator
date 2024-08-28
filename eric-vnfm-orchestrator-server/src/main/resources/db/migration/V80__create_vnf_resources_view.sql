CREATE VIEW vnf_resources_view AS SELECT vnf.*, op.state_entered_time AS last_state_changed,
                                         op.lifecycle_operation_type AS lifecycle_operation_type,
                                         op.operation_state AS operation_state
  FROM app_vnf_instance vnf
  LEFT OUTER JOIN app_lifecycle_operations op ON vnf.vnf_id = op.vnf_instance_id AND vnf.current_life_cycle_operation_id = op.operation_occurrence_id;
