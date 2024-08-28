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
package contracts.api.getVnfcScaleInfoNewInternal.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for requesting VNFC scale info

```
given:
  Client requests to get the VNFC scale info of a vnf resource
when:
  A valid request is submitted
then:
  the VNFC scale info is returned
```

""")
    request {
        method 'GET'
        urlPath($(regex("/api/v1/resources/[a-z0-9]+(-[a-z0-9]+)*/vnfcScaleInfo"))){
            queryParameters {
                parameter 'type': value(consumer(regex(/(SCALE_OUT|SCALE_IN)/)),
                        producer("SCALE_OUT"))
                parameter 'aspectId': value(consumer(regex(nonEmpty()).asString()), producer("Payload"))
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body("""
            [{
                "vnfcName": "test-vnfc-name",
                "currentReplicaCount": 23,
                "expectedReplicaCount": 42
            },
            {
                "vnfcName": "test-vnfc-name-2",
                "currentReplicaCount": 2,
                "expectedReplicaCount": 21
            },
            {
                "vnfcName": "test-vnfc-name-3",
                "currentReplicaCount": 1,
                "expectedReplicaCount": 9
            }]
        """)
        bodyMatchers {
            jsonPath('$.[*]', byCommand("assertThat(parsedJson.read(\"\$.[0]\", Object.class)).isNotNull()"))
            jsonPath('$[0].vnfcName', byRegex(nonEmpty()).asString())
            jsonPath('$[0].currentReplicaCount', byRegex(nonEmpty().asInteger()))
            jsonPath('$[0].expectedReplicaCount', byRegex(nonEmpty().asInteger()))
        }
    }
    priority 2
}
