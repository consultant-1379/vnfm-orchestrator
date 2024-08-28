-- FIX TEST DATA FOR HEAL COMPLETED FAILED WITHOUT OTP

UPDATE app_lifecycle_operations
SET operation_params = '{"cause":"Full Restore", "additionalParams":{"ipVersion": "ipv4", "restore.backupFileReference": "sftp://users@14BCP04/my-backup", "restore.password": "password"}}'
WHERE operation_occurrence_id = 'oki98ec3-474f-4673-91ee-656fd8366666';
