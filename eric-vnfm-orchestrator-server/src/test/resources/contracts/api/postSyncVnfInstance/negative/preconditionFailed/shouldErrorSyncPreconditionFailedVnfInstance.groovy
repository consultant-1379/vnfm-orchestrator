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
package contracts.api.postSyncVnfInstance.negative.preconditionFailed;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a negative scenario sync of a VNF Instance

```
given:
  Client requests to sync a single VNF instance
when:
  A valid request is submitted
then:
  The VNF instance is not synced, another operation is in progress
```

""")
    request {
        method 'POST'
        url("/vnflcm/v1/vnf_instances/precondition-failed/sync")
        body(
                "{}")
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status PRECONDITION_FAILED()
        body("{\"detail\": \"Lifecycle operation SYNC is in progress for " +
                "vnf instance PRECONDITION_FAILED, hence cannot perform operation\"}")
    }
    priority 2
}
