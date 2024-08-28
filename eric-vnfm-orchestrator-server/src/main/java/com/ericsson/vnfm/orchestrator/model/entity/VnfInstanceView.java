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
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vnf_instance_view")
public class VnfInstanceView implements Serializable {
    private static final long serialVersionUID = 1005905017997878691L;

    @Id
    @Column(nullable = false, length = 64, name = "vnf_id", insertable = false, updatable = false)
    private String vnfId;

    @Column(nullable = false, length = 64, name = "software_packages", insertable = false, updatable = false)
    private String softwarePackage;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
