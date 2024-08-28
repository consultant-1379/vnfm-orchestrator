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
package contracts.api.deregisterClusterConfig.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario during de-registration a cluster config file

```
given:
  cluster config file with name cnfUsed.config registered and used by CNF package
when:
  de-registration called with cnfUsed.config cluster config file name
then:
  the request is rejected
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/clusterconfigs/clusterConfigInUse.config"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type": "about:blank",
                          "title": "Cluster config file is in use and cannot be removed",
                          "status": 409,
                          "detail": "Cluster config file clusterConfigInUse.config is in use and not available for deletion.",
                          "instance": "about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
