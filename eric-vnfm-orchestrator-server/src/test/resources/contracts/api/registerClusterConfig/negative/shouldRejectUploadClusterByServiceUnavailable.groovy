/*
 *   COPYRIGHT Ericsson 2024
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package contracts.api.registerClusterConfig.negative


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a unsuccessful scenario of uploading a cluster config file

```
given:
  cluster config file with name noResponseRegister.config already exists
when:
  client requests to upload cluster config file with same name
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
                description: "Description for service unavailable error"
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status SERVICE_UNAVAILABLE()
    }
    priority 1
}
