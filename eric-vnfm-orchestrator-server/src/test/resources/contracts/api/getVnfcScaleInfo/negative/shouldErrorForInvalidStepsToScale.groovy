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

package contracts.api.getVnfcScaleInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for requesting VNFC scale info

```
given:
  Client requests to get the VNFC scale info of a vnf resource with an invalid value for number of steps
when:
  the request is submitted
then:
  the request returns a 400 BAD REQUEST with details of the problem
```

""")
    request {
        method GET()
        urlPath($(regex("/vnflcm/v1/resources/[a-z0-9]+(-[a-z0-9]+)*/vnfcScaleInfo"))){
            queryParameters {
                parameter 'type': 'SCALE_IN'
                parameter 'aspectId': 'Payload'
                parameter 'numberOfSteps': value(consumer(regex(/(-(\d+))|0/)), producer(-1))
            }
        }
        headers {
            accept(applicationJson())
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
                          "detail":"Invalid scale step provided, Scale step should be a positive integer",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}