CREATE TABLE terminated_helm_chart (
  id VARCHAR UNIQUE NOT NULL,
  vnf_id VARCHAR NOT NULL,
  helm_chart_url VARCHAR NOT NULL,
  priority INT NOT NULL,
  release_name VARCHAR DEFAULT NULL,
  revision_number VARCHAR DEFAULT NULL,
  state VARCHAR DEFAULT NULL,
  retry_count SMALLINT DEFAULT 0,
  delete_pvc_state VARCHAR DEFAULT NULL,
  downsize_state VARCHAR DEFAULT NULL,
  replica_details VARCHAR,
  is_chart_enabled BOOLEAN DEFAULT true,
  helm_chart_name VARCHAR DEFAULT '',
  helm_chart_version VARCHAR DEFAULT '',
  helm_chart_type chart_type_enum DEFAULT 'CNF',
  helm_chart_artifact_key VARCHAR DEFAULT null,
  life_cycle_operation_id VARCHAR DEFAULT NULL,

  FOREIGN KEY (vnf_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE
);