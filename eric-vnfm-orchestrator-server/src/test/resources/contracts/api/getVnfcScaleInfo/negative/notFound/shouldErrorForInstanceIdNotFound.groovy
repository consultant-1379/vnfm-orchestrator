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
package contracts.api.postScaleVnf.negative.notFound

import org.springframework.cloud.contract.spec.Contract


Contract.make {
    description("""
Represents an error scenario for requesting VNFC scale info

```
given:
  Client requests to get the VNFC scale info of a vnf resource which does not exist
when:
  A request with a non existent resource Id is submitted
then:
  The request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'GET'
        urlPath($(regex("/vnflcm/v1/resources/NOT_FOUND/vnfcScaleInfo"))){
            queryParameters {
                parameter 'type': value(consumer(regex(/(SCALE_OUT|SCALE_IN)/)),
                        producer("SCALE_OUT"))
                parameter 'aspectId': value(consumer(regex(nonEmpty()).asString()), producer("Payload"))
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"about:blank",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"${fromRequest().path(3)} vnf resource not present",
                          "instance":"http://localhost/vnflcm/v1/vnf_instances/${fromRequest().path(3)}"
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
