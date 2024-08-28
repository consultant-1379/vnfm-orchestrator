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
package contracts.api.getClusterConfigsByPageAndSize.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of listing cluster configs with pagination

```
given:
  client requests to view cluster configs on 2nd page (page numbers start from 1) with page size of 2 and in ascending order sorted by name
when:
  a valid request is made
then:
  configs specified are displayed.
```

""")
    request {
        method GET()
        urlPath("/vnflcm/v1/clusterconfigs") {
            queryParameters {
                parameter 'page': '2'
                parameter 'size': '2'
                parameter 'sort': value(consumer(regex(/(name,asc)/)), producer("name,asc"))
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
        body(file('pageTwoClusterConfigsAsc.json'))
        bodyMatchers {
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.first.href', byCommand("assertThat(parsedJson.read(\"\$._links.first\", Object.class)).isNotNull()"))
            jsonPath('$._links.prev.href', byCommand("assertThat(parsedJson.read(\"\$._links.prev\", Object.class)).isNotNull()"))
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
    priority(6)
}
