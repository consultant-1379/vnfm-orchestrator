CREATE TABLE granting_vdu_info (

id VARCHAR UNIQUE,
vnf_id VARCHAR NOT NULL,
vdu VARCHAR DEFAULT NULL,

PRIMARY KEY (id),
FOREIGN KEY (vnf_id) REFERENCES app_vnf_instance (vnf_id) ON DELETE CASCADE );

CREATE TABLE granting_oscontainer_info (

id VARCHAR UNIQUE,
vdu_id VARCHAR NOT NULL,
os_container VARCHAR DEFAULT NULL,

PRIMARY KEY (id),

FOREIGN KEY (vdu_id) REFERENCES granting_vdu_info (id) ON DELETE CASCADE );

CREATE TABLE granting_virtual_block_storage_info (

id VARCHAR UNIQUE,
vdu_id VARCHAR NOT NULL,
virtual_block_storage VARCHAR DEFAULT NULL,

PRIMARY KEY (id),

FOREIGN KEY (vdu_id) REFERENCES granting_vdu_info (id) ON DELETE CASCADE );