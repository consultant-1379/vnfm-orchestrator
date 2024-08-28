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

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonDeserialize(builder = ReplicaDetails.Builder.class)
public final class ReplicaDetails {

    private String minReplicasParameterName;
    private Integer minReplicasCount;
    private String maxReplicasParameterName;
    private Integer maxReplicasCount;
    private String scalingParameterName;
    private Integer currentReplicaCount;
    private String autoScalingEnabledParameterName;
    private Boolean autoScalingEnabledValue;

    private ReplicaDetails(final Builder builder) {
        setMinReplicasParameterName(builder.minReplicasParameterName);
        setMinReplicasCount(builder.minReplicasCount);
        setMaxReplicasParameterName(builder.maxReplicasParameterName);
        setMaxReplicasCount(builder.maxReplicasCount);
        setScalingParameterName(builder.scalingParameterName);
        setCurrentReplicaCount(builder.currentReplicaCount);
        setAutoScalingEnabledParameterName(builder.autoScalingEnabledParameterName);
        setAutoScalingEnabledValue(builder.autoScalingEnabledValue);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String minReplicasParameterName;
        private Integer minReplicasCount;
        private String maxReplicasParameterName;
        private Integer maxReplicasCount;
        private String scalingParameterName;
        private Integer currentReplicaCount;
        private String autoScalingEnabledParameterName;
        private Boolean autoScalingEnabledValue;

        private Builder() {
        }

        public Builder withMinReplicasParameterName(final String minReplicasParameterName) {
            this.minReplicasParameterName = minReplicasParameterName;
            return this;
        }

        public Builder withMinReplicasCount(final Integer minReplicasCount) {
            this.minReplicasCount = minReplicasCount;
            return this;
        }

        public Builder withMaxReplicasParameterName(final String maxReplicasParameterName) {
            this.maxReplicasParameterName = maxReplicasParameterName;
            return this;
        }

        public Builder withMaxReplicasCount(final Integer maxReplicasCount) {
            this.maxReplicasCount = maxReplicasCount;
            return this;
        }

        public Builder withScalingParameterName(final String scalingParameterName) {
            this.scalingParameterName = scalingParameterName;
            return this;
        }

        public Builder withCurrentReplicaCount(final Integer currentReplicaCount) {
            this.currentReplicaCount = currentReplicaCount;
            return this;
        }

        public Builder withAutoScalingEnabledParameterName(final String autoScalingEnabledParameterName) {
            this.autoScalingEnabledParameterName = autoScalingEnabledParameterName;
            return this;
        }

        public Builder withAutoScalingEnabledValue(final Boolean autoScalingEnabledValue) {
            this.autoScalingEnabledValue = autoScalingEnabledValue;
            return this;
        }

        public ReplicaDetails build() {
            return new ReplicaDetails(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReplicaDetails that = (ReplicaDetails) o;
        return Objects.equals(minReplicasParameterName, that.minReplicasParameterName) &&
            Objects.equals(minReplicasCount, that.minReplicasCount) &&
            Objects.equals(maxReplicasParameterName, that.maxReplicasParameterName) &&
            Objects.equals(maxReplicasCount, that.maxReplicasCount) &&
            Objects.equals(scalingParameterName, that.scalingParameterName) &&
            Objects.equals(currentReplicaCount, that.currentReplicaCount) &&
            Objects.equals(autoScalingEnabledParameterName, that.autoScalingEnabledParameterName) &&
            Objects.equals(autoScalingEnabledValue, that.autoScalingEnabledValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minReplicasParameterName, minReplicasCount, maxReplicasParameterName, maxReplicasCount,
                            scalingParameterName, currentReplicaCount, autoScalingEnabledParameterName,
                            autoScalingEnabledValue);
    }
}
