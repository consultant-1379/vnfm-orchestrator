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
package contracts.api.updateClusterConfig.positive


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of updating the default cluster config file by making it not default

```
given:
  valid cluster config file is provided
when:
  client requests to update cluster config file
then:
  the request is accepted and fulfilled
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
                description: "Updated cluster config file description.",
                isDefault: false
        )
        headers {
            contentType(multipartFormData())
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
