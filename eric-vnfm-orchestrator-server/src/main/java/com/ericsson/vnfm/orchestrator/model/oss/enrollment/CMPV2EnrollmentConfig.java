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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CMPV2EnrollmentConfig {
    @JsonProperty("ca-certs")
    private List<CACertificate> certificates;
    @JsonProperty("certificate-authorities")
    private CertificateAuthority certificateAuthorities;
    @JsonProperty("cmp-server-groups")
    private CMPServerGroups cmpServerGroups;
    private List<Enrollment> enrollments;
    @JsonProperty("enrollment-retry-timeout")
    private int retryTimeout;
}
