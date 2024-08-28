-- -----------------------------------------------------
-- ENUM `app_instantiation_state`
-- -----------------------------------------------------
create type app_cluster_config_status as ENUM('NOT_IN_USE', 'IN_USE');

ALTER TABLE APP_CLUSTER_CONFIG_FILE ALTER COLUMN CONFIG_FILE_STATUS TYPE app_cluster_config_status USING config_file_status::app_cluster_config_status;

-- -----------------------------------------------------
-- Table `cluster_config_instances`
-- -----------------------------------------------------

CREATE table cluster_config_instances (
  id SERIAL UNIQUE,
  CONFIG_FILE_NAME VARCHAR NOT NULL,
  instance_id VARCHAR UNIQUE
);

ALTER TABLE cluster_config_instances DROP CONSTRAINT cluster_config_instances_id_key;
