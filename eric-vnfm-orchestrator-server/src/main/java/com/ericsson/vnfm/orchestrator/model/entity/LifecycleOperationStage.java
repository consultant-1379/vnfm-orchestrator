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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lifecycle_operation_stage")
public class LifecycleOperationStage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    @Id
    @Column(length = 64, nullable = false, name = "operation_id")
    private String operationId;

    @Column(name = "owner")
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "checkpoint")
    private CheckpointType checkpoint;

    @Column(columnDefinition = "owned_since")
    private LocalDateTime ownedSince;

    @Column(columnDefinition = "valid_until")
    private LocalDateTime validUntil;

    @OneToOne
    @JoinColumn(name = "operation_id")
    @MapsId
    private LifecycleOperation lifecycleOperation;

}
