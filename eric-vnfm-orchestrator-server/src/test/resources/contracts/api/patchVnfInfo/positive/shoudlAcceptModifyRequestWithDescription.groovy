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
Represents a successful scenario of Modifying a VNF Instance with all parameters

```
given:
  client requests to modify a VNF instance with description
when:
  a valid request is submitted
then:
  the VNF instance is modified
```

""")
    request {
        method 'PATCH'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}"
        body (
                """{
                    "vnfInstanceDescription" : "instantiated instance"
                }"""
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
        }
        bodyMatchers {
            jsonPath('$.vnfInstanceDescription', byRegex(/(.|\s)*/))

        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/5f43fb8e-1316-468a-9f9c-b375e5d82094")
        }
    }
    priority(1)
}
