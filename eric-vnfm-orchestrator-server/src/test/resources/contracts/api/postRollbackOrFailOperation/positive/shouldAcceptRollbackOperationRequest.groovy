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
package contracts.api.postRollbackOrFailOperation.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of rollback of an operation

```
given:
  Client requests to rollback an operation
when:
  A valid request is submitted
then:
  The operation is rolled back
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_lcm_op_occs/${value(consumer(anyNonEmptyString()))}/rollback"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status ACCEPTED()
    }
    priority 3
}
