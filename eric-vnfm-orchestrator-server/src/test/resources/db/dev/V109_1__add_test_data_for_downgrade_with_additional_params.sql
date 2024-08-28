UPDATE app_lifecycle_operations SET operation_params = '{"additionalParams": {"applicationTimeOut": 100}}',
values_file_params = '{"test_VDU": {"replicaCount": 3}}}' WHERE operation_occurrence_id = 'downgrade-74f-4673-91ee-761fd83991e5';
UPDATE app_lifecycle_operations SET operation_params = '{"additionalParams": {"applicationTimeOut": 300}}',
values_file_params = '{"test_VDU_2": {"replicaCount": 5}}}' WHERE operation_occurrence_id = 'downgrade-74f-4673-91ee-761fd83991e6';