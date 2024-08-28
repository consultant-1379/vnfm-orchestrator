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
package contracts.api.getAllOperationDetails.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of listing all operations.

```
given:
  client requests to view all operations
when:
  a valid request is made
then:
  all operations are displayed.
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/operations"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file('allOperations.json'))
        bodyMatchers {
            jsonPath('$.[*].timestamp', byCommand("assertThat(parsedJson.read(\"\$.[0].timestamp\", Object.class)).isNotNull()")) }
    }
}
