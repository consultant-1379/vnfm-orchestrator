/*
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
 */
package com.ericsson.vnfm.orchestrator.model.backup;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BroActionRequest {

    private String action;
    private Payload payload;

    public static class BroActionRequestBuilder {
        private String action;
        private String backupName;
        private String uri;
        private String password;

        public BroActionRequestBuilder(String action) {
            this.action = action;
        }

        public BroActionRequestBuilder withBackupName(String backupName) {
            this.backupName = backupName;
            return this;
        }

        public BroActionRequestBuilder withUri(String uri) {
            this.uri = uri;
            return this;
        }

        public BroActionRequestBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public BroActionRequest buildRequest() {
            BroActionRequest broActionRequest = new BroActionRequest();
            broActionRequest.setAction(action);
            Payload payload = new Payload();
            payload.setPassword(password);
            payload.setUri(uri);
            payload.setBackupName(backupName);
            broActionRequest.setPayload(payload);

            return broActionRequest;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
