ALTER TABLE app_vnf_instance ADD enable_alarm_python_file BYTEA default NULL;
ALTER TABLE app_vnf_instance ADD alarm_supervision_status VARCHAR DEFAULT NULL;
ALTER TABLE app_lifecycle_operations ADD set_alarm_supervision_error_message VARCHAR DEFAULT NULL;
