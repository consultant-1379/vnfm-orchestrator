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
package contracts.api.getAllVnfLcmOpOcc.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting all VNF LCM Operation Occurrences

```
given:
  client requests all VNF LCM Operation Occurrences
when:
  a request is submitted
then:
  the request is accepted
```

""")
    request {
        method GET()
        url "/vnflcm/v1/vnf_lcm_op_occs"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
            header("PaginationInfo", "number=1,size=100,totalPages=1,totalElements=5")
            header 'Link': value(
                    // a limitation in SCC, i.e. turning 'rel="first"', from the contract, into 'rel=\"first\"' in the contract test causes
                    // the contract test to fail so therefore we can only check the 'Link' header is not empty
                    producer(anyNonEmptyString()),
                    consumer('<https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_lcm_op_occs?nextpage_opaque_marker=1>; rel="first"' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_lcm_op_occs?nextpage_opaque_marker=1>; rel="self",' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_lcm_op_occs?nextpage_opaque_marker=1>; rel="last"')
            )
        }
        body (file("AllVnfLcmOpOccDetails.json"))
        bodyMatchers {
            jsonPath('$.[0].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[0].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[0].startTime', byCommand("assertThat(parsedJson.read(\"\$.[0].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[0]._links', byCommand("assertThat(parsedJson.read(\"\$.[0]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[0]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[0]._links.self.href\", String.class)).isNotNull()"))

            jsonPath('$.[1].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[1].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[1].startTime', byCommand("assertThat(parsedJson.read(\"\$.[1].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[1]._links', byCommand("assertThat(parsedJson.read(\"\$.[1]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[1]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[1]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[1]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[1]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[1]._links.self.href\", String.class)).isNotNull()"))

            jsonPath('$.[2].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[2].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[2].startTime', byCommand("assertThat(parsedJson.read(\"\$.[2].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[2]._links', byCommand("assertThat(parsedJson.read(\"\$.[2]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[2]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[2]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[2]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[2]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[2]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[2]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[2]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[2]._links.self.href\", String.class)).isNotNull()"))

            jsonPath('$.[3].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[3].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[3].startTime', byCommand("assertThat(parsedJson.read(\"\$.[3].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[3]._links', byCommand("assertThat(parsedJson.read(\"\$.[3]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[3]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[3]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[3]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[3]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[3]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[3]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[3]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[3]._links.self.href\", String.class)).isNotNull()"))

            jsonPath('$.[4].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[4].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[4].startTime', byCommand("assertThat(parsedJson.read(\"\$.[4].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[4]._links', byCommand("assertThat(parsedJson.read(\"\$.[4]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[4]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[4]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[4]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[4]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[4]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[4]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[4]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[4]._links.self.href\", String.class)).isNotNull()"))

            jsonPath('$.[5].stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.[5].stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.[5].startTime', byCommand("assertThat(parsedJson.read(\"\$.[5].startTime\", String.class)).isNotNull()"))
            jsonPath('$.[5]._links', byCommand("assertThat(parsedJson.read(\"\$.[5]._links\", Object.class)).isNotNull()"))
            jsonPath('$.[5]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[5]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[5]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[5]._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$.[5]._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$.[5]._links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[5]._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$.[5]._links.self.href\", String.class)).isNotNull()"))
        }
    }
    priority(1)
}

