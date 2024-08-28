-- -----------------------------------------------------
-- ENUM `change_package_operation_subtype`
-- -----------------------------------------------------
create type change_package_operation_subtype as ENUM('UPGRADE', 'DOWNGRADE');

CREATE TABLE change_package_operation_details (
  operation_occurrence_id VARCHAR NOT NULL,
  operation_subtype change_package_operation_subtype,
  target_operation_occurrence_id VARCHAR NOT NULL,
  PRIMARY KEY (operation_occurrence_id),
  FOREIGN KEY (operation_occurrence_id) REFERENCES app_lifecycle_operations (operation_occurrence_id) ON DELETE CASCADE
);