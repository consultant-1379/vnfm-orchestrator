ALTER TABLE helm_chart ADD retry_count SMALLINT DEFAULT 0;
DROP TABLE IF EXISTS APP_CLUSTER_CONFIG_FILE;
