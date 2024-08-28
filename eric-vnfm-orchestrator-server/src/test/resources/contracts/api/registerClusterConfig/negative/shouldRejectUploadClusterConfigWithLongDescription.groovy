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
package contracts.api.registerClusterConfig.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of uploading a cluster config file

```
given:
  cluster config file with description longer then 250 characters
when:
  client requests to upload cluster config file invalid description length
then:
  the request is rejected
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/clusterconfigs'
        multipart(
                clusterConfig: named(
                        name: $(consumer(regex('noResponseRegister.config')),
                                producer('noResponseRegister.config')),
                        content: $(consumer(nonEmpty()),
                                producer(file('noResponseRegister.config')))
                ),
                description: "Description longer then 250 Description longer then 250 Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250Description longer then 250 Description longer then 250"
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
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
