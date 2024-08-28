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
package contracts.api.postInstantiateVnf.negative.alreadyInstantiated;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Instantiating a VNF Instance

```
given:
  client requests to instantiate a VNF Instance
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with 409 Conflict
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/vnf_instances/already-instantiated/instantiate'
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """
           {
            "additionalParams": {
               "namespace": "my-namespace"
                 }
                }
                     """
        )
        bodyMatchers {
            jsonPath('$.additionalParams', byRegex(/(.|\s)*/))
        }
    }

    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"The resource is already in the INSTANTIATED state",
                          "status":409,
                          "detail":"VNF instance ID already-instantiated is already in the INSTANTIATED state",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(4)
}
