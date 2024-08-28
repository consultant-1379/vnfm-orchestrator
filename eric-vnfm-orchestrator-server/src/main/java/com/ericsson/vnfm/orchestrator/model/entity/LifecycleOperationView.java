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
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lifecycle_operation_view")
public class LifecycleOperationView implements Serializable {
    private static final long serialVersionUID = 1;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "operation_occurrence_id")
    private String operationOccurrenceId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "operation_state")
    private LifecycleOperationState operationState;

    @Column(columnDefinition = "error")
    private String error;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "lifecycle_operation_type")
    private LifecycleOperationType lifecycleOperationType;

    @JsonIgnore
    @Column(columnDefinition = "start_time")
    private LocalDateTime startTime;

    @JsonIgnore
    @Column(columnDefinition = "state_entered_time")
    private LocalDateTime stateEnteredTime;

    @Column(name = "vnf_product_name")
    private String vnfProductName;

    @Column(name = "vnf_software_version")
    private String vnfSoftwareVersion;

    @Column(name = "vnf_id")
    private String vnfInstanceId;

    @Column(length = 64, name = "vnf_instance_name")
    private String vnfInstanceName;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(length = 64, name = "namespace")
    private String namespace;

    @Column(name = "username")
    private String username;
}
