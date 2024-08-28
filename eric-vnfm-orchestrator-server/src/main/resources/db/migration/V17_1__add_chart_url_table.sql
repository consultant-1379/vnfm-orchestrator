CREATE TABLE helm_chart (
  id VARCHAR UNIQUE NOT NULL,
  vnf_id VARCHAR NOT NULL,
  helm_chart_url VARCHAR NOT NULL,
  priority INT NOT NULL,
  FOREIGN KEY (vnf_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE
);
