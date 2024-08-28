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
package contracts.api.getResourcesPaginated.downgradeInfo.negative

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
  failed with un-processable entity 
```

""")
    request {
        method 'GET'
        url "/api/v1/resources/${value(consumer('DOWNGRADE_NOT_SUPPORTED_FOR_INSTANCE_ID'), producer('DOWNGRADE_NOT_SUPPORTED_FOR_INSTANCE_ID'))}/downgradeInfo"
    }
    response {
        status UNPROCESSABLE_ENTITY()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Downgrade not supported",
                          "status":422,
                          "detail":"Downgrade not supported for instance id ${fromRequest().path(3)}",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        bodyMatchers {
            jsonPath('$.detail', byCommand("assertThat(parsedJson.read(\"\$.detail\", String.class)).isNotNull().isEqualTo(\"Downgrade not supported for instance id DOWNGRADE_NOT_SUPPORTED_FOR_INSTANCE_ID\")"))
        }
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}