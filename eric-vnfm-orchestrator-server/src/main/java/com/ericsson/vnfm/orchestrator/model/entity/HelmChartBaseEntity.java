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
package com.ericsson.vnfm.orchestrator.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@MappedSuperclass
public class HelmChartBaseEntity {

    @Column(name = "helm_chart_url")
    @ToString.Include
    private String helmChartUrl;

    @Column(name = "priority")
    private int priority;

    @Column(name = "release_name")
    @ToString.Include
    private String releaseName;

    @Column(name = "revision_number")
    private String revisionNumber;

    @Column(name = "state")
    private String state;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "delete_pvc_state")
    private String deletePvcState;

    @Column(name = "downsize_state")
    private String downsizeState;

    @Column(name = "replica_details")
    @ToString.Include
    private String replicaDetails;

    @Builder.Default
    @Column(name = "is_chart_enabled")
    private boolean isChartEnabled = true;
}
