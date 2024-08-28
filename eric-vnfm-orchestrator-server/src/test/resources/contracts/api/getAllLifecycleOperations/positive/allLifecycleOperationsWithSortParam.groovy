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
package contracts.api.getAllLifecycleOperations.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of listing all operations.

```
given:
  client requests to view all operations with pagination information for page 1
when:
  a valid request is made
then:
  all operations are displayed.
```

""")
    request {
        method 'GET'
        urlPath("/api/v1/operations") {
            queryParameters {
                parameter 'sort': 'vnfProductName,asc'
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file('allLifecycleOperationsWithSortParam.json'))
        bodyMatchers {
            jsonPath('$.items.[*].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.items[*].stateEnteredTime\", Object.class)).isNotNull()"))
            jsonPath('$.items.[*].startTime', byCommand("assertThat(parsedJson.read(\"\$.items[*].startTime\", Object.class)).isNotNull()"))
            jsonPath('$.items.[*].operationState', byCommand("assertThat(parsedJson.read(\"\$.items[*].operationState\", Object.class)).isNotNull()"))
            jsonPath('$.items.[*].lifecycleOperationType', byCommand("assertThat(parsedJson.read(\"\$.items[*].lifecycleOperationType\", Object.class)).isNotNull()"))
            jsonPath('$.items.[*].username', byCommand("assertThat(parsedJson.read(\"\$.items[*].username\", Object.class)).isNotNull()"))
        }
    }
}
