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
package com.ericsson.vnfm.orchestrator.model;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VnfResource {

    private String instanceId;
    private String vnfInstanceName;
    private String vnfInstanceDescription;
    private String vnfdId;
    private String vnfProvider;
    private String vnfProductName;
    private String vnfSoftwareVersion;
    private String vnfdVersion;
    private String vnfPkgId;
    private String clusterName;
    private String namespace;
    private String instantiationState;
    private boolean addedToOss;
    private Object instantiateOssTopology;
    private Object scalingInfo;
    private InstantiatedVnfInfo instantiatedVnfInfo;
    private List<VnfResourceLifecycleOperation> lcmOperationDetails;
    private boolean isDowngradeSupported;
    private Boolean isHealSupported;
    private String instantiationLevelId;
    private Object extensions;

    public Boolean isHealSupported() {
        return isHealSupported;
    }

    public void setHealSupported(final Boolean healSupported) {
        this.isHealSupported = healSupported;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
