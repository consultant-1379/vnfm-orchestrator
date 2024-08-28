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
package contracts.api.getInstance.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for getting a vnf Instance by Id

```
given:
  client requests vnf instance details
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with 404 NOT FOUND
```

""")
    request {
        method GET()
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+){0,1}-not-found*/)))}"
        headers {
            accept(applicationJson())
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
}
