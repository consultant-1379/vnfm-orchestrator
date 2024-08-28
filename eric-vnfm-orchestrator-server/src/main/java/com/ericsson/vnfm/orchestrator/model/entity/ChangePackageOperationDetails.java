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
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.ericsson.vnfm.orchestrator.model.ChangePackageOperationSubtype;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "change_package_operation_details")
public class ChangePackageOperationDetails implements Serializable {

    private static final long serialVersionUID = -2588603141042980059L;

    @Id
    @Column(unique = true, nullable = false, length = 64, name = "operation_occurrence_id")
    private String operationOccurrenceId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "operation_subtype", name = "operation_subtype")
    private ChangePackageOperationSubtype changePackageOperationSubtype;

    @Column(columnDefinition = "target_operation_occurrence_id")
    private String targetOperationOccurrenceId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
