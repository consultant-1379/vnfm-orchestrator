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
package contracts.api.getBackups.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of getting a vnf resource matching the filter

```
given:
  client requests to get the list of all backups for an instance
when:
  a valid request is submitted
then:
  the list of all the backups for that instance is returned
```

""")
    request {
        method GET()
        url "/vnflcm/v1/vnf_instances/${value(consumer(regex(/[a-z0-9]+(-[a-z0-9]+)*/)))}/backups"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body("""[
                {
                    "id": "12345",
                    "name": "cnf-backup_3.2.0_20210120155030",
                    "creationTime": "2020-01-20T15:52:31.831",
                    "status": "COMPLETE",
                    "scope": "EVNFM"
                },
                {
                    "id": "12346",
                    "name": "cnf-backup_3.2.0_20210120165030",
                    "creationTime": "2020-01-20T16:52:31.831",
                    "status": "INCOMPLETE",
                    "scope": "EVNFM"
                },
                {
                    "id": "12347",
                    "name": "cnf-backup_3.2.0_20210120175030",
                    "creationTime": "2020-01-20T17:52:31.831",
                    "status": "CORRUPTED",
                    "scope": "EVNFM"
                }
        ]""")
        bodyMatchers{
            jsonPath('$[0].creationTime', byRegex(nonEmpty().asString()))
            jsonPath('$[1].creationTime', byRegex(nonEmpty().asString()))
            jsonPath('$[2].creationTime', byRegex(nonEmpty().asString()))
        }
    }
    priority(2)
}