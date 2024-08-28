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
package contracts.api.getResources.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of getting a vnf resource

```
given:
  client requests to get a vnf resource with instance id 343532
when:
  a valid request is submitted
then:
  a vnf resource is returned
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/resources/343532"
    }
    response {
        status OK()
        body(file('resource3.json'))
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}

