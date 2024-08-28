ALTER TABLE app_lifecycle_operations ADD delete_node_failed BOOLEAN DEFAULT FALSE;
ALTER TABLE app_lifecycle_operations ADD delete_node_error_message VARCHAR DEFAULT NULL;
ALTER TABLE app_lifecycle_operations ADD delete_node_finished BOOLEAN DEFAULT FALSE;