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
package contracts.api.getVnfLcmOpOccById.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of returning the VNF LCM Operation by OppOccId which includes changedInfo in response

```
given:
  client requests to get VNF LCM Operation
when:
  a valid LCM operation occurrence Id is passed
then:
  the VNF LCM Operation for that occurrence Id is returned
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/vnf_lcm_op_occs/${value(consumer('12345with_changed_info'), producer('12345with_changed_info'))}"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        body(
                """
                    {
                      "id": "12345",
                      "operationState": "STARTING",
                      "stateEnteredTime": "2019-05-15T13:19:53.488Z",
                      "startTime": "2019-05-15T13:19:53.488Z",
                      "vnfInstanceId": "string",
                      "grantId": null,
                      "operation": "MODIFY_INFO",
                      "isAutomaticInvocation": false,
                      "operationParams": {},
                      "isCancelPending": false,
                      "cancelMode": null,
                      "error": null,
                      "_links": {
                        "self": {
                          "href": "http://localhost/vnflcm/v1/vnf_lcm_op_occs/${fromRequest().path(3)}"
                        },
                        "vnfInstance": {
                          "href": "http://localhost/vnflcm/v1/vnf_instances/54321"
                        },
                        "grant": null,
                        "cancel": null,
                        "retry": null,
                        "rollback": null,
                        "fail": null
                      },
                      "changedInfo": {
                        "vnfInstanceName":  "test-vnf",
                        "vnfInstanceDescription": "description",
                        "vnfPkgId": "01234567",
                        "metadata": {"metadataKey":  "metadataValue"},
                        "extensions": {
                          "vnfControlledScaling": {"Aspect1": "CISMControlled","Aspect2": "ManualControlled"}
                        }
                      }
                    }
                """

        )
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.operationState', byRegex("STARTING|PROCESSING|COMPLETED|FAILED_TEMP|FAILED|ROLLING_BACK|ROLLED_BACK"))
            jsonPath('$.stateEnteredTime', byCommand("assertThat(parsedJson.read(\"\$.stateEnteredTime\", String.class)).isNotNull()"))
            jsonPath('$.startTime', byCommand("assertThat(parsedJson.read(\"\$.startTime\", String.class)).isNotNull()"))
            jsonPath('$.vnfInstanceId', byCommand("assertThat(parsedJson.read(\"\$.vnfInstanceId\", String.class)).isNotNull()"))
            jsonPath('$.grantId', byNull())
            jsonPath('$.operation', byRegex("INSTANTIATE|SCALE|SCALE_TO_LEVEL|CHANGE_FLAVOUR|TERMINATE|HEAL|OPERATE|CHANGE_EXT_CONN|MODIFY_INFO"))
            jsonPath('$.isAutomaticInvocation', byRegex("true|false"))
            jsonPath('$.operationParams', byNull())
            jsonPath('$.isCancelPending', byRegex("true|false"))
            jsonPath('$.cancelMode', byNull())
            jsonPath('$.error', byNull())
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$._links\", Object.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$._links.vnfInstance\", Object.class)).isNotNull()"))
            jsonPath('$._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$._links.vnfInstance.href\", String.class)).isNotNull()"))
            jsonPath('$.changedInfo', byCommand("assertThat(parsedJson.read(\"\$.changedInfo\", Object.class)).isNotNull()"))
            jsonPath('$.changedInfo.vnfInstanceName', byCommand("assertThat(parsedJson.read(\"\$.changedInfo.vnfInstanceName\", " +
                    "String.class)).isNotNull()"))
            jsonPath('$.changedInfo.vnfInstanceDescription', byCommand("assertThat(parsedJson.read(\"\$.changedInfo.vnfInstanceDescription\", " +
                    "String.class)).isNotNull()"))


        }
        headers {
            contentType(applicationJson())
        }
    }
    priority(1)
}
