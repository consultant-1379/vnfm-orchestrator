CREATE VIEW vnf_instance_view AS SELECT vnf_id, CONCAT(vnf_provider, '.', vnf_product_name, '.', vnf_software_version) AS software_packages FROM app_vnf_instance;
