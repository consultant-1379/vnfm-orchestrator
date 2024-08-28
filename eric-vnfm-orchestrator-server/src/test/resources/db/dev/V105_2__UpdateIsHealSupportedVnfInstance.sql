UPDATE app_vnf_instance SET is_heal_supported = TRUE WHERE is_heal_supported IS NULL;
ALTER TABLE app_vnf_instance ALTER COLUMN is_heal_supported SET NOT NULL;