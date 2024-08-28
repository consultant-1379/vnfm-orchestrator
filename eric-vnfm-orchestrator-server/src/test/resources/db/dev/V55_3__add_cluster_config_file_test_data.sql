UPDATE app_lifecycle_operations SET expired_application_time = CURRENT_TIMESTAMP WHERE operation_occurrence_id = 'm08fcbc8-474f-4673-91ee-761fd83991e6';

UPDATE app_vnf_instance SET cluster_name = 'multiplesca1e' WHERE vnf_id = 'd3def1ce-4cf4-477c-aab3-21c454e6a379';