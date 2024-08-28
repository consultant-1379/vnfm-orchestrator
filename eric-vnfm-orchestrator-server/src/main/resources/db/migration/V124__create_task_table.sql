-- -----------------------------------------------------
-- ENUM `task_name_enum`
-- -----------------------------------------------------
CREATE TYPE task_name_enum
AS ENUM ('UPDATE_PACKAGE_STATE', 'DELETE_VNF_INSTANCE', 'DELETE_NODE', 'SEND_NOTIFICATION');

-- -----------------------------------------------------
-- Table `task`
-- -----------------------------------------------------
CREATE TABLE task (
  id SERIAL UNIQUE,
  task_name task_name_enum NOT NULL,
  vnf_instance_id VARCHAR NOT NULL,
  additional_params VARCHAR,
  version BIGINT NOT NULL,
  last_update_time TIMESTAMP NOT NULL,
  priority INTEGER NOT NULL,

  PRIMARY KEY (id),
  FOREIGN KEY (vnf_instance_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE
);
