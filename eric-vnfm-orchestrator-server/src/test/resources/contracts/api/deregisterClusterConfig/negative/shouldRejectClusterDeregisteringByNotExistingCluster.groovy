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
Represents an unsuccessful scenario during de-registration a cluster config file

```
given:
  cluster config file with name notExist.config not registered
when:
  de-registration called with notExist.config cluster config file name
then:
   the request is rejected
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/clusterconfigs/notExist.config"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type": "about:blank",
                          "title": "Cluster config file does not exist.",
                          "status": 404,
                          "detail": "Cluster config file notExist.config does not exist.",
                          "instance": "about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
