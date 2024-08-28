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
  partial cluster config update called with notExist.config cluster config file name  
when:
  cluster config file with name notExist.config not registered
then:
  the request is rejected
```

""")
    request {
        method 'PATCH'
        url "/vnflcm/v1/clusterconfigs/notExist.config"
        body(
               "{" +
                   "\"description\": \"Description for not existing\" " +
               "}"
        )
        headers {
            contentType("application/merge-patch+json")
            accept(applicationJson())
        }
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Cluster config file does not exist.",
                          "status":404,
                          "detail":"Cluster config file notExist.config does not exist.",
                          "instance":"about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
