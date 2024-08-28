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
package contracts.api.postChangeVnfPkgInfo.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of changing package info with additional parameters for a VNF Instance

```
given:
  Client requests to change package info with with additional parameters for  a VNF instance
when:
  A valid request is submitted
then:
  The VNF instance is changed
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/change_vnfpkg")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """{
                                "vnfdId": "d807978b-13e2-478e-8694-5bedbf2145e2",
                                    "additionalParams": {
                                        "system.protocol": "MD5",
                                        "restore.scope": "Rollback",
                                        "component.replicaCount": 3,
                                        "day0.configuration.param1.key": "key1",
                                        "day0.configuration.param2.key": "key2",
                                        "day0.configuration.secretname": "secret",
                                        "restore.backupName": "Backup name",
                                        "day0.configuration.param2.value": "param2",
                                        "day0.configuration.param1.value": "param1",
                                        "skipJobVerification": true
                                    }
                            }"""
        )
        bodyMatchers {
            jsonPath('$.vnfdId', byRegex(nonEmpty()).asString())
            jsonPath('$.additionalParams', byRegex(/(.|\s)*/))
        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/d807978b-13e2-478e-8694-5bedbf2145e2")
        }
    }
    priority 2
}