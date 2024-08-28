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
import java.util.List;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EntityConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Builder
@EqualsAndHashCode(exclude = { "allOperations", "lastLifecycleOperation", "scaleInfoEntity" })
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vnf_resources_view")
public class VnfResourceView implements Serializable {
    private static final long serialVersionUID = 1;

    @Id
    @Column(name = "vnf_id")
    private String vnfInstanceId;

    @Column(length = 64, name = "vnf_instance_name")
    private String vnfInstanceName;

    @Column(length = 255, name = "vnf_instance_description")
    private String vnfInstanceDescription;

    @Column(nullable = false, length = 64, name = "vnfd_id")
    private String vnfDescriptorId;

    @Column(nullable = false, name = "vnf_provider")
    private String vnfProviderName;

    @Column(nullable = false, name = "vnf_product_name")
    private String vnfProductName;

    @Column(nullable = false, name = "vnf_software_version")
    private String vnfSoftwareVersion;

    @Column(nullable = false, name = "vnfd_version")
    private String vnfdVersion;

    @Column(nullable = false, name = "vnf_pkg_id")
    private String vnfPackageId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "instantiation_state")
    private InstantiationState instantiationState = InstantiationState.NOT_INSTANTIATED;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(name = "namespace")
    private String namespace;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_life_cycle_operation_id", referencedColumnName = "operation_occurrence_id")
    private LifecycleOperation lastLifecycleOperation;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinColumn(name = "vnf_instance_id")
    private List<LifecycleOperation> allOperations;

    @Convert(converter = EntityConverter.class)
    @Column(name = "oss_topology")
    private String ossTopology;

    @Convert(converter = EntityConverter.class)
    @Column(name = "instantiate_oss_topology")
    private String instantiateOssTopology;

    @Convert(converter = EntityConverter.class)
    @Column(name = "add_node_oss_topology")
    private String addNodeOssTopology;

    @Column(name = "added_to_oss")
    private boolean addedToOss;

    @Column(name = "policies")
    private String policies;

    @Column(name = "resource_details")
    private String resourceDetails;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "vnf_instance_id")
    private List<ScaleInfoEntity> scaleInfoEntity;

    @Column(name = "mano_controlled_scaling")
    private Boolean manoControlledScaling;

    @Convert(converter = EntityConverter.class)
    @Column(name = "temp_instance")
    private String tempInstance;

    @Column(name = "override_global_registry")
    private boolean overrideGlobalRegistry;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "alarm_supervision_status")
    private String alarmSupervisionStatus;

    @Column(name = "clean_up_resources")
    private boolean cleanUpResources;

    @Column(name = "is_heal_supported")
    @Accessors(prefix = "is")
    private Boolean isHealSupported;

    @Convert(converter = EntityConverter.class)
    @Column(name = "sitebasic_file")
    private String sitebasicFile;

    @Convert(converter = EntityConverter.class)
    @Column(name = "oss_node_protocol_file")
    private String ossNodeProtocolFile;

    @Convert(converter = EntityConverter.class)
    @Column(name = "sensitive_info")
    private String sensitiveInfo;

    @Column(name = "bro_endpoint_url")
    private String broEndpointUrl;

    @Column(name = "vnf_info_modifiable_attributes_extensions")
    private String vnfInfoModifiableAttributesExtensions;

    @Column(name = "instantiation_level")
    private String instantiationLevel;

    @Column(name = "crd_namespace")
    private String crdNamespace;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_operation_type")
    private LifecycleOperationType lifecycleOperationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_state")
    private LifecycleOperationState operationState;

    @Column(name = "last_state_changed")
    private LocalDateTime lastStateChanged;
}
