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
package contracts.api.deleteVnfBackup.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failed scenario of deleting backup for a VNF Instance when VNFD host is not defined 

```
given:
  Client requests to delete backup for a VNF instance
when:
  A valid request is submitted but no VNFD host parameter found
then:
  The return that the request response was 400
```

""")
    request {
        method 'DELETE'
        url "/vnflcm/v1/vnf_instances/wf1ce-rd45-477c-vnf0-snapshot004/backups" +
                "/${value(consumer(regex(/[a-zA-Z0-9-_.]+/)))}/${value(consumer(regex(/[a-zA-Z0-9]+/)))}"
        headers {
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status UNPROCESSABLE_ENTITY()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Mandatory parameter missing",
                          "status":422,
                          "detail":"bro_endpoint_url is not defined in VNFD for instance. Please see documentation",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 2
}
