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
package contracts.api.postChangeVnfPkgInfo.negative.upgradeExtensions;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of change package info for a VNF Instance with extensions

```
given:
  client requests to change package info for a VNF instance
when:
  a request with an invalid extension is submitted
then:
  the request is rejected with 400 Bad Request
```

""")
    request {
        method 'POST'
        urlPath"/vnflcm/v1/vnf_instances/invalid-extensions-upgrade/change_vnfpkg"
        multipart(
                changeCurrentVnfPkgRequest: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-json-part.json'))),
                        contentType: value("application/json")
                ),
                valuesFile: named(
                        name: value('values.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('values.yaml.properties')))
                )
        )
        headers {
            contentType(multipartFormData())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                           "type":"about:blank",
                           "title":"Malformed Request",
                           "status":400,
                           "detail":"Extensions should be key value pair.",
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
