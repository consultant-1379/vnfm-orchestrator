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
package contracts.api.addNode.negative.invalidValuesFile;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of adding a node 

```
given:
  client requests to add a node for a VNF Instance
when:
  a request with a vnfInstanceId is submitted using an empty values file
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/addNode"
        multipart(
                valuesFile: named(
                        name: $(consumer('invalid.yaml.properties'),
                                producer('invalid.yaml')),
                        content: $(consumer(regex(nonEmpty())),
                                producer(file('invalid.yaml.properties'))
                        )
                ))
        headers {
            accept(applicationJson())
            contentType(multipartFormData())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }

    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Invalid Values File",
                          "status":400,
                          "detail":"Values file contains invalid YAML. ",
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

