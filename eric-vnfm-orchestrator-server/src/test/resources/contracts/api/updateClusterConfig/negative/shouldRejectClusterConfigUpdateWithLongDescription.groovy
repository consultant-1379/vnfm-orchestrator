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
package contracts.api.updateClusterConfig.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of updating a cluster config file

```
given:
  client requests to update cluster config file  
when:
  cluster config file with description longer then 250 characters
then:
  the request is rejected
```

""")
    request {
        method 'PUT'
        url"/vnflcm/v1/clusterconfigs/invalidDescription.config"
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('invalidDescription.config')),
                                producer('invalidDescription.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('invalidDescription.config')))
                ),
                description: "Description longer then 250 Description longer then 250 Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250",
                isDefault: true
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Description validation failed",
                          "status":400,
                          "detail":"Description should not be longer then 250 characters",
                          "instance":"about:blank"
                       }
                """
        )
    }
    priority 1
}
