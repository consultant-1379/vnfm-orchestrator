-- -----------------------------------------------------
-- Table `app_cluster_config_file`
-- -----------------------------------------------------
alter table app_cluster_config_file
	add cluster_server varchar;

alter table app_cluster_config_file
	add user_token varchar;

alter table app_cluster_config_file
          	add constraint app_cluster_config_file_server_and_token
          		unique (cluster_server, user_token);
