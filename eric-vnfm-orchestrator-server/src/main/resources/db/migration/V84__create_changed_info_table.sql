CREATE TABLE changed_info (
id VARCHAR UNIQUE,
vnf_pkg_id VARCHAR DEFAULT NULL,
vnf_instance_name VARCHAR DEFAULT NULL,
vnf_instance_description VARCHAR DEFAULT NULL,
metadata VARCHAR DEFAULT NULL,
PRIMARY KEY (id),
FOREIGN KEY (id) REFERENCES app_lifecycle_operations (operation_occurrence_id) ON DELETE CASCADE
);