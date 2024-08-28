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
package contracts.api.getValues.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting latest values for a given VNF instance

```
given:
  client requests values
when:
  a request with is submitted
then:
  values in yaml format is returned
```

""")
    request {
        method GET()
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/values"
        headers {
            accept(textPlain())
        }
    }
    response {
        status OK()
        headers {
            contentType(textPlain())
        }
        body(file("sample_values.yaml.properties").asString())
    }
    priority 1
}

