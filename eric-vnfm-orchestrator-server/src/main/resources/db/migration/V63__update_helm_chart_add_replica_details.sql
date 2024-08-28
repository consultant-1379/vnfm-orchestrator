-- -----------------------------------------------------
-- Table `helm_chart & helm_chart_history`
-- -----------------------------------------------------
-- Preserve replica_details
ALTER TABLE helm_chart
    ADD replica_details VARCHAR;
ALTER TABLE helm_chart_history
    ADD replica_details VARCHAR;