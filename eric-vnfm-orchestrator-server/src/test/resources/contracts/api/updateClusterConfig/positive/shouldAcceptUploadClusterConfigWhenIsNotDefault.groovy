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
Represents a successful scenario of updating a cluster config file without passing isDefault flag

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
        url "/vnflcm/v1/clusterconfigs/${value(consumer(anyNonEmptyString()), producer('cluster02.config'))}?skipSameClusterVerification=${value(consumer(anyBoolean()), producer(true))}"
        multipart(
                clusterConfig: named(
                        name: $(consumer(nonEmpty()),
                                producer('cluster02.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('cluster02.config')))
                ),
                isDefault: false,
                description: $(consumer(optional(regex('.*'))), producer("Updated cluster config file description."))
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
        }
    }
    response {
        status OK()
        body(
                id: $(regex("(.+-){4}.+")),
                name: "cluster02.config",
                status: "NOT_IN_USE",
                description: "Updated cluster config file description.",
                crdNamespace: "eric-crd-ns",
                isDefault: false
        )
    }
    priority 2
}