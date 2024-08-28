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
package contracts.api.addNode.negative.notFound;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of adding a node 

```
given:
  client requests to add a node for a VNF Instance
when:
  a request with a vnfInstanceId is submitted
then:
  the request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/vnf_instances/non-existent-id/addNode'
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        body(
                """
           {
            "ossTopology.managedElementId": "anId", 
            "ossTopology.networkElementType": "networkElementTypeTest",
            "ossTopology.networkElementUsername": "networkElementUsernameTest",
            "ossTopology.networkElementPassword": "networkElementPasswordTest", 
            "ossTopology.nodeIpAddress": "nodeIpAddressTest",
            "ossTopology.snmpSecurityLevel": "AUTH_PRIV", 
            "ossTopology.snmpSecurityName": "snpmSecurityName",
            "ossTopology.netConfPort": "22"
                }
                     """
        )
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"Vnf instance with id ${fromRequest().path(3)} does not exist",
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

