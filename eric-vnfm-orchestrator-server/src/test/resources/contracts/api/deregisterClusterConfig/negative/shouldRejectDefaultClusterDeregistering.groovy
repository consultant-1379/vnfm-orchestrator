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
configUsedByDefault
```
given:
  cluster config file with name configUsedByDefault.config is registered 
when:
  de-registration called with configUsedByDefault.config cluster config file name
then:
   the request is rejected
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/clusterconfigs/configUsedByDefault.config"
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
                          "title": "Cluster operation failed.",
                          "detail": "Default cluster config can not be deregister.",
                          "status": 409,
                          "instance": "about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
