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
package contracts.api.postSyncVnfInstance.negative.notInstantiated;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a negative scenario sync of a VNF Instance

```
given:
  Client requests to sync a single VNF instance
when:
  A sync request with not instantiated vnf instance is submitted
then:
  The VNF instance is failed with CONFLICT status 
```

""")
    request {
        method 'POST'
        url("/vnflcm/v1/vnf_instances/not-instantiated/sync")
        body(
                "{}")
        headers {
            contentType(applicationJson())
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
                          "title":"This resource is not in the INSTANTIATED state",
                          "status":409,
                          "detail":"VNF instance ID dummy_id is not in the INSTANTIATED state",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
    }
    priority 2
}
