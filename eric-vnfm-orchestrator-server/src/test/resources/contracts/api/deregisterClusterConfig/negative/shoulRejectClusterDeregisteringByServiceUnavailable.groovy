/*
 *   COPYRIGHT Ericsson 2024
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */
package contracts.api.deregisterClusterConfig.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario during de-registration a cluster config file

```
given:
  cluster config file with name cluster503ForDeregister.config registered
when:
  de-registration called with cluster503ForDeregister.config cluster config file name
then:
   the request is rejected by service unavailable
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/clusterconfigs/cluster503ForDeregister.config"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status SERVICE_UNAVAILABLE()
    }
}
