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
package contracts.api.deleteVnfIdentifier.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of deleting a VNF Identifier.

```
given:
  client requests to deleteFile an existing VNF Identifier, where the VNF instance resource is not in the INSTANTIATED state
when:
  a valid request is submitted
then:
  the VNF Identifier is deleted.
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/vnf_instances/${value(consumer(anyNonEmptyString()))}"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status NO_CONTENT()
    }
    priority(2)
}
