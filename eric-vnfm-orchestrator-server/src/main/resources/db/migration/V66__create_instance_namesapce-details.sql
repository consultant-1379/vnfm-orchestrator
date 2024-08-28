CREATE TABLE vnfinstance_namespace_details (
  id VARCHAR UNIQUE NOT NULL,
  vnf_id VARCHAR UNIQUE NOT NULL,
  namespace VARCHAR NOT NULL,
  cluster_server VARCHAR NOT NULL,
  namespace_deletion_in_progess BOOLEAN  default FALSE
);
