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
package contracts.api.postSyncVnfInstance.negative.notFound;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a negative scenario sync of a VNF Instance

```
given:
  Client requests to sync a single VNF instance
when:
  A sync request with not existed vnf instance is submitted
then:
  The VNF instance is failed with NOT_FOUND status
```

""")
    request {
        method 'POST'
        url("/vnflcm/v1/vnf_instances/not-found/sync")
        body(
                "{}")
        headers {
            contentType(applicationJson())
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
    }
    priority 2
}
