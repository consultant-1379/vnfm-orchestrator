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
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.ericsson.am.shared.vnfd.model.OperationDetail;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EntityConverter;
import com.ericsson.vnfm.orchestrator.model.converter.OperationDetailListConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Builder
@EqualsAndHashCode(exclude = {"allOperations", "helmCharts", "terminatedHelmCharts", "scaleInfoEntity", "supportedOperations"})
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_vnf_instance")
@JsonIgnoreProperties(ignoreUnknown = true)
public class VnfInstance implements Serializable {
    private static final long serialVersionUID = 6913334832577624193L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "vnf_id")
    @ToString.Include
    private String vnfInstanceId;

    @Column(length = 64, name = "vnf_instance_name")
    @ToString.Include
    private String vnfInstanceName;

    @Column(length = 255, name = "vnf_instance_description")
    private String vnfInstanceDescription;

    @Column(nullable = false, length = 64, name = "vnfd_id")
    @ToString.Include
    private String vnfDescriptorId;

    @Column(nullable = false, length = 64, name = "vnf_provider")
    private String vnfProviderName;

    @Column(nullable = false, length = 64, name = "vnf_product_name")
    private String vnfProductName;

    @Column(nullable = false, length = 64, name = "vnf_software_version")
    private String vnfSoftwareVersion;

    @Column(nullable = false, length = 64, name = "vnfd_version")
    private String vnfdVersion;

    @Column(nullable = false, length = 64, name = "vnf_pkg_id")
    @ToString.Include
    private String vnfPackageId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "instantiation_state")
    private InstantiationState instantiationState = InstantiationState.NOT_INSTANTIATED;

    @Column(length = 64, name = "cluster_name")
    @ToString.Include
    private String clusterName;

    @Column(length = 64, name = "namespace")
    @ToString.Include
    private String namespace;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "vnfInstance", orphanRemoval = true)
    private List<HelmChart> helmCharts;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "vnfInstance", orphanRemoval = true)
    private List<TerminatedHelmChart> terminatedHelmCharts;

    @Column(length = 64, name = "current_life_cycle_operation_id")
    @ToString.Include
    private String operationOccurrenceId;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "vnfInstance")
    private List<LifecycleOperation> allOperations;

    @Convert(converter = OperationDetailListConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<OperationDetail> supportedOperations; // NOSONAR

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

    @Convert(converter = EntityConverter.class)
    @Column(name = "combined_values_file")
    private String combinedValuesFile;

    @Column(name = "combined_additional_params")
    private String combinedAdditionalParams;

    @Column(name = "policies")
    private String policies;

    @Column(name = "resource_details")
    @ToString.Include
    private String resourceDetails;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "vnfInstance")
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
    @ToString.Include
    private String instantiationLevel;

    @Column(name = "crd_namespace")
    private String crdNamespace;

    @Column(name = "is_rel4")
    @ToString.Include
    private boolean isRel4;

    @Column(name = "is_deployable_modules_supported")
    private boolean isDeployableModulesSupported;

    @Column(name = "helm_client_version")
    private String helmClientVersion;

    public String[] getAllBashInjectionCheckedValuesAsArray() {
        return new String[]{vnfInstanceId, vnfInstanceDescription};
    }

    public Boolean isHealSupported() {
        return isHealSupported;
    }

    public void setHealSupported(final Boolean healSupported) {
        isHealSupported = healSupported;
    }

    public String getCrdNamespace() {
        return crdNamespace;
    }

    public void setCrdNamespace(String crdNamespace) {
        this.crdNamespace = crdNamespace;
    }

    @PrePersist
    @PreUpdate
    private void setIsHealSupported() {
        isHealSupported = isHealSupported != null && isHealSupported;
    }
}
