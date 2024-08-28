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
package contracts.api.addNode.negative.notFound;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of deleting a node 

```
given:
  client requests to deleteFile a node for a VNF Instance
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with a 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/vnf_instances/ftl-error/deleteNode'
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                "{}"
        )
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Malformed Request",
                          "status":400,
                          "detail":"Failed to deleteFile node with following parameters due to the following error",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
        bodyMatchers {
            jsonPath('$.detail', byRegex(nonEmpty()).asString())
        }
    }
    priority(1)
}

