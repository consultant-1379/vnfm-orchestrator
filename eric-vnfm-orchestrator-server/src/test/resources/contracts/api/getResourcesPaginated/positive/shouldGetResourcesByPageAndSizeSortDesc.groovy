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
Represents a successful scenario of getting all vnf resources with pagination

```
given:
  client requests to get all vnf resources on 1st page(page numbers start from 1) with page size of 4 and in descending order sorted by 
  vnfInstanceName 
when:
  a valid request is submitted
then:
  the list of all vnf resources are returned
```

""")
    request {
        method 'GET'
        urlPath("/api/v1/resources") {
            queryParameters {
                parameter 'page': '1'
                parameter 'size': '4'
                parameter 'sort': value(consumer(regex(/(vnfInstanceName,desc)/)), producer("vnfInstanceName,desc"))
            }
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file('pageOneResourcesDesc.json'))
    }
}
