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
                parameter 'filter': value(consumer(regex("^\\((?:eq|neq|in|nin|gt|gte|lt|lte|cont|ncont),(?:" +
                        "instanceId|vnfInstanceName|vnfInstanceDescription|vnfdId|vnfProvider|vnfProductName|" +
                        "vnfSoftwareVersion|vnfdVersion|vnfPkgId|clusterName|instantiationState|" +
                        "lcmOperationDetails/currentLifecycleOperation|lcmOperationDetails/operationOccurrenceId|" +
                        "lcmOperationDetails/operationState|lcmOperationDetails/stateEnteredTime|" +
                        "lcmOperationDetails/startTime|lcmOperationDetails/grantId|" +
                        "lcmOperationDetails/lifecycleOperationType|lcmOperationDetails/automaticInvocation|" +
                        "lcmOperationDetails/operationParams|lcmOperationDetails/cancelPending|" +
                        "lcmOperationDetails/cancelMode),(?:\\w+)\\)\$")),
                        producer("(eq,lcmOperationDetails/currentLifecycleOperation,true)"))
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }

        body(file("allResourcesDefault.json"))
        bodyMatchers {
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.first.href', byCommand("assertThat(parsedJson.read(\"\$._links.first\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.next.href', byCommand("assertThat(parsedJson.read(\"\$._links.next\", Object.class)).isNotNull()"))
            jsonPath('$._links.last.href', byCommand("assertThat(parsedJson.read(\"\$._links.last\", Object.class)).isNotNull()"))
            jsonPath('$.page', byCommand("assertThat(parsedJson.read(\"\$.page\", Object.class)).isNotNull()"))
            jsonPath('$.page.number', byCommand("assertThat(parsedJson.read(\"\$.page.number\", Object.class)).isNotNull()"))
            jsonPath('$.page.size', byCommand("assertThat(parsedJson.read(\"\$.page.size\", Object.class)).isNotNull()"))
            jsonPath('$.page.totalPages', byCommand("assertThat(parsedJson.read(\"\$.page.totalPages\", Object.class)).isNotNull()"))
            jsonPath('$.page.totalElements', byCommand("assertThat(parsedJson.read(\"\$.page.totalElements\", Object.class)).isNotNull()"))
            jsonPath('$.items', byCommand("assertThat(parsedJson.read(\"\$.items\", Object.class)).isNotNull()"))
        }

    }
}

