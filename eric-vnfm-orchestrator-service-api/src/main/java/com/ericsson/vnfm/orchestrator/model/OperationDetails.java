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
package com.ericsson.vnfm.orchestrator.model;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationDetails {
    @JsonProperty("resourceInstanceName")
    private String resourceInstanceName;
    @JsonProperty("resourceID")
    private String resourceID;
    @JsonProperty("operation")
    private String operation;
    @JsonProperty("event")
    private String event;
    @JsonProperty("vnfSoftwareVersion")
    private String vnfSoftwareVersion;
    @JsonProperty("timestamp")
    private Date timestamp;
    @JsonProperty("error")
    private ProblemDetails error;
    @JsonProperty("vnfProductName")
    private String vnfProductName;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
