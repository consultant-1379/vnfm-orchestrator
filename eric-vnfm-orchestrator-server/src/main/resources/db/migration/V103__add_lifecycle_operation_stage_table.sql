-- -----------------------------------------------------
-- Table `lifecycle_operation_stage`
-- -----------------------------------------------------

CREATE TABLE lifecycle_operation_stage
(
    operation_id VARCHAR UNIQUE NOT NULL,
    owner        VARCHAR        NOT NULL,
    checkpoint   VARCHAR        NOT NULL,
    owned_since  TIMESTAMP      NOT NULL,
    PRIMARY KEY (operation_id),
    FOREIGN KEY (operation_id) REFERENCES app_lifecycle_operations (operation_occurrence_id) ON DELETE CASCADE
);