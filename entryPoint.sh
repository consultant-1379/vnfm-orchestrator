#!/bin/bash
#
# COPYRIGHT Ericsson 2024
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

JAVA_OPTS=$@

java -Dspring.profiles.active=prod -Djdk.tls.client.protocols="TLSv1.3,TLSv1.2" -Djava.security.egd=file:/dev/./urandom -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp $JAVA_OPTS -jar /eric-vnfm-orchestrator-service.jar