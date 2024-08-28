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
package contracts.api.getResourcesPaginated.podstatus.negative.noPodsFound

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting the component status for a valid id with no pods

```
given:
  client requests to get a a list of component status with an valid id
when:
  a invalid instanceId is provided
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        url "/api/v1/resources/${value(consumer(anyNonEmptyString()), producer('test'))}NOPODSFOUND/pods"
    }
    response {
        status NOT_FOUND()
        body(
                """
                    {
                        "type":"about:blank",
                        "title":"The Pod Status operation request failed",
                        "status":404,
                        "detail":"No pods found for instance testNOPODSFOUND",
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



