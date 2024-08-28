INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error)
VALUES
('sca7e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca1e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_IN", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null),

('sca8e-6a0e-443c-9b0c-4da9d9c2ab71', 'sca1e-4cf4-477c-aab3-21c454e6a380', 'PROCESSING', CURRENT_TIMESTAMP,
CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SCALE', 'FALSE', '{"type":"SCALE_OUT", "aspectId": "Payload", "numberOfSteps": "1"}', 'FALSE','FORCEFUL', null)
;

UPDATE helm_chart SET state = null WHERE id = 'sca21e-4cf4-477c-aab3-21c454e6a382';