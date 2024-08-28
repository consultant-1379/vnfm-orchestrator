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
Captures cases where a VNF instance resource is in the INSTANTIATED state.

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
        url "/vnflcm/v1/vnf_instances/already-instantiated"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Illegal State Exception",
                          "status":409,
                          "detail":"Conflicting resource state - VNF instance resource for VnfInstanceId ${fromRequest().path(3)} is currently NOT in the NOT_INSTANTIATED state.",
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
