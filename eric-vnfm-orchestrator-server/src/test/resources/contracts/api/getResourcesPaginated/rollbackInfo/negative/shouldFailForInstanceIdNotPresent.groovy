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
package contracts.api.getResources.rollbackInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting rollback info for InstanceId not present

```
given:
  client requests to get a rollbackInfo
when:
  a instanceId not present
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        url "/api/v1/resources/${value(consumer('NO_INSTANCE_ID_FOUND'), producer('NO_INSTANCE_ID_FOUND'))}/rollbackInfo"
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"Vnf instance with id NO_INSTANCE_ID_FOUND does not exist",
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
