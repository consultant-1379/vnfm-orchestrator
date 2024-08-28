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
package contracts.api.postRollbackOrFailOperation.positive;

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of rollback of an operation

```
given:
  Client requests to rollback an operation
when:
  A valid request is submitted
then:
  The operation is rolled back
```

""")
    request {
        method 'POST'
        url "/vnflcm/v1/vnf_lcm_op_occs/${value(consumer(anyNonEmptyString()))}/fail"
        headers {
            accept(applicationJson())
            header("Idempotency-key", consumer(regex("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")))
        }
    }
    response {
        status OK()
        body(
                """
                    {
                      "id": "12345",
                      "operationState": "FAILED",
                      "stateEnteredTime": "2019-05-15T13:19:53.488Z",
                      "startTime": "2019-05-15T13:19:53.488Z",
                      "vnfInstanceId": "string",
                      "grantId": null,
                      "operation": "INSTANTIATE",
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
                        "rollback":{
                          "href": "http://localhost/vnflcm/v1/vnf_lcm_op_occs/54321/rollback"
                        },
                        "fail": {
                          "href": "http://localhost/vnflcm/v1/vnf_lcm_op_occs/54321/fail"
                        }
                      }
                    }
                """
        )
        bodyMatchers {
            jsonPath('$.id', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$.operationState', byRegex("FAILED|ROLLING_BACK"))
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
            jsonPath('$._links', byCommand("assertThat(parsedJson.read(\"\$.id\", String.class)).isNotNull()"))
            jsonPath('$._links.self', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.self.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
            jsonPath('$._links.vnfInstance', byCommand("assertThat(parsedJson.read(\"\$._links.self\", Object.class)).isNotNull()"))
            jsonPath('$._links.vnfInstance.href', byCommand("assertThat(parsedJson.read(\"\$._links.self.href\", String.class)).isNotNull()"))
        }
    }
    priority 3
}
