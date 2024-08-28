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
package contracts.api.postCleanup.positive

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents a successful scenario of cleaning up resources of a VNF Instance after a failed instantiate request

```
given:
  Client requests to cleanup resources of a VNF Instance after a failed instantiate request
when:
  A valid request is submitted
then:
  The request is accepted
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/cleanup")))
        headers {
            accept(applicationJson())
            contentType(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/5f43fb8e-1316-468a-9f9c-b375e5d82333")
        }
    }
}
