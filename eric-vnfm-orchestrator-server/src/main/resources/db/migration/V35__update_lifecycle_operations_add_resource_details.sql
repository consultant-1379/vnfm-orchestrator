-- Preserve resource details with scaling level to restore during downgrade operation
ALTER TABLE app_lifecycle_operations ADD resource_details VARCHAR;
ALTER TABLE app_lifecycle_operations ADD scale_info_entities VARCHAR;