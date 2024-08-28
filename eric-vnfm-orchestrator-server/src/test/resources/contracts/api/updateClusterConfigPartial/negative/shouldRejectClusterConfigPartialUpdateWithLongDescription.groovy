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
package contracts.api.updateClusterConfigPartial.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of partial updating a cluster config file

```
given:
  client requests to update cluster config file partially
when:
  description field is longer then 250 characters
then:
  the request is rejected
```

""")
    request {
        method 'PATCH'
        url '/vnflcm/v1/clusterconfigs/cluster02.config'
        body(
                "{" +
                        "\"description\": \"Description longer then 250 Description longer then 250 Description " +
                        "longer then 250 Description longer then 250Description longer then 250 Description longer" +
                        " then 250Description longer then 250 Description longer then 250Description longer then 250" +
                        " Description longer then 250Description longer then 250 Description longer then 250Description" +
                        " longer then 250 Description longer then 250Description longer then 250 Description longer" +
                        " then 250Description longer then 250 Description longer then 250Description longer then 250" +
                        " Description longer then 250Description longer then 250 Description longer then 250Description" +
                        " longer then 250 Description longer then 250Description longer then 250 Description longer" +
                        " then 250Description longer then 250 Description longer then 250Description longer then 250 " +
                        "longer then 250 Description longer then 250Description longer then 250 Description longer then\"" +
                "}"
        )
        headers {
            contentType("application/merge-patch+json")
            accept(applicationJson())
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
                          "detail":"description size must be between 0 and 250",
                          "instance":"about:blank"
                       }
                """
        )
    }
    priority 1
}
