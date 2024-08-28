-- Remove unused columns
-- app_vnf_instance
alter table app_vnf_instance
  drop COLUMN add_node_python_file,
  drop COLUMN delete_node_python_file,
  drop COLUMN enable_alarm_python_file,
  drop COLUMN disable_alarm_python_file;

-- app_cluster_config_file
alter table app_cluster_config_file drop COLUMN user_token;

-- Convert and rename config file
alter table app_cluster_config_file rename config_file_blob TO config_file;
alter table app_cluster_config_file alter COLUMN config_file TYPE VARCHAR USING convert_from(config_file::bytea, 'UTF8');