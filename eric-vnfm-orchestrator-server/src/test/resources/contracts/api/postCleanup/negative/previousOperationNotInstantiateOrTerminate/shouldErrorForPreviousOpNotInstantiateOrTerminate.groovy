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
package contracts.api.postCleanup.negative.previousOperationNotInstantiateOrTerminate

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents an error scenario of cleaning up resources of a VNF Instance

```
given:
  Client requests to cleanup resources of a VNF Instance after a failed instantiate request
when:
  A request with a vnfInstanceId is submitted but previous operation was not of type INSTANTIATE
then:
  The request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/last-op-not-instantiate-or-terminate/cleanup")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Clean up of Resources cannot be performed",
                          "status":400,
                          "detail":"Resources will not be cleaned up; last operation on instance was not a failed INSTANTIATE or TERMINATE",
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
