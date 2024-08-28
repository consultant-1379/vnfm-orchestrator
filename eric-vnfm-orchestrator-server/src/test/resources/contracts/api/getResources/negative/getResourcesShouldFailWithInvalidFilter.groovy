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
package contracts.api.getResources.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting a vnf resources

```
given:
  client requests to get a vnf resources with an invalid id
when:
  a invalid instanceId is provided
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        urlPath("/vnflcm/v1/resources") {
            queryParameters {
                parameter 'filter': value(consumer(regex("^\\((?:eq|neq|in|nin|gt|gte|lt|lte|cont|ncont),((?!" +
                        "instanceId|vnfInstanceName|vnfInstanceDescription|vnfdId|vnfProvider|vnfProductName|" +
                        "vnfSoftwareVersion|vnfdVersion|vnfPkgId|clusterName|instantiationState|" +
                        "lcmOperationDetails/currentLifecycleOperation|lcmOperationDetails/operationOccurrenceId|" +
                        "lcmOperationDetails/operationState|lcmOperationDetails/stateEnteredTime|" +
                        "lcmOperationDetails/startTime|lcmOperationDetails/grantId|" +
                        "lcmOperationDetails/lifecycleOperationType|lcmOperationDetails/automaticInvocation|" +
                        "lcmOperationDetails/operationParams|lcmOperationDetails/cancelPending|" +
                        "lcmOperationDetails/cancelMode).*),(?:\\w+)\\)\$")), producer("(eq,test,false)"))
            }
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
                         "detail":"Filter not supported for (eq,test,false)",
                         "instance":"about:blank"
                      }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}



