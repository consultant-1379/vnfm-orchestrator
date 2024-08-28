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
package contracts.api.postInstantiateVnf.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Instantiating a VNF Instance with Instantiation Level Id and some Addtional Parameters

```
given:
  client requests to instantiate a VNF instance with Instantiation Level Id and some Additional Parameters
when:
  a valid request is submitted
then:
  the VNF instance is instantiated
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/instantiate"
        body(
                """
           {
            "instantiationLevelId": "g6h1df5",
            "additionalParams": {
               "cleanupResources": "true",
               "namespace": "my-namespace"
                 }
                }
                     """
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        bodyMatchers {
            jsonPath('$.instantiationLevelId', byRegex('[a-zA-Z0-9]+'))
            jsonPath('$.additionalParams', byRegex(/(.|\s)*/))
        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/5f43fb8e-1316-468a-9f9c-b375e5d82094")
        }
    }
    priority(2)
}
