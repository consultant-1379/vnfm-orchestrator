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
package contracts.api.postChangePackageInfoVnf.negative.notInstantiated;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of changing package info for a VNF Instance

```
given:
  Client requests to change package info for  a VNF Instance
when:
  A request with a vnfInstanceId is submitted
then:
  The request is rejected with 409 CONFLICT
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+){0,1}-not-instantiated/)))}/change_vnfpkg"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                "vnfdId": "d3def1ce-4cf4-477c-aab3-21cb04e6a379"
        )
        bodyMatchers {
            jsonPath('$.vnfdId', byRegex('[a-z0-9]+(-[a-z0-9]+)*'))
        }
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"This resource is not in the INSTANTIATED state",
                          "status":409,
                          "detail":"VNF instance ID dummy_id is not in the INSTANTIATED state",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
