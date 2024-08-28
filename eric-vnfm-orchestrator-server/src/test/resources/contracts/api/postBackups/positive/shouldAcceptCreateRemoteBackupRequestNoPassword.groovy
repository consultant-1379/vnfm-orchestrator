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
package contracts.api.postBackups.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of creating a remote backup without password for a VNF Instance

```
given:
  Client requests to create a remote backup for a VNF instance
when:
  A valid request is submitted
then:
  The return that the request was accepted
```

""")
    request {
        method 'POST'
        urlPath($(regex("/vnflcm/v1/vnf_instances/[a-z0-9]+(-[a-z0-9]+)*/backups")))
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """{
                               "additionalParams":{
                               }
                            }"""
        )
        bodyMatchers {
            jsonPath("\$.['additionalParams'].['scope']", byRegex(nonEmpty()).asString())
            jsonPath("\$.['additionalParams'].['backupName']", byRegex(nonEmpty()).asString())
            jsonPath("\$.['additionalParams'].['remote'].['host']", byRegex(url()).asString())
        }
    }
    response {
        status ACCEPTED()
    }
    priority 3

}
