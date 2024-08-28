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
package contracts.api.postHealVnf.negative.healBadRequest;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of healing a VNF Instance

```
given:
  client requests to heal a VNF Instance
when:
  a request with restore.backupFileReference and restore.backupName as additional params is submitted
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*BAD_REQUEST_CNA_AND_CNF_PARAMS_DEFINED/heal")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """{
                               "cause": "Full Restore",
                               "additionalParams":{
                                    "restore.backupFileReference": "file",
                                    "restore.backupName": "name"
                               }
                            }"""
        )
        bodyMatchers {
            jsonPath('$.cause', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Invalid Heal Request Exception",
                          "status":400,
                          "detail":"restore.backupName and restore.backupFileReference can not be present in the same HEAL Request",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
