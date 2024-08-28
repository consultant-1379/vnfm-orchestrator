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
package contracts.api.registerClusterConfig.negative


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of uploading a cluster config file

```
given:
  client requests to upload cluster config file
when:
  cluster details not provided in cluster config file
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/clusterconfigs'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('.+nullClusters\\.config')),
                                producer('cluster01nullClusters.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('withoutClusters.config')))
                ),
                description: "Cluster config file description."
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                            type: "about:blank",
                            title: "Cluster config file not valid.",
                            status: 400,
                            detail: "An error occurred during validating cluster config: kube config clusters cannot be null",
                            instance: "about:blank"
        )
    }
}
