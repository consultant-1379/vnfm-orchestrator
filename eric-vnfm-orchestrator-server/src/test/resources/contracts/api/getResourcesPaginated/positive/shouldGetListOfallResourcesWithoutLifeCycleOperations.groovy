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
Represents a successful scenario of getting a vnf resource matching the filter

```
given:
  client requests to get a vnf resource with a filter
when:
  a valid request is submitted
then:
  vnf resource is returned matching the filter
```

""")
    request {
        method 'GET'
        urlPath("/api/v1/resources") {
            queryParameters {
                parameter 'getAllResources': true
            }
        }
    }
    response {
        status OK()
        body(file('allResourcesWithoutLifecycles.json'))
        bodyMatchers {
            jsonPath('$.items[*].[\'lcmOperationDetails\']', byCommand("assertThat(parsedJson.read(\"\$.items[*].['lcmOperationDetails']\", Object.class)).isNotNull()"))
        }
        headers {
            contentType(applicationJson())
        }
    }
}

