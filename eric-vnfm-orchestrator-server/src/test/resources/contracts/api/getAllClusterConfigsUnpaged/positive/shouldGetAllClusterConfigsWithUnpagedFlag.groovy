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
package contracts.api.getAllClusterConfigsUnpaged.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of listing all cluster configs.

```
given:
  client requests to view all configs detail by one request with get getAllConfigs flag
when:
  a valid request is made
then:
  all configs detail are displayed.
```

""")
    request {
        method 'GET'
        urlPath("/vnflcm/v1/clusterconfigs") {
            queryParameters {
                parameter 'getAllConfigs': 'true'
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
        body(file('allClusterConfigs.json'))
    }
}
