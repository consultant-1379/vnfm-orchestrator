--- Complement to V33_2__add_downgrade_check_data
--- Adds a helm charts history for operations that should succeed in test scenario

--- INSTANTIATE
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count, life_cycle_operation_id)
  VALUES ('d480bdee-065a-4056-9cb1-b1cbaabd0163', 'https://sky.net/helm/registry/magic-chart-238.tgz', 0, 'downgrade-test-1', 'COMPLETED',
  '1', 1, 'downgrade-74f-4673-91ee-761fd83991e5');
--- UPGRADE
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count, life_cycle_operation_id)
  VALUES ('8cf641a9-3ea2-474c-be4c-0d9434b3182f', 'https://sky.net/helm/registry/magic-chart-241.tgz', 0, 'downgrade-test-1', 'COMPLETED',
  '2', 1, 'downgrade-74f-4673-91ee-761fd83991e6');
--- SCALE
INSERT INTO helm_chart_history (id, helm_chart_url, priority, release_name, state, revision_number, retry_count, life_cycle_operation_id)
  VALUES ('2aaed0b8-366b-4a64-8ab8-e21e52a55ed8', 'https://sky.net/helm/registry/magic-chart-241.tgz', 0, 'downgrade-test-1', 'COMPLETED',
  '3', 1, 'downgrade-74f-4673-91ee-761fd83991e7');
