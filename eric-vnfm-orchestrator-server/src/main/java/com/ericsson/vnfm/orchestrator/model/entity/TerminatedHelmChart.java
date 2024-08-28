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

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"vnfInstance"}, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Entity
@Table(name = "terminated_helm_chart")
public final class TerminatedHelmChart extends HelmChartBaseEntity implements Serializable {

    private static final long serialVersionUID = 1492702944529224971L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "id")
    @ToString.Include
    private String id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "vnf_id", nullable = false, referencedColumnName = "vnf_id")
    private VnfInstance vnfInstance;

    @Column(name = "helm_chart_name")
    @ToString.Include
    private String helmChartName;

    @Column(name = "helm_chart_version")
    @ToString.Include
    private String helmChartVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "helm_chart_type")
    @ToString.Include
    private HelmChartType helmChartType;

    @Column(name = "helm_chart_artifact_key")
    @ToString.Include
    private String helmChartArtifactKey;

    @Column(length = 64, name = "life_cycle_operation_id")
    @ToString.Include
    private String operationOccurrenceId;
}
