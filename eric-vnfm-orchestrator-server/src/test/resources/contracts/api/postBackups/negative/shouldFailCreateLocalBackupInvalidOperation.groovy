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
package contracts.api.postBackups.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failed scenario of creating a local backup for a VNF Instance that is not in an completed state 

```
given:
  Client requests to create a local backup for a VNF instance
when:
  A valid request is submitted but the operation state is not in Completed
then:
  The return that the request response was 422
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/wf1ce-rd45-477c-vnf0-snapshot004/backups")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                file('local-additional-parameters.json')
        )
    }
    response {
        status UNPROCESSABLE_ENTITY()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Invalid Operation State",
                          "status":422,
                          "detail":"Operation state has to be in COMPLETED in order to create a Snapshot",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1

}
