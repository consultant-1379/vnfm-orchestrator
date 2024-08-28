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
package com.ericsson.vnfm.orchestrator.model.oss.enrollment;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CMPServer {
    private String name;
    private String uri;
    @JsonProperty("ca-certs")
    private String caCerts;
    @JsonProperty("certificate-authority")
    private String certificateAuthority;
    private int priority;
    @JsonProperty("ra-mode-enabled")
    private boolean raModeEnabled;
    @JsonProperty("ra-client-identity")
    private Map<String, String> raClientIdentity;
}
