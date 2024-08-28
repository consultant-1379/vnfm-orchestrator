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
  The operation is not found
then:
  The request is rejected
```


""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_lcm_op_occs/NOT_FOUND/${value(regex(/(rollback|fail)/))}"
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
                          "detail":"The vnfLcmOpOccId-NOT_FOUND does not exist",
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
