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
package contracts.api.getVnfLcmOpOccById.negative;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of returning the VNF LCM Operation by OppOccId, specifically for testing EO EVNFM NBI

```
given:
  client requests to get VNF LCM Operation
when:
  a VMLCM LCM operation occurrence Id that is not found in the database is passed
then:
  the response is returned with a NOT_FOUND and the reason
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/vnf_lcm_op_occs/${value(consumer(anyNonEmptyString()))}VMLCM"
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
                          "detail":"The vnfLcmOpOccId-${fromRequest().path(3)} does not exist",
                          "instance":"about:blank"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}
