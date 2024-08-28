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
package contracts.api.getResourcesPaginated.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of getting all vnf resources of first page.

```
given:
  client requests to get all vnf resources
when:
  a valid request is submitted
then:
  the list of all vnf resources of first page are returned
```

""")
    request {
        method 'GET'
        url '/api/v1/resources'
    }
    response {
        status OK()
        body(file('allResourcesDefault.json'))
        headers {
            contentType(applicationJson())
        }
    }
}

