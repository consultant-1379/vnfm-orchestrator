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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode(exclude = {"lifecycleOperation"})
@Table(name = "changed_info")
public class ChangedInfo implements Serializable {

    private static final long serialVersionUID = -4970068816134607071L;

    @Id
    @Column(unique = true, nullable = false, length = 64, name = "id")
    private String id;

    @Column(length = 64, name = "vnf_pkg_id")
    private String vnfPkgId;

    @Column(length = 64, name = "vnf_instance_name")
    private String vnfInstanceName;

    @Column(name = "vnf_instance_description")
    private String vnfInstanceDescription;

    @Column(name = "metadata")
    private String metadata;

    @Column(length = 64, name = "vnfd_id")
    private String vnfDescriptorId;

    @Column(length = 64, name = "vnf_provider")
    private String vnfProviderName;

    @Column(length = 64, name = "vnf_product_name")
    private String vnfProductName;

    @Column(length = 64, name = "vnf_software_version")
    private String vnfSoftwareVersion;

    @Column(length = 64, name = "vnfd_version")
    private String vnfdVersion;

    @Column(name = "vnf_info_modifiable_attributes_extensions")
    private String vnfInfoModifiableAttributesExtensions;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    @MapsId
    private LifecycleOperation lifecycleOperation;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
