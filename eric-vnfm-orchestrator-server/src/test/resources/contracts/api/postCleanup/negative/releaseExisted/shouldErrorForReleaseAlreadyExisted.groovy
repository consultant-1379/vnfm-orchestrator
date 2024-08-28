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
package contracts.api.postCleanup.negative.releaseExisted

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents an error scenario of cleaning up resources of a VNF Instance after a failed instantiate request

```
given:
  Client requests to cleanup resources of a VNF Instance after a failed instantiate request
when:
  A request with a vnfInstanceId is submitted but instantiate had failed due to pre-existent release
then:
  The request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/release-already-existed/cleanup")))
        headers {
            accept(applicationJson())
            contentType(applicationJson())
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
                          "detail":"Resources will not be cleaned up; instantiate failed because another release with the same name already existed",
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
