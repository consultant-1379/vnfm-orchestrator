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
package contracts.api.updateClusterConfigPartial.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of partial updating the default cluster config file with isDefault false

```
given:
  client requests to update cluster config file partially
when:
  description field is longer then 250 characters
then:
  the request is rejected
```

""")
    request {
        method 'PATCH'
        url '/vnflcm/v1/clusterconfigs/cluster02.config'
        body(
                "{\"isDefault\": false}"
        )
        headers {
            contentType("application/merge-patch+json")
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                   {
                      "type": "about:blank",
                      "title": "Cluster config update validation error",
                      "detail": "One of the clusters must be marked as default",
                      "status": 400,
                      "instance": "about:blank"
                   }
            """
        )
    }
}
