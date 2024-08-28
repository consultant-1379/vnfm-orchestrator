ALTER TABLE app_vnf_instance
    ADD CONSTRAINT duplicate_release_name UNIQUE (vnf_instance_name, cluster_name);
