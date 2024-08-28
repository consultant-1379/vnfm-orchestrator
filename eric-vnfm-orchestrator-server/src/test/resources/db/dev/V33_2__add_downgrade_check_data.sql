--BELOW INSTANCE REPRESENTS A FLOW THAT QUALIFIES FOR DOWNGRADE;
--INSTANTIATE VERSION 'downgrade-cf4-477c-aab3-21c454e6a379'
--UPGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a389'
--SCALE OF VERSION 'downgrade-cf4-477c-aab3-21c454e6a389'
--CHANGE PACKAGE IN PROGRESS

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('downgrade-3b-40b7-ab48-dd15d88332a7', 'downgrade-test', 'vnfInstanceDescription',
'ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-DOWNGRADE', 'NOT_INSTANTIATED', 'default66',
'downgrade-74f-4673-91ee-761fd83991e8', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd83991e5', 'downgrade-3b-40b7-ab48-dd15d88332a7', 'COMPLETED', now()- INTERVAL '6 hours' ,
now()- INTERVAL '6 hours',
null,
'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177','b1bb0ce7-ebca-4fa7-95ed-4840d70a1177');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd83991e6', 'downgrade-3b-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '5 hours', now()- INTERVAL '5 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177','ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('downgrade-74f-4673-91ee-761fd83991e6','UPGRADE','downgrade-cf4-477c-aab3-21c454e6a389');

--SCALE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd83991e7', 'downgrade-3b-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '4 hours', now()- INTERVAL '4 hours', null, 'SCALE', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5','ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5');

--BELOW INSTANCE REPRESENTS A FLOW THAT DOES NOT QUALIFY FOR DOWNGRADE AS THERE IS NO VERSION CHAMGE;
--INSTANTIATE VERSION 'downgrade-cf4-477c-aab3-21c454e6a379'
--UPGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a379'
--CHANGE PACKAGE  IN PROGRESS

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('no-downgrade-40b7-ab48-dd15d88332a7', 'no-downgrade-test', 'vnfInstanceDescription',
'downgrade-cf4-477c-aab3-21c454e6a379', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'no-scaling', 'INSTANTIATED', 'default66',
'no-downgrade-74f-4673-91ee-761fd83991e8', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('no-downgrade-74f-4673-91ee-761fd83991e6', 'no-downgrade-40b7-ab48-dd15d88332a7', 'COMPLETED', now()- INTERVAL '6 hours' ,
now()- INTERVAL '6 hours', null, 'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, null,'downgrade-cf4-477c-aab3-21c454e6a379');

--UPGRADE TO SAME VNFDID
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('no-downgrade-74f-4673-91ee-761fd83991e7', 'no-downgrade-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '5 hours', now()- INTERVAL '5 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'downgrade-cf4-477c-aab3-21c454e6a379','downgrade-cf4-477c-aab3-21c454e6a379');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('no-downgrade-74f-4673-91ee-761fd83991e7','UPGRADE','downgrade-cf4-477c-aab3-21c454e6a379');

--BELOW INSTANCE REPRESENTS A FLOW WHERE A DOWNGRADE HAS ALREADY BEEN PERFORMED
--INSTANTIATE VERSION 'downgrade-cf4-477c-aab3-21c454e6a379'
--UPGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a389'
--UPGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a390'
--DOWNGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a389'
--CHANGE PACKAGE IN PROGRESS TO 'downgrade-cf4-477c-aab3-21c454e6a379'

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('2downgrades-3b-40b7-ab48-dd15d88332a7', '2downgrades-test', 'vnfInstanceDescription',
'2downgrades-cf4-477c-aab3-21c454e6a389', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001', 'NOT_INSTANTIATED', 'default66',
'2downgrades-74f-4673-91ee-761fd83991e9', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e5', '2downgrades-3b-40b7-ab48-dd15d88332a7', 'COMPLETED', now()- INTERVAL '6 hours' ,
now()- INTERVAL '6 hours',
null,
'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, null,'downgrade-cf4-477c-aab3-21c454e6a379');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e6', '2downgrades-3b-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '5 hours', now()- INTERVAL '5 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'downgrade-cf4-477c-aab3-21c454e6a379','downgrade-cf4-477c-aab3-21c454e6a389');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e6','UPGRADE','downgrade-cf4-477c-aab3-21c454e6a389');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e7', '2downgrades-3b-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '4 hours', now()- INTERVAL '4 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'downgrade-cf4-477c-aab3-21c454e6a389','downgrade-cf4-477c-aab3-21c454e6a390');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e7','UPGRADE','downgrade-cf4-477c-aab3-21c454e6a390');

--DOWNGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e8', '2downgrades-3b-40b7-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '3 hours', now()- INTERVAL '3 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'downgrade-cf4-477c-aab3-21c454e6a390','downgrade-cf4-477c-aab3-21c454e6a389');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('2downgrades-74f-4673-91ee-761fd83991e8','DOWNGRADE','downgrade-cf4-477c-aab3-21c454e6a389');

--BELOW INSTANCE REPRESENTS A FLOW THAT NOT QUALIFIES FOR DOWNGRADE BECAUSE OF REMOVED PACKAGE/VNFD
--INSTANTIATE VERSION 'downgrade-cf4-477c-aab3-21c454e6a379'
--UPGRADE TO VERSION 'downgrade-cf4-477c-aab3-21c454e6a389'
--CHANGE PACKAGE IN PROGRESS

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('downgrade-no-package-ab48-dd15d88332a7', 'downgrade-no-package-test', 'vnfInstanceDescription',
'ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', 'd3def1ce-4cf4-477c-aab3-21cb04e6a379NOTFOUND', 'NOT_INSTANTIATED', 'default66',
'downgrade-74f-4673-91ee-761fd83991e8', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('downgrade-no-package-91ee-761fd83991e5', 'downgrade-no-package-ab48-dd15d88332a7', 'COMPLETED', now()- INTERVAL '6 hours' ,
now()- INTERVAL '6 hours',
null,
'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, null,'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177NOTFOUND');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('downgrade-no-package-91ee-761fd83991e6', 'downgrade-no-package-ab48-dd15d88332a7', 'COMPLETED',
now()- INTERVAL '5 hours', now()- INTERVAL '5 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177NOTFOUND','ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('downgrade-no-package-91ee-761fd83991e6','UPGRADE','downgrade-no-package-91ee-761fd83991e5');

--BELOW INSTANCE REPRESENTS A FLOW THAT DOESN'T QUALIFY FOR DOWNGRADE BECAUSE OF MATCHING POLICY ABSENCE;

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('downgrade-3b-40b7-ab48-dd15d67103a7', 'downgrade-no-policy-test', 'vnfInstanceDescription',
'ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-DOWNGRADE', 'NOT_INSTANTIATED', 'default66',
'downgrade-74f-4673-91ee-761fd83991e8', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd84670e5', 'downgrade-3b-40b7-ab48-dd15d67103a7', 'COMPLETED', now()- INTERVAL '5 hours' ,
now()- INTERVAL '5 hours',
null,
'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-coba-4fa7-95ed-8888d70a1177','b1bb0ce7-coba-4fa7-95ed-8888d70a1177');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd88824e6', 'downgrade-3b-40b7-ab48-dd15d67103a7', 'COMPLETED',
now()- INTERVAL '3 hours', now()- INTERVAL '3 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-coba-4fa7-95ed-8888d70a1177','ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('downgrade-74f-4673-91ee-761fd88824e6','UPGRADE','downgrade-cf4-477c-aab3-21c081e6a389');

--BELOW INSTANCE REPRESENTS A FLOW THAT QUALIFIES FOR DOWNGRADE WITHOUT ROLLBACK PATTERN;

INSERT INTO app_vnf_instance(vnf_id, vnf_instance_name, vnf_instance_description, vnfd_id, vnf_provider,
vnf_product_name, vnf_software_version, vnfd_version, vnf_pkg_id, instantiation_state, cluster_name,
current_life_cycle_operation_id, namespace)
VALUES ('downgrade-3b-40b7-ab48-dd15d87654a7', 'downgrade-test-no-pattern', 'vnfInstanceDescription',
'ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5', 'Ericsson', 'SGSN-MME',
'1.20 (CXS101289_R81E08)', 'cxp9025898_4r81e08', '9392468011745350001-DOWNGRADE-NO-PATTERN', 'NOT_INSTANTIATED', 'default66',
'downgrade-74f-4673-91ee-761fd83991e8', 'test');

--ORIGINAL INSTANTIATE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error, combined_additional_params, combined_values_file, source_vnfd_id, target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd32609e5', 'downgrade-3b-40b7-ab48-dd15d87654a7', 'COMPLETED', now()- INTERVAL '6 hours' ,
now()- INTERVAL '6 hours',
null,
'INSTANTIATE', 'FALSE', null, 'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177','b1bb0ce7-ebca-4fa7-95ed-4840d70a1177');

--UPGRADE
INSERT INTO app_lifecycle_operations(operation_occurrence_id, vnf_instance_id, operation_state, state_entered_time,
start_time, grant_id, lifecycle_operation_type, automatic_invocation, operation_params, cancel_pending, cancel_mode,
error,combined_additional_params,combined_values_file,source_vnfd_id,target_vnfd_id)
VALUES ('downgrade-74f-4673-91ee-761fd32298e6', 'downgrade-3b-40b7-ab48-dd15d87654a7', 'COMPLETED',
now()- INTERVAL '5 hours', now()- INTERVAL '5 hours', null, 'CHANGE_PACKAGE_INFO', 'FALSE', null,
'FALSE', 'FORCEFUL', null, null, null, 'b1bb0ce7-ebca-4fa7-95ed-4840d70a1177','ebc68e34-0cfa-40ba-8b45-9caa31f9dcb5');

--ADD CHANGE PACKAGE UPGRADE INFO
INSERT INTO change_package_operation_details (operation_occurrence_id, operation_subtype, target_operation_occurrence_id)
VALUES ('downgrade-74f-4673-91ee-761fd32298e6','UPGRADE','downgrade-cf4-477c-aab3-21c889e6a389');