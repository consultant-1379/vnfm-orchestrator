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
package contracts.api.updateClusterConfig.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario during update of cluster config file

```
given:
  cluster config update called with cluster503ForUpdate.config cluster config file name  
when:
  cluster config file with name cluster503ForDeregister.config registered
then:
  the request is rejected by service unavailable
```

""")
    request {
        method 'PUT'
        url"/vnflcm/v1/clusterconfigs/cluster503ForUpdate.config"
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('^((?![`~!@#$%^&*()_|+\\-=?;:\'",<>\\{\\}\\[\\]\\\\\\/]).)*.config$')),
                                producer('cluster503ForUpdate.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('cluster503ForUpdate.config')))
                ),
                description: "Description for service unavailable error",
                isDefault: true
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
        }
    }
    response {
        status SERVICE_UNAVAILABLE()
    }
}
