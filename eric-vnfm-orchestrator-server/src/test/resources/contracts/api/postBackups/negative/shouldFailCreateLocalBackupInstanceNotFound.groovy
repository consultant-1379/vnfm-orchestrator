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
Represents a failed scenario of creating a local backup for a VNF Instance when vnfInstance is not found

```
given:
  Client requests to create a local backup for a VNF instance
when:
  An invalid request is submitted with unknown vnfInstance
then:
  The return that the request response was 404
```

""")
    request {
        method 'POST'
        url("/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/backups")
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
    priority 5

}
