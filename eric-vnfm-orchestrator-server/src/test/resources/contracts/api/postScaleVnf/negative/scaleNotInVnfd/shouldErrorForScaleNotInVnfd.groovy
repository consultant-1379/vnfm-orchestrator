package contracts.api.postScaleVnf.negative.scaleNotInVnfd

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
import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents an error scenario of scale for a VNF Instance

```
given:
  Client requests to scale a VNF Instance where vnfd does not include the scale interface
when:
  A request with a vnfInstanceId is submitted
then:
  The request is rejected with 412 PRECONDITION FAILED
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/no-scale-interface/scale")))
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
        status PRECONDITION_FAILED()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Operation not present in VNFD",
                          "status":412,
                          "detail":"Vnfd does not contain interface scale",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }

}
