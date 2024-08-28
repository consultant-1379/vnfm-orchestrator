-- -----------------------------------------------------
-- Table `app_lifecycle_operations`
-- -----------------------------------------------------
-- Preserve instantiation_level and vnf_info_modifiable_attributes_extensions
ALTER TABLE app_lifecycle_operations
    ADD instantiation_level VARCHAR;
ALTER TABLE app_lifecycle_operations
    ADD vnf_info_modifiable_attributes_extensions VARCHAR;