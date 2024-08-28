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
package contracts.api.postChangePackageInfoVnf.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of changing package info for a VNF Instance with a values.yaml file

```
given:
  client requests to changePackageInfo a VNF instance
when:
  a request is submitted with extra properties in the json part which are unknown to the service
then:
  the VNF instance is changed
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/change_package_info"
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        multipart(
                changePackageInfoVnfRequest: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('unknown-properties.json'))),
                        contentType: value("application/json")
                ),
                valuesFile: named(
                        name: value('values.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('values.yaml.properties')))
                )
        )
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/vnflcm/v1/vnf_lcm_op_occs/d807978b-13e2-478e-8694-5bedbf2145e2")
        }
    }
}
