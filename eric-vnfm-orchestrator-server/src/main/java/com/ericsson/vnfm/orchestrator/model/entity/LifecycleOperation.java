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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.GenericGenerator;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EntityConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(exclude = { "vnfInstance" })
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "app_lifecycle_operations")
public class LifecycleOperation implements Serializable {
    private static final long serialVersionUID = 4120321749223994563L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 64, nullable = false, name = "operation_occurrence_id")
    private String operationOccurrenceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vnf_instance_id", nullable = false)
    private VnfInstance vnfInstance;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "operation_state")
    private LifecycleOperationState operationState;

    @JsonIgnore
    @Column(columnDefinition = "state_entered_time")
    private LocalDateTime stateEnteredTime;

    @JsonIgnore
    @Column(columnDefinition = "start_time")
    private LocalDateTime startTime;

    @Column(columnDefinition = "grant_id")
    private String grantId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "lifecycle_operation_type")
    private LifecycleOperationType lifecycleOperationType;

    @Column(columnDefinition = "automatic_invocation")
    private boolean automaticInvocation;

    @Convert(converter = EntityConverter.class)
    @Column(columnDefinition = "operation_params")
    private String operationParams;

    @Column(columnDefinition = "cancel_pending")
    private boolean cancelPending;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "cancel_mode")
    private CancelModeType cancelMode;

    @Setter(AccessLevel.PACKAGE)
    @Column(columnDefinition = "error")
    private String error;

    @Convert(converter = EntityConverter.class)
    @Column(name = "values_file_params")
    private String valuesFileParams;

    @Column(name = "vnf_software_version")
    private String vnfSoftwareVersion;

    @Column(name = "vnf_product_name")
    private String vnfProductName;

    @Convert(converter = EntityConverter.class)
    @Column(name = "combined_values_file")
    private String combinedValuesFile;

    @Column(name = "combined_additional_params")
    private String combinedAdditionalParams;

    @Column(name = "resource_details")
    private String resourceDetails;

    @Column(name = "scale_info_entities")
    private String scaleInfoEntities;

    @Column(name = "source_vnfd_id")
    private String sourceVnfdId;

    @Column(name = "target_vnfd_id")
    private String targetVnfdId;

    @Column(name = "delete_node_failed")
    private boolean deleteNodeFailed;

    @Column(name = "delete_node_error_message")
    private String deleteNodeErrorMessage;

    @Column(name = "delete_node_finished")
    private boolean deleteNodeFinished;

    @Column(name = "application_timeout")
    private String applicationTimeout;

    @JsonIgnore
    @Column(columnDefinition = "expired_application_time")
    private LocalDateTime expiredApplicationTime;

    @Column(name = "set_alarm_supervision_error_message")
    private String setAlarmSupervisionErrorMessage;

    @Column(name = "downsize_allowed")
    private boolean downsizeAllowed;

    @Column(name = "is_auto_rollback_allowed")
    private boolean isAutoRollbackAllowed;

    @Column(name = "rollback_failure_pattern")
    private String failurePattern;

    @Column(name = "vnf_info_modifiable_attributes_extensions")
    private String vnfInfoModifiableAttributesExtensions;

    @Column(name = "instantiation_level")
    private String instantiationLevel;

    @Column(name = "rollback_pattern")
    private String rollbackPattern;

    @Column(name = "upgrade_pattern")
    private String upgradePattern;

    @Column(name = "username")
    private String username;

    @Column(name = "helm_client_version")
    private String helmClientVersion;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "lifecycleOperation", orphanRemoval = true)
    private LifecycleOperationStage lifecycleOperationStage;

    public CancelModeType getCancelModeType() {
        return cancelMode;
    }

    public void setCancelModeType(final CancelModeType cancelModeType) {
        this.cancelMode = cancelModeType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
