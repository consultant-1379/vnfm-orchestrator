CREATE OR REPLACE FUNCTION updateOperationsInProgress() RETURNS TRIGGER AS $update_operations_in_progress$
  BEGIN
    raise notice 'Updating OperationsInProgress for %', OLD.vnf_instance_id;
    IF (NEW.operation_state = 'COMPLETED' OR NEW.operation_state = 'FAILED' OR NEW.operation_state = 'ROLLED_BACK') THEN
      DELETE FROM operations_in_progress WHERE vnf_id = OLD.vnf_instance_id;
    END IF;
    RETURN NEW;
   END;
$update_operations_in_progress$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_operations_in_progress ON app_lifecycle_operations;

CREATE TRIGGER update_operations_in_progress AFTER UPDATE ON app_lifecycle_operations
FOR EACH ROW EXECUTE PROCEDURE updateOperationsInProgress();
