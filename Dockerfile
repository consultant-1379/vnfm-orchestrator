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

ARG BASE_IMAGE_VERSION
FROM armdocker.rnd.ericsson.se/proj-am/sles/sles-corretto-openjdk17:${BASE_IMAGE_VERSION}

ARG GIT_COMMIT=""
ARG APP_VERSION=""
ARG BUILD_TIME=""
# User Id generated based on ADP rule DR-D1123-122
ARG uid=157772
ARG gid=157772

ENV JAVA_OPTS ""

LABEL product.number="CXU 101 0679" \
      product.revision="R1A" \
      GIT_COMMIT=$GIT_COMMIT \
      com.ericsson.product-name="EVNFM LCM Service" \
      com.ericsson.product-number="CXU 101 0679" \
      com.ericsson.product-revision="R1A" \
      org.opencontainers.image.title="EVNFM LCM Service" \
      org.opencontainers.image.created=${BUILD_TIME} \
      org.opencontainers.image.revision=${GIT_COMMIT} \
      org.opencontainers.image.version=${APP_VERSION} \
      org.opencontainers.image.vendor="Ericsson"

RUN echo "${uid}:x:${uid}:${gid}:orchestrator-user:/:/bin/bash" >> /etc/passwd
RUN sed -i '/root/s/bash/false/g' /etc/passwd

ADD eric-vnfm-orchestrator-server/target/eric-vnfm-orchestrator-service.jar eric-vnfm-orchestrator-service.jar

COPY entryPoint.sh /entryPoint.sh

RUN sh -c 'touch /eric-vnfm-orchestrator-service.jar' \
    && chmod 755 /entryPoint.sh \
    && zypper install -y libcap-progs

USER ${uid}:${gid}

ENTRYPOINT ["sh", "-c", "/entryPoint.sh $JAVA_OPTS"]

EXPOSE 8888