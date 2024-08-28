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
package contracts.api.getAllVnfLcmOpOcc.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario for getting all VNF lifecycle operation with filter

```
given:
  client requests all lifecycle operation details with a invalid filter
when:
  a request with is submitted
then:
  fails with 400 response code
```

""")
    request {
        method GET()
        urlPath("/vnflcm/v1/vnf_lcm_op_occs") {
            queryParameters {
                parameter 'filter': value(consumer(regex("^\\((eq|neq|in|nin|gt|gte|lt|lte|cont|ncont),(" +
                        "(?!id|operationState|stateEnteredTime|startTime|vnfInstanceId|operation|grantId|" +
                        "isAutomaticInvocation|operationParams|isCanclePending|canclMode).*),(?:\\w+)\\)\$")),
                        producer("(eq,test,e3de)"))
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Malformed Request",
                          "status":400,
                          "detail":"Filter eq,test,e3de not supported",
                          "instance":"about:blank"
                       }
                """
        )
    }
}

