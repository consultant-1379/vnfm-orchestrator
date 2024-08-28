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
package contracts.api.postScaleVnf.negative.notFound

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents an error scenario of scaling a VNF Instance

```
given:
  Client requests to scale a VNF Instance
when:
  A request with a vnfInstanceId is submitted
then:
  The request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/not-found/scale")))
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
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"Vnf instance with id ${fromRequest().path(3)} does not exist",
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
