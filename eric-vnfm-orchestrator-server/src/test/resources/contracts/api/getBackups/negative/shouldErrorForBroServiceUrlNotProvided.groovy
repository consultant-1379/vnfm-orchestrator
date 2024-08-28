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
package contracts.api.getBackups.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of getting all backups

```
given:
  client requests to get all backups
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with 400 since BRO service url is not provided
```

""")
    request {
        method GET()
        urlPath($(regex("/vnflcm/v1/vnf_instances/wf1ce-rd45-477c-vnf0-backup001/backups")))
        headers {
            accept(applicationJson())
        }
    }
    response {
        status UNPROCESSABLE_ENTITY()
        body(
                """
                      {
                         "type":"about:blank",
                         "title":"Mandatory parameter missing",
                         "status":422,
                         "detail":"bro_endpoint_url is not defined in VNFD for instance. Please see documentation",
                         "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                      }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}

