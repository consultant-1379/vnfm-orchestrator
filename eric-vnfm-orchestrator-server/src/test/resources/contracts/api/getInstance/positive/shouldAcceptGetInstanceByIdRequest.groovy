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
package contracts.api.getInstance.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting a VNF Instance

```
given:
  client requests vnf instance details
when:
  a request with a vnfInstanceId is submitted
then:
  the request is accepted
```

""")
    request {
        method GET()
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        body(file("VnfResponse.json").asString().replaceAll("ID_TEMP", "${fromRequest().path(3).serverValue}"))

        headers {
            contentType(applicationJson())
        }
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.vnfInstanceName', byRegex(/[a-z]([-a-z0-9]*[a-z0-9])?/))
            jsonPath('$.vnfInstanceDescription', byRegex(/\w*/))
            jsonPath('$.vnfdId', byCommand("assertThat(parsedJson.read(\"\$.vnfdId\", String.class)).isNotNull()"))
            jsonPath('$.vnfProvider', byCommand("assertThat(parsedJson.read(\"\$.vnfProvider\", String.class)).isNotNull()"))
            jsonPath('$.vnfProductName', byCommand("assertThat(parsedJson.read(\"\$.vnfProductName\", String.class)).isNotNull()"))
            jsonPath('$.vnfSoftwareVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfSoftwareVersion\", String.class)).isNotNull()"))
            jsonPath('$.vnfdVersion', byCommand("assertThat(parsedJson.read(\"\$.vnfdVersion\", String.class)).isNotNull()"))
            jsonPath('$.vnfPkgId', byCommand("assertThat(parsedJson.read(\"\$.vnfPkgId\", String.class)).isNotNull()"))
            jsonPath('$.clusterName', byCommand("assertThat(parsedJson.read(\"\$.clusterName\", String.class)).isNotNull()"))
            jsonPath('$.instantiationState', byRegex("NOT_INSTANTIATED|INSTANTIATED"))
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
        }
    }
    priority(1)
}
