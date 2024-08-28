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
package contracts.api.registerClusterConfig.positive


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of uploading a cluster config file

```
given:
  client requests to upload cluster config file
when:
  valid cluster config file is provided
then:
  the request is accepted
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/clusterconfigs'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*\.config/)),
                                producer('cluster01.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('cluster01.config')))
                ),
                description: $(consumer(optional(regex('.*'))), producer("Cluster config file description.")),
                isDefault: $(consumer(anyBoolean()), producer(true))
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status CREATED()
        body(
                id: $(regex("(.+-){4}.+")),
                name: "cluster01.config",
                status: "NOT_IN_USE",
                description: "Cluster config file description.",
                crdNamespace: "eric-crd-ns",
                isDefault: true
        )
    }
    priority 3
}
