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
Represents a failure scenario of getting rollback info for temp instance not present

```
given:
  client requests to get a rollbackInfo
when:
  a temp instance not present
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/resources/${value(consumer('NO_TEMP_INSTANCE_FOUND'), producer('NO_TEMP_INSTANCE_FOUND'))}/rollbackInfo"
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Malformed Request",
                          "status":400,
                          "detail":"Unable to get the rollback info as temp instance not found for NO_TEMP_INSTANCE_FOUND",
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
