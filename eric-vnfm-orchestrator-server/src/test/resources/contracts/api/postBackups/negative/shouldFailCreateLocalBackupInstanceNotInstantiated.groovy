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
package contracts.api.postBackups.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failed scenario of creating a local backup for a VNF Instance that is not in an instantiated state 

```
given:
  Client requests to create a local backup for a VNF instance
when:
  A valid request is submitted but VNF instance is not in an instantiated state
then:
  The return that the request response was 409
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/g7def1ce-4cf4-477c-ahb3-61c454e6a344/backups")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                file('local-additional-parameters.json')
        )
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"This resource is not in the INSTANTIATED state",
                          "status":409,
                          "detail":"VNF instance ID ${fromRequest().path(3)} is not in the INSTANTIATED state",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1

}
