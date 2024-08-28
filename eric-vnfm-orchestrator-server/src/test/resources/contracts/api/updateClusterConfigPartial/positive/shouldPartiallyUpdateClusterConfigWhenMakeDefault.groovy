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
package contracts.api.updateClusterConfigPartial.positive


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of updating a cluster config file partially

```
given:
  valid cluster config file fields provided
when:
  client requests to make cluster config file as default
then:
  the request is accepted and fulfilled
```

""")
    request {
        method 'PATCH'
        url "/vnflcm/v1/clusterconfigs/${value(consumer(anyNonEmptyString()), producer('cluster01.config'))}?skipSameClusterVerification=${value(consumer(anyBoolean()), producer(true))}"
        body(
                "{" +
                        " \"isDefault\": true" +
                        "}"
        )
        headers {
            contentType("application/merge-patch+json")
            accept(applicationJson())
        }
    }
    response {
        status OK()
        body(
                id: $(regex("(.+-){4}.+")),
                name: "cluster01.config",
                status: "NOT_IN_USE",
                description: "Original description",
                crdNamespace: "eric-crd-ns",
                isDefault: true
        )
    }
    priority 4
}