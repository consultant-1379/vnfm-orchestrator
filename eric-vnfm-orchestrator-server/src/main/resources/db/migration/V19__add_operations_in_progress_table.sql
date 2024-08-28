CREATE TABLE operations_in_progress (
  id VARCHAR UNIQUE NOT NULL,
  vnf_id VARCHAR UNIQUE NOT NULL,
  lifecycle_operation_type app_lcm_operation_type NOT NULL
);

CREATE OR REPLACE FUNCTION updateOperationsInProgress() RETURNS TRIGGER AS $update_operations_in_progress$
  BEGIN
    raise notice 'Updating OperationsInProgress for %', OLD.vnf_instance_id;
    IF (NEW.operation_state = 'COMPLETED' OR NEW.operation_state = 'FAILED') THEN
      DELETE FROM operations_in_progress WHERE vnf_id = OLD.vnf_instance_id;
    END IF;
    RETURN NEW;
   END;
$update_operations_in_progress$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_operations_in_progress ON app_lifecycle_operations;

CREATE TRIGGER update_operations_in_progress AFTER UPDATE ON app_lifecycle_operations
FOR EACH ROW EXECUTE PROCEDURE updateOperationsInProgress();
