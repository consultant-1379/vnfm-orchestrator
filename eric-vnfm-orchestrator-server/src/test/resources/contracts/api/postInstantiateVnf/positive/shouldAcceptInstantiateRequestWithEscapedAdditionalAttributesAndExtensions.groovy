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
package contracts.api.postInstantiateVnf.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Instantiating a VNF Instance with escaped additional attributes and extensions

```
given:
  client requests to instantiate a VNF instance with escaped additional parameters and extensions
when:
  a valid request is submitted
then:
  the VNF instance is instantiated
```

""")
    request {
        method POST()
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/instantiate"
        body(
                file('escaped-additional-parameters-with-extensions.json')
        )
        bodyMatchers {
            jsonPath("\$.['additionalParams'].['json'].['escaped-1']", byRegex(nonBlank()).asString())
            jsonPath("\$.['additionalParams'].['json'].['escaped-2']", byRegex(nonBlank()).asString())
            jsonPath("\$.['additionalParams'].['json'].['escaped-3']", byRegex(nonBlank()).asString())
            jsonPath("\$.['additionalParams'].['json'].['escaped-4']", byRegex(nonBlank()).asString())
            jsonPath("\$.['additionalParams'].['xml']", byRegex(nonBlank()).asString())
            jsonPath("\$.['additionalParams'].['script']", byRegex(nonBlank()).asString())
            jsonPath("\$.['extensions'].['vnfControlledScaling'].['Aspect1']", byRegex(/(ManualControlled|CISMControlled)/))
            jsonPath("\$.['extensions'].['vnfControlledScaling'].['Aspect2']", byRegex(/(ManualControlled|CISMControlled)/))
            jsonPath("\$.['extensions'].['deployableModules'].['deployable_module_1']", byRegex(/(enabled|disabled)/))
            jsonPath("\$.['extensions'].['deployableModules'].['deployable_module_2']", byRegex(/(enabled|disabled)/))
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