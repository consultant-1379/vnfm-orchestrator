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
package contracts.api.getResourcesPaginated.podstatus.notInstantiated;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting the component status for a non instantiated resource Id

```
given:
  client requests to get a a list of component status with an valid id
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with 409 CONFLICT
```

""")
    request {
        method 'GET'
        url "/api/v1/resources/dummy_id/pods"
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
    priority(2)
}
