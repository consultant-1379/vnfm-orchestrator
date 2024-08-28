CREATE TABLE helm_chart_history(
    id UUID NOT NULL,
    vnf_id VARCHAR NOT NULL,
    helm_chart_url VARCHAR NOT NULL,
    priority INT NOT NULL,
    release_name VARCHAR NOT NULL,
    state VARCHAR default null,
    revision_number VARCHAR DEFAULT NULL,
    retry_count SMALLINT DEFAULT 0,
    life_cycle_operation_id VARCHAR NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (vnf_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE,
    FOREIGN KEY (life_cycle_operation_id) REFERENCES app_lifecycle_operations (operation_occurrence_id) ON DELETE CASCADE
);

ALTER TABLE app_lifecycle_operations ADD combined_additional_params VARCHAR;
ALTER TABLE app_lifecycle_operations ADD combined_values_file VARCHAR;
ALTER TABLE app_lifecycle_operations ADD source_vnfd_id VARCHAR;
ALTER TABLE app_lifecycle_operations ADD target_vnfd_id VARCHAR;
