CREATE TABLE scale_info (
  scale_info_id VARCHAR UNIQUE NOT NULL,
  vnf_instance_id VARCHAR NOT NULL,
  aspect_id VARCHAR NOT NULL,
  scale_level SMALLINT NOT NULL,
  FOREIGN KEY (vnf_instance_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE
);