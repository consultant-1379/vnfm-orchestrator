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
package contracts.api.postInstantiateVnf.negative.instantiationLevelId;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Instantiating a VNF Instance

```
given:
  client requests to instantiate a VNF Instance with Instantiation Level Id not is not present in the vnfd
when:
  a request with an invalid instantiationLevelId is submitted
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/invalid-instantiationlevelid/instantiate"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """
           {
            "clusterName": "mycluster",
            "instantiationLevelId": "invalid-instantiation-level",
            "additionalParams": {
               "namespace": "my-namespace"
                 }
                }
                     """
        )
        bodyMatchers {
            jsonPath('$.instantiationLevelId', byRegex(nonEmpty()).asString())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Invalid Input Exception",
                          "status":400,
                          "detail":"InstantiationLevelId: invalid-instantiation-level not present in VNFD.",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}
