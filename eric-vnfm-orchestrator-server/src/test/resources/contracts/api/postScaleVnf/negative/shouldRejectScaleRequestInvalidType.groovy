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
package contracts.api.postScaleVnf.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a negative scenario scale of a VNF Instance

```
given:
  Client requests to scale a VNF instance
when:
  A invalid request is submitted
then:
  The request is rejected
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/scale")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                "type": "not_valid",
                "aspectId": "test_id"
        )
        bodyMatchers {
            jsonPath('$.type', byRegex(nonEmpty()).asString())
            jsonPath('$.aspectId', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                   {
                       "type":"about:blank",
                       "title":"Malformed Request",
                       "status":400,
                       "detail":"JSON parse error: Cannot construct instance of `com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest\$TypeEnum`, problem: Unexpected value 'not_valid'",
                       "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                   }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 3
}
