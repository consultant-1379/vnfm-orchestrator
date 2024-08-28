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
package contracts.api.getResources.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of getting a vnf resources

```
given:
  client requests to get a vnf resources with an invalid id
when:
  a invalid instanceId is provided
then:
  failed with data not found
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/resources/${value(consumer(anyNonEmptyString()), producer('test'))}NOTFOUND"
    }
    response {
        status NOT_FOUND()
        body(
                """
                    {
                        "type":"about:blank",
                        "title":"Not Found Exception",
                        "status":404,
                        "detail":"some string",
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



