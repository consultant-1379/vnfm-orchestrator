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
Represents a unsuccessful scenario of updating a cluster config file when request sent without file.

```
given:
  cluster config file not provided in request  
when:
  client requests to update cluster config file
then:
  the request is rejected
```

""")
    request {
        method 'PUT'
        url"/vnflcm/v1/clusterconfigs/noConfigFile.config"
        multipart(
                description: "Cluster config file description."
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
                          "title":"Malformed Request",
                          "status":400,
                          "detail":"Required part 'clusterConfig' is not present.",
                          "instance":"about:blank"
                       }
                """
        )
    }
}
