-- -----------------------------------------------------
-- Drop 'not null' for vnfInstanceId
-- -----------------------------------------------------
ALTER TABLE task ALTER COLUMN vnf_instance_id DROP NOT NULL;

-- -----------------------------------------------------
-- Drop constraint to VnfInstances table
-- -----------------------------------------------------
ALTER TABLE task DROP CONSTRAINT task_vnf_instance_id_fkey;

-- -----------------------------------------------------
-- Rename column 'last_update_time' to 'perform_at_time'
-- -----------------------------------------------------
ALTER TABLE task RENAME COLUMN last_update_time TO perform_at_time;