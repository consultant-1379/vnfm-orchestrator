/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package contracts.api.postInstantiateVnf.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Instantiating a VNF Instance with Target Scale Level Info

```
given:
  client requests to instantiate a VNF instance with Target Scale Level Info
when:
  a valid request is submitted
then:
  the VNF instance is instantiated
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/instantiate"
        body(
                """
            {
                "clusterName": "haber002",
                "additionalParams": {
                    "namespace": "zztakno-ns-test",
                    "releaseName": "zztakno-ns-test",
                    "applicationTimeOut": "3600",
                    "disableOpenapiValidation": true,
                    "skipJobVerification": false,
                    "skipVerification": false,
                    "helmNoHooks": false,
                    "cleanUpResources": false,
                    "manoControlledScaling": false,
                    "tags.all": false,
                    "tags.pm": true,
                    "eric-adp-gs-testapp.ingress.enabled": false,
                    "eric-pm-server.server.ingress.enabled": false,
                    "influxdb.ext.apiAccessHostname": "influxdb-service2.rontgen010.seli.gic.ericsson.se",
                    "pm-testapp.ingress.domain": "rontgen010.seli.gic.ericsson.se",
                    "eric-pm-server.server.persistentVolume.storageClass": "network-block",
                    "eric-adp-gs-testapp.tls.dced.enabled": false,
                    "config.nbi.file.enabled": false,
                    "bro_endpoint_url": "http://eric-ctrl-bro.YOUR-NAMESPACE:7001",
                    "global.hosts.bro": "bro.test.hahn061.rnd.gic.ericsson.se",
                    "bro.ingress.enabled": false,
                    "backup-controller.enabled": false,
                    "retrieveUnsealKey": false,
                    "day0.configuration.secretname": "restore-external-storage-secret",
                    "day0.configuration.param1.key": "restore.externalStorageURI",
                    "day0.configuration.param1.value": "external-storage-url",
                    "day0.configuration.param2.key": "restore.externalStorageCredentials",
                    "day0.configuration.param2.value": "external-storage-credentials"
                },
                "extensions": {
                    "vnfControlledScaling": {
                        "Aspect1": "ManualControlled",
                        "Aspect2": "ManualControlled",
                        "Aspect3": "ManualControlled",
                        "Aspect5": "CISMControlled"
                    },
                    "deployableModules" : {
                        "deployable_module_1" : "enabled",
                        "deployable_module_2" : "disabled"
                    }
                },
                "instantiationLevelId": null,
                "targetScaleLevelInfo": [
                    {
                        "aspectId": "Aspect1",
                        "scaleLevel": "4"
                    },
                    {
                        "aspectId": "Aspect3",
                        "scaleLevel": "2"
                    },
                    {
                        "aspectId": "Aspect5",
                        "scaleLevel": "6"
                    }
                ]
            }
                 """
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        bodyMatchers {
            jsonPath('$.targetScaleLevelInfo', byRegex(/(.|\s)*/))
        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/5f43fb8e-1316-468a-9f9c-b375e5d82094")
        }
    }
    priority(2)
}
