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
package contracts.api.deregisterClusterConfig.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of de-registration a cluster config file

```
given:
  registered cluster config file
when:
  de-registration called with appropriate cluster config file name
then:
  cluster config file should be deleted and response ok.
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/clusterconfigs/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*\.config/)))}"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status NO_CONTENT()
    }
    priority 1
}
