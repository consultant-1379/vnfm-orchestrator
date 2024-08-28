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
package contracts.api.getValues.negative.notFound

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario for getting backup Scopes given VNF instance

```
given:
  client requests all backup Scopes with wrong URL
when:
  a request is submitted
then:
  the request is rejected with 404 NOT FOUND
```

""")
    request {
        method GET()
        urlPath($(regex("/vnflcm/v1/vnf_instances/not-found/backup/scopes")))
        headers {
            accept(applicationJson())
            contentType(applicationJson())
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
