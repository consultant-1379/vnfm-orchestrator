update app_vnf_instance
set combined_additional_params = '{"Payload_InitialDelta.replicaCount":3,
 "Payload_InitialDelta_1.replicaCount":1, "helmWait":true, "manoControlledScaling":true, "helmNoHooks":true, "disableOpenapiValidation":true,
 "skipJobVerification":true, "skipVerification":true, "applicationTimeOut":500, "cleanUpResources":true}'
where vnf_id = 'values-4cf4-477c-aab3-21c454e6a380';

update app_vnf_instance
set combined_additional_params = '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300"}'
where vnf_id = 'vnfi100e-4cf4-477c-aab3-21c454e6666';

update app_vnf_instance
set combined_additional_params = '{"skipVerification": true, "skipJobVerification": true,"applicationTimeOut":"300"}'
where vnf_id = 'h1def1ce-4cf4-477c-aab3-21c454e6666';
