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
package contracts.api.addNode.positive


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of adding a node for a VNF Instance with a values.yaml file

```
given:
  client requests to add a node for a VNF instance with a valid values.yaml file
when:
  a valid request is submitted
then:
  the node is added for VNF instance specified
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/addNode"
        multipart(
                valuesFile: named(
                        name: $(consumer(regex(nonEmpty())),
                                producer('valid_oss_topology.yaml')),
                        content: $(consumer(regex(nonEmpty())),
                                producer(file('valid_oss_topology.yaml.properties')))
                )
        )
        headers {
            accept(applicationJson())
            contentType(multipartFormData())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status OK()

    }
    priority 3
}
