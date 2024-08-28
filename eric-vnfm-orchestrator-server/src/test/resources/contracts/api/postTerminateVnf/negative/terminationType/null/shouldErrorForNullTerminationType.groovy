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
package contracts.api.postTerminateVnf.negative.terminationType;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Terminating a VNF Instance

```
given:
  client requests to terminate a VNF Instance
when:
  a request with a null terminationType is submitted
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/terminate")))
        body(
                "terminationType": ""
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
                            "type": "about:blank",
                            "title": "Malformed Request",
                            "status": 400,
                            "detail": "JSON parse error: Cannot construct instance of `com.ericsson.vnfm.orchestrator.model.TerminateVnfRequest\$TerminationTypeEnum`, problem: Unexpected value ''",
                            "instance": "http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                        }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(2)
}
