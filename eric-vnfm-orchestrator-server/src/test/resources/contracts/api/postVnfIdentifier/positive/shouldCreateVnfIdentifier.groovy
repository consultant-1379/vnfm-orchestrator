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
package contracts.api.postVnfIdentifier.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Creating a VNF Identifier

```
given:
  client requests to create a VNF Identifier
when:
  a valid request is submitted
then:
  the vnf Identifier is created
```

""")
    request {
        method 'POST'
        url '/vnflcm/v1/vnf_instances'
        body(
                "vnfdId": "g08fcbc8-474f-4673-91ee-761fd83991e6",
                "vnfInstanceName": "vnf-instance",
                "vnfInstanceDescription": "Sample description about the vnf. Another description about the vnf."
        )
        headers {
            contentType(applicationJson())
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
        bodyMatchers {
            jsonPath('$.vnfdId', byRegex(nonEmpty()).asString())
            jsonPath('$.vnfInstanceName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.vnfInstanceDescription', byRegex(/(.|\s)*/))
        }
    }
    response {
        status CREATED()
        body(
                """
                    {
                      "id": "e3def1ce-4cf4-477c-aab3-21c454e6a389",
                      "vnfInstanceName": "${fromRequest().body('$.vnfInstanceName')}",
                      "vnfInstanceDescription": "${fromRequest().body('$.vnfInstanceDescription')}",
                      "vnfdId": "${fromRequest().body('$.vnfdId')}",
                      "vnfProvider": "Ericsson",
                      "vnfProductName": "SGSN-MME",
                      "vnfSoftwareVersion": "1.20",
                      "vnfdVersion": "1.20",
                      "vnfPkgId": "9392468011745350001",
                      "clusterName": null,
                      "instantiationState": "NOT_INSTANTIATED",
                      "instantiatedVnfInfo": {
                        "flavourId": "flavourId-not-supported",
                        "vnfState": "STOPPED",
                        "vnfcResourceInfo": [],
                        "scaleStatus": null,
                        "mcioInfo": null
                        },
                       "_links": {
                            "self": {
                                "href": "http://localhost/vnflcm/v1/vnf_instances/e3def1ce-4cf4-477c-aab3-21c454e6a389"
                            },
                            "instantiate": {
                                "href": "http://localhost/vnflcm/v1/vnf_instances/e3def1ce-4cf4-477c-aab3-21c454e6a389/instantiate"
                            },
                            "terminate": {
                                "href": "http://localhost/vnflcm/v1/vnf_instances/e3def1ce-4cf4-477c-aab3-21c454e6a389/terminate"
                            },
                            "scale": {
                                "href": "http://localhost/vnflcm/v1/vnf_instances/e3def1ce-4cf4-477c-aab3-21c454e6a389/scale"
                            },
                            "change_vnfpkg": {
                                "href": "http://localhost/vnflcm/v1/vnf_instances/e3def1ce-4cf4-477c-aab3-21c454e6a389/change_vnfpkg"
                            }
                        }
                    }
                """
        )
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.vnfInstanceName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.vnfInstanceDescription', byRegex(/(.|\s)*/))
            jsonPath('$.vnfdId', byCommand("assertThat(parsedJson.read(\"\$.vnfdId\", String.class)).isNotNull()"))
            jsonPath('$.vnfProvider', byCommand("assertThat(parsedJson.read(\"\$.vnfProvider\", String.class)).isNotNull()"))
            jsonPath('$.vnfProductName', byCommand("assertThat(parsedJson.read(\"\$.vnfProductName\", String.class)).isNotNull()"))
            jsonPath('$.vnfSoftwareVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfSoftwareVersion\", String.class)).isNotNull()"))
            jsonPath('$.vnfdVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfdVersion\", String.class)).isNotNull()"))
            jsonPath('$.vnfPkgId', byCommand("assertThat(parsedJson.read(\"\$.vnfPkgId\", String.class)).isNotNull()"))
            jsonPath('$.clusterName', byNull())
            jsonPath('$.instantiationState', byRegex("NOT_INSTANTIATED|INSTANTIATED"))
            jsonPath('$.instantiatedVnfInfo', byCommand("assertThat(parsedJson.read(\"\$.instantiatedVnfInfo\", Object.class)).isNotNull()"))
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.instantiate.href\", String.class)).isNotNull()"))
            jsonPath('$._links.instantiate', byCommand("assertThat(parsedJson.read(\"\$._links.instantiate\", Object.class)).isNotNull()"))
            jsonPath('$._links.instantiate.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$._links.terminate', byCommand("assertThat(parsedJson.read(\"\$._links.terminate\", Object.class)).isNotNull()"))
            jsonPath('$._links.terminate.href', byCommand("assertThat(parsedJson.read(\"\$._links.terminate.href\", String.class)).isNotNull()"))
            jsonPath('$._links.scale', byCommand("assertThat(parsedJson.read(\"\$._links.scale\", Object.class)).isNotNull()"))
            jsonPath('$._links.scale.href', byCommand("assertThat(parsedJson.read(\"\$._links.scale.href\", String.class)).isNotNull()"))
            jsonPath('$._links.change_vnfpkg', byCommand("assertThat(parsedJson.read(\"\$._links.change_vnfpkg\", Object.class)).isNotNull()"))
            jsonPath('$._links.change_vnfpkg.href', byCommand("assertThat(parsedJson.read(\"\$._links.change_vnfpkg.href\", String.class)).isNotNull()"))
        }
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}
