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
package contracts.api.getValidateNamespace.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of namespace validation

```
given:
  client requests to validate namespace
when:
  a valid request is submitted
then:
  400 BAD REQUEST with error details is returned
```

""")
    request {
        method 'GET'
        url "/vnflcm/v1/validateNamespace/${value(consumer(anyNonEmptyString()), producer('default'))}/${value(consumer(anyNonEmptyString()), producer('cluster0'))}"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        body("""
                    {
                        "type":"about:blank",
                        "title":"Malformed Request",
                        "status":400,
                        "detail":"Cannot instantiate in any of the Kubernetes initialized namespaces : [default, kube-system, kube-public, kube-node-lease]",
                        "instance":"about:blank"
                    }
                """)
    }
    priority(2)
}