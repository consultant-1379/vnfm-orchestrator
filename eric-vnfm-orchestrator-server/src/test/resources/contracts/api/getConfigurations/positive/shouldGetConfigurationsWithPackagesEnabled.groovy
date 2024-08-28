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
package contracts.api.getConfigurations.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of getting configurations with packages service enabled

```
given:
  client requests to get configurations information
when:
  a valid request is submitted
then:
  a valid response is returned with packages set to true
```

""")
    request {
        method GET()
        url "/info/v1/configurations"
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(file('with-packages.json'))
    }
    priority(2)
}