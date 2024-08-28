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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.GenericGenerator;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EntityConverter;
import com.ericsson.vnfm.orchestrator.model.ConfigFileStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "app_cluster_config_file")
public class ClusterConfigFile implements Serializable {

    private static final long serialVersionUID = 3037919543708220787L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(unique = true, nullable = false, length = 64, name = "id")
    private String id;

    @Column(nullable = false, name = "config_file_name", unique = true)
    private String name;

    @Convert(converter = EntityConverter.class)
    @Column(name = "config_file")
    private String content;

    @Column(name = "config_file_status")
    @Enumerated(EnumType.STRING)
    private ConfigFileStatus status;

    @Column(name = "config_file_description")
    private String description;

    @Column(name = "crd_namespace")
    private String crdNamespace;

    @Column(name = "cluster_server")
    private String clusterServer;

    @Column(name = "verification_namespace_uid")
    private String verificationNamespaceUid;

    @Column(name = "is_default")
    private boolean isDefault;

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("content", content)
                .append("status", status)
                .append("description", description)
                .append("crdNamespace", crdNamespace)
                .append("clusterServer", clusterServer)
                .append("is_default", isDefault)
                .toString();
    }
}
