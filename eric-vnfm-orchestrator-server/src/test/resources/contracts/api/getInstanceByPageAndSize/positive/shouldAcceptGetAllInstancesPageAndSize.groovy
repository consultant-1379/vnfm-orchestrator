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
package contracts.api.getInstanceByPageAndSize.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting all VNF Instances

```
given:
  client requests all vnf instance details
when:
  a request with is submitted
then:
  the request is accepted
```

""")
    request {
        method GET()
        urlPath("/vnflcm/v1/vnf_instances") {
            queryParameters {
                parameter 'nextpage_opaque_marker': '2'
                parameter 'size': '2'
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
            header("PaginationInfo", "number=2,size=2,totalPages=3,totalElements=5")
            header 'Link': value(
                    // a limitation in SCC, i.e. turning 'rel="first"', from the contract, into 'rel=\"first\"' in the contract test causes
                    // the contract test to fail so therefore we can only check the 'Link' header is not empty
                    producer(anyNonEmptyString()),
                    consumer('<https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_instances?nextpage_opaque_marker=1>; rel="first"' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_instances?nextpage_opaque_marker=1>; rel="prev"' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_instances?nextpage_opaque_marker=2>; rel="self",' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_instances?nextpage_opaque_marker=3>; rel="next",' +
                            ' <https://evnfm.rontgen000.seli.gic.ericsson.se/vnflcm/v1/vnf_instances?nextpage_opaque_marker=3>; rel="last"')
            )
        }
        body (file("AllVnfResponseDetailsPage2Of4.json"))
        bodyMatchers {
            jsonPath('$.[0]_links', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]_links\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]_links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.instantiate', byCommand("assertThat(parsedJson.read(\"\$[0].[*]._links.instantiate\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.terminate', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]_links.terminate\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]_links.self.href\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.terminate.href', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]._links.terminate.href\", Object.class)).isNotNull()"))
            jsonPath('$.[0]._links.instantiate.href', byCommand("assertThat(parsedJson.read(\"\$.[0].[*]._links.instantiate.href\", Object.class)).isNotNull()"))

            jsonPath('$.[1]_links', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]_links\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.self', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]_links.self\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.instantiate', byCommand("assertThat(parsedJson.read(\"\$[1].[*]._links.instantiate\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.terminate', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]_links.terminate\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.self.href', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]_links.self.href\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.terminate.href', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]._links.terminate.href\", Object.class)).isNotNull()"))
            jsonPath('$.[1]._links.instantiate.href', byCommand("assertThat(parsedJson.read(\"\$.[1].[*]._links.instantiate.href\", Object.class)).isNotNull()"))
        }
    }
}

