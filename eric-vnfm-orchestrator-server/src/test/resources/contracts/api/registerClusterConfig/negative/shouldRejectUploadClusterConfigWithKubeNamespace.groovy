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
  cluster has a few kubernetes namespaces e.g. kube-system
when:
  client requests to upload cluster config file with a CRD namespace which points to a kubernetes namespace
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/clusterconfigs'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('^((?![`~!@#$%^&*()_|+\\-=?;:\'",<>\\{\\}\\[\\]\\\\\\/]).)*.config$')),
                                producer('validCluster.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('validCluster.config')))
                ),
                description: "Description for a valid cluster config file",
                crdNamespace: $(consumer(anyOf("default", "kube-system", "kube-public", "kube-node-lease")), producer('kube-system'))
        )
        headers {
            contentType(multipartFormData())
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
                      "title": "Cluster failed to register.",
                      "detail": "Kubernetes reserved namespace cannot be used as a CRD namespace.",
                      "status": 409,
                      "instance": "about:blank"
                   }
            """
        )
    }
    priority 1
}
