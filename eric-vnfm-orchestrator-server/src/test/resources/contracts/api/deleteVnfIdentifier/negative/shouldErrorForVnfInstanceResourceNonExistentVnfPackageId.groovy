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
package contracts.api.deleteVnfIdentifier.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Captures cases where a VNF instance resource has non-existent VNF Package ID.

```
given:
  client requests to deleteFile a VNF Identifier, where the VNF instance resource is in the INSTANTIATED state
when:
  a request is made to deleteFile the VNF Identifier
then:
  an error message is returned.
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/vnf_instances/wrong-package-id"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Bad request",
                          "status":400,
                          "detail":"Package Id and Instance Id are mandatory fields",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(4)
}
