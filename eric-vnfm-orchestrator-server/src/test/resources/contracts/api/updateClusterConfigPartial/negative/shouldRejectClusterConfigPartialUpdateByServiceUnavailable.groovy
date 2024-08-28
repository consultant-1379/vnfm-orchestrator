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
Represents an unsuccessful scenario during partial update of cluster config file

```
given:
  partial cluster config update called with cluster503ForUpdate.config cluster config file name  
when:
  cluster config file with name cluster503ForDeregister.config registered
then:
  the request is rejected by service unavailable
```

""")
    request {
        method 'PATCH'
        url "/vnflcm/v1/clusterconfigs/cluster503ForUpdate.config"
        body(
                "{" +
                    " \"description\": \"Description for service unavailable error\" " +
                "}"
        )
        headers {
            contentType("application/merge-patch+json")
            accept(applicationJson())
        }
    }
    response {
        status SERVICE_UNAVAILABLE()
    }
}
