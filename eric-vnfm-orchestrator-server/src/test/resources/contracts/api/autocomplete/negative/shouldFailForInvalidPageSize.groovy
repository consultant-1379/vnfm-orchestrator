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
package contracts.api.autocomplete.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
An invalid scenario when invalid page number is provided

```
given:
  client requests to retrieve filter data
when:
  a invalid page size is provided
then:
  error response is returned
```

""")
    request{
        method GET()
        urlPath("/vnflcm/api/v1/instance/filter/autocomplete") {
            queryParameters {
                parameter 'pageSize': value(consumer(regex("^([^0-9]*)\$")), producer("test"))
            }
        }
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                """
                        {
                            "type":"about:blank",
                            "title":"Invalid Input Exception",
                            "status":400,
                            "detail":"pageSize only supports number value",
                            "instance":"about:blank"
                        }
                """
        )
    }
}
