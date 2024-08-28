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
package contracts.api.postVnfIdentifier.negative;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Creating a VNF Identifier

```
given:
  client requests to create a VNF Identifier
when:
  a request without vnfInstanceName is submitted
then:
  the request is rejected with 400
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/vnf_instances'
        body(
                "vnfdId": "54321",
                "vnfInstanceName": null,
                "vnfInstanceDescription": "testVnfDescription"
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        bodyMatchers {
            jsonPath('$.vnfdId', byRegex(nonEmpty()).asString())
            jsonPath('$.vnfInstanceDescription', byRegex(/([\w ]*)/))
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
                            "detail":"vnfInstanceName must not be null",
                            "instance":"about:blank"
                        }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
