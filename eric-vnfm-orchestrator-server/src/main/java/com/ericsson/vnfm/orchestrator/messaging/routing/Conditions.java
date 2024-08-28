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
package com.ericsson.vnfm.orchestrator.messaging.routing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.ericsson.vnfm.orchestrator.messaging.MessagingLifecycleOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public final class Conditions {

    public static final Conditions HEAL_OPERATION_CONDITIONS =
            new Conditions(MessagingLifecycleOperationType.HEAL.toString(), HelmReleaseLifecycleMessage.class);

    private final String operationTypeName;

    private final Class<?> messageType;

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final Conditions that = (Conditions) object;

        return new EqualsBuilder()
                .append(operationTypeName, that.operationTypeName)
                .append(messageType, that.messageType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(operationTypeName)
                .append(messageType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("Conditions={")
                .append("operationTypeName='").append(operationTypeName).append('\'')
                .append(", messageType='").append(messageType.getName()).append('\'')
                .append("}")
                .toString();
    }
}
