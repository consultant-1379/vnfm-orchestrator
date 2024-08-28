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
package contracts.api.postChangeVnfPkgInfo.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of changing package info for a VNF Instance with Extensions

```
given:
  Client requests to change package info for  a VNF instance with Extensions
when:
  A valid request is submitted
then:
  The VNF instance is changed
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/change_vnfpkg"
        body(
                """{
                    "vnfdId": "1234567",
                    "extensions" : {
                        "vnfControlledScaling" : {
                            "Aspect1" : "CISMControlled" ,
                            "Aspect2" : "ManualControlled"
                        },
                        "deployableModules":  {
                            "deployable_module_1": "enabled",
                            "deployable_module_2": "disabled"
                        }
                    }
                }"""
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        bodyMatchers {
            jsonPath('$.vnfdId', byRegex(nonEmpty()).asString())
            jsonPath('$.extensions.vnfControlledScaling.Aspect1', byRegex(/(ManualControlled|CISMControlled)/))
            jsonPath('$.extensions.vnfControlledScaling.Aspect2', byRegex(/(ManualControlled|CISMControlled)/))
        }
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/d807978b-13e2-478e-8694-5bedbf2145e2")
        }
    }
    priority (2)

}
