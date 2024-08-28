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
package contracts.api.postChangePackageInfoVnf.negative;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of changing package info for a VNF Instance

```
given:
  Client requests to change package info for a VNF Instance
when:
  A request with a null vnfdid is submitted
then:
  The request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/change_vnfpkg")))
        body(
                "{}"
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Bad Request",
                          "status":400,
                          "detail":"vnfdId must not be null",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
