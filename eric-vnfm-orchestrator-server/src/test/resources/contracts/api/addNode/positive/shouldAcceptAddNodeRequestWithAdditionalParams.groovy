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
Represents a successful scenario of adding a node for a VNF Instance

```
given:
  client requests to add a node for a VNF instance
when:
  a valid request is submitted
then:
  the node is added for VNF instance specified
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/addNode"
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
        bodyMatchers {
            jsonPath('$.managedElementId', byRegex(nonEmpty()).asString())
            jsonPath('$.networkElementType', byRegex(nonEmpty()).asString())
            jsonPath('$.networkElementUsername', byRegex(nonEmpty()).asString())
            jsonPath('$.networkElementPassword', byRegex(nonEmpty()).asString())
            jsonPath('$.nodeIpAddress', byRegex(nonEmpty()).asString())
            jsonPath('$.snmpSecurityLevel', byRegex(nonEmpty()).asString())
            jsonPath('$.snmpSecurityName', byRegex(nonEmpty()).asString())
            jsonPath('$.netConfPort', byRegex(nonEmpty()).asString())
        }}
    response {
        status OK()
    }
    priority(1)
}
