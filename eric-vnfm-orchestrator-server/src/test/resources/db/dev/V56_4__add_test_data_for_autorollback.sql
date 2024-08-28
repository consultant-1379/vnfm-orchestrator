UPDATE app_lifecycle_operations
SET source_vnfd_id = 'd3def1ce-4cf4-477c-aab3-21cb04e6a379',
target_vnfd_id = 'b3def1ce-4cf4-477c-aab3-21cb04e6a252'
WHERE operation_occurrence_id IN ('wm8fcbc8-rd1f-4673-oper-downsize0002', 'wm8fcbc8-rd1f-4673-oper-downsize0003');

UPDATE app_vnf_instance
SET combined_additional_params = '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300","commandTimeOut":"300"}',
combined_values_file = '{"eric-adp-gs-testapp":{"ingress":{"enabled":false}}}'
WHERE vnf_id IN ('wf1ce-rd14-477c-vnf0-downsize0200', 'wf1ce-rd14-477c-vnf0-downsize0300');
