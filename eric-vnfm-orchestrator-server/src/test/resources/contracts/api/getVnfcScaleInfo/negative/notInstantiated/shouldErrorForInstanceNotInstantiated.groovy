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

package contracts.api.getVnfcScaleInfo.negative.notInstantiated

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for requesting VNFC scale info

```
given:
  Client requests to get the VNFC scale info of a vnf resource which has not been instantiated
when:
  the request is submitted
then:
  The request is rejected with 409 CONFLICT
```

""")
    request {
        method 'GET'
        urlPath($(regex("/vnflcm/v1/resources/NOT_INSTANTIATED/vnfcScaleInfo"))){
            queryParameters {
                parameter 'type': value(consumer(regex(/(SCALE_OUT|SCALE_IN)/)),
                        producer("SCALE_OUT"))
                parameter 'aspectId': value(consumer(regex(nonEmpty()).asString()), producer("test_id"))
            }
        }
        headers {
            accept(applicationJson())
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
