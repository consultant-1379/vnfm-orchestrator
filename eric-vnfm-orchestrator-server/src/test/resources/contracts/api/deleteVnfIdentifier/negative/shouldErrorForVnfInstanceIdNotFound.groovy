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
Captures cases where the requested VnfInstanceId is not found.

```
given:
  client requests to deleteFile a VNF Identifier by a VnfInstanceId which does not exist
when:
  a request is made to deleteFile the VNF Identifier
then:
  an error message is returned.
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
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"Vnf instance with id ${fromRequest().path(3)} does not exist",
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
