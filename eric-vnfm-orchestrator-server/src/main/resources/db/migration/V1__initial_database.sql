-- -----------------------------------------------------
-- ENUM `app_instantiation_state`
-- -----------------------------------------------------
create type app_instantiation_state as ENUM('INSTANTIATED', 'NOT_INSTANTIATED');

-- -----------------------------------------------------
-- ENUM `app_lcm_operation_state`
-- -----------------------------------------------------
create type app_lcm_operation_state as ENUM('STARTING', 'PROCESSING', 'COMPLETED', 'FAILED_TEMP', 'FAILED',
'ROLLING_BACK', 'ROLLED_BACK');

-- -----------------------------------------------------
-- ENUM `app_lcm_operation_type`
-- -----------------------------------------------------
create type app_lcm_operation_type as ENUM('INSTANTIATE', 'SCALE', 'SCALE_TO_LEVEL', 'CHANGE_FLAVOUR', 'TERMINATE',
'HEAL', 'OPERATE', 'CHANGE_EXT_CONN', 'MODIFY_INFO');

-- -----------------------------------------------------
-- ENUM `app_cancel_mode_type`
-- -----------------------------------------------------
create type app_cancel_mode_type as ENUM('GRACEFUL', 'FORCEFUL');


-- -----------------------------------------------------
-- Table `app_vnf_instance`
-- -----------------------------------------------------

CREATE TABLE app_vnf_instance (
  vnf_id VARCHAR UNIQUE NOT NULL,
  vnf_instance_name VARCHAR DEFAULT NULL,
  vnf_instance_description VARCHAR DEFAULT NULL,
  vnfd_id VARCHAR NOT NULL,
  vnf_provider VARCHAR NOT NULL,
  vnf_product_name VARCHAR NOT NULL,
  vnf_software_version VARCHAR NOT NULL,
  vnfd_version VARCHAR NOT NULL,
  vnf_pkg_id VARCHAR NOT NULL,
  instantiation_state app_instantiation_state,
  cluster_name VARCHAR DEFAULT NULL,
  helm_chart_url VARCHAR DEFAULT NULL,
  namespace VARCHAR DEFAULT NULL,
  current_life_cycle_operation_id VARCHAR DEFAULT NULL
);

-- -----------------------------------------------------
-- Table `app_lifecycle_operations`
-- -----------------------------------------------------

CREATE TABLE app_lifecycle_operations (
  operation_occurrence_id VARCHAR UNIQUE NOT NULL,
  vnf_instance_id VARCHAR NOT NULL,
  operation_state app_lcm_operation_state,
  state_entered_time TIMESTAMP NOT NULL,
  start_time TIMESTAMP NOT NULL,
  grant_id VARCHAR DEFAULT NULL,
  lifecycle_operation_type app_lcm_operation_type,
  automatic_invocation BOOLEAN DEFAULT FALSE,
  operation_params VARCHAR DEFAULT NULL,
  cancel_pending BOOLEAN DEFAULT FALSE,
  cancel_mode app_cancel_mode_type,
  error VARCHAR DEFAULT NULL,
  FOREIGN KEY (vnf_instance_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
