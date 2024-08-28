-- -----------------------------------------------------
-- Table `app_vnf_instance`
-- -----------------------------------------------------
-- Preserve instantiation_level and vnf_info_modifiable_attributes_extensions
ALTER TABLE app_vnf_instance
    ADD instantiation_level VARCHAR;
ALTER TABLE app_vnf_instance
    ADD vnf_info_modifiable_attributes_extensions VARCHAR;