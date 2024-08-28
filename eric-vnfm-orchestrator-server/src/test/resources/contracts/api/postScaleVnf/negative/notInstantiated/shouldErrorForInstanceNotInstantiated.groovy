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
package contracts.api.postScaleVnf.negative.notInstantiated;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario scale of a VNF Instance

```
given:
  Client requests to scale a VNF Instance
when:
  A request with a vnfInstanceId is submitted
then:
  The request is rejected with 409 CONFLICT
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/not-instantiated/scale")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                "type": "SCALE_OUT",
                "aspectId": "test_id"
        )
        bodyMatchers {
            jsonPath('$.type', byRegex(/(SCALE_OUT|SCALE_IN)/))
            jsonPath('$.aspectId', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"This resource is not in the INSTANTIATED state",
                          "status":409,
                          "detail":"VNF instance ID dummy_id is not in the INSTANTIATED state",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
