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
package contracts.api.postHealVnf.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of healing a VNF Instance

```
given:
  client requests to heal a VNF instance
when:
  a valid CNA request is submitted with the required & optional day0 additional params
then:
  the VNF instance is terminated
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/heal")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """{
                               "cause": "Full Restore",
                               "additionalParams":{
                                    "restore.backupName": "backupName",
                                    "restore.scope": "scope",
                                    "day0.configuration.secretname": "secret",
                                    "day0.configuration.param1.key": "key1",
                                    "day0.configuration.param1.value": "value1",
                                    "day0.configuration.param2.key": "key2",
                                    "day0.configuration.param2.value": "value2"
                               }
                            }"""
        )
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/d807978b-13e2-478e-8694-5bedbf2145e2")
        }
    }
    priority(6)
}