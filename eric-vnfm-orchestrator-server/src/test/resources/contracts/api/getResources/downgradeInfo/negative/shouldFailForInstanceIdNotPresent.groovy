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
package contracts.api.getResources.downgradeInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting downgrade info for InstanceId not present

```
given:
  client requests to get a downgradeInfo
when:
  a instanceId not present
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/resources/${value(consumer('NO_INSTANCE_ID_FOUND'), producer('NO_INSTANCE_ID_FOUND'))}/downgradeInfo"
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Downgrade not supported",
                          "status":404,
                          "detail":"NO_INSTANCE_ID_FOUND vnf resource not present",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        bodyMatchers {
            jsonPath('$.detail', byCommand("assertThat(parsedJson.read(\"\$.detail\", String.class)).isNotNull()"))
        }
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}