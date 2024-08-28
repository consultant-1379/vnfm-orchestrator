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
Represents a failure scenario of getting downgrade info for package deleted

```
given:
  client requests to get a downgradeInfo
when:
  a package id of downgrade package not present
then:
  failed with pre condition failed
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/resources/${value(consumer('package-details-deleted-for-downgrade'), producer('package-details-deleted-for-downgrade'))}/downgradeInfo"
    }
    response {
        status PRECONDITION_FAILED()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Downgrade not supported",
                          "status":412,
                          "detail":"Downgrade not supported for instance id ${fromRequest().path(3)} as the target downgrade package is no longer available",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        bodyMatchers {
            jsonPath('$.detail', byCommand("assertThat(parsedJson.read(\"\$.detail\", String.class)).isNotNull().isEqualTo(\"Downgrade not supported for instance id package-details-deleted-for-downgrade as the target downgrade package is no longer available\")"))
        }
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}