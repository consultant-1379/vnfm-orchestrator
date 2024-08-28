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
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "helm_chart_history")
public class HelmChartHistoryRecord extends HelmChartBaseEntity implements Serializable {

    private static final long serialVersionUID = -8290415366716652915L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, name = "id")
    private UUID id;

    @Column(name = "life_cycle_operation_id")
    private String lifecycleOperationId;

    public HelmChartHistoryRecord(HelmChart helmChart, String lifecycleOperationId) {
        setHelmChartProperties(helmChart);
        this.lifecycleOperationId = lifecycleOperationId;
    }

    private void setHelmChartProperties(HelmChart helmChart) {
        this.setHelmChartUrl(helmChart.getHelmChartUrl());
        this.setPriority(helmChart.getPriority());
        this.setReleaseName(helmChart.getReleaseName());
        this.setRevisionNumber(helmChart.getRevisionNumber());
        this.setState(helmChart.getState());
        this.setRetryCount(helmChart.getRetryCount());
        this.setReplicaDetails(helmChart.getReplicaDetails());
        this.setChartEnabled(helmChart.isChartEnabled());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
