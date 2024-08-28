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
package contracts.api.postRollbackOrFailOperation.negative.operationNotFound;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of rollback/fail of an operation

```
given:
  Client requests to rollback/fail an operation
when:
  The operation is not the current operation
then:
  The request is rejected
```


""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_lcm_op_occs/NOT_CURRENT/${value(regex(/(rollback|fail)/))}"
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
                          "detail":"The requested operation NOT_CURRENT is not the latest operation, can not perform request",
                          "instance":"about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 2

}
