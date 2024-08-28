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
package com.ericsson.vnfm.orchestrator.repositories.testData;

import java.util.List;

import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;

public class VnfResourceViewTestData {

    public static List<VnfResourceView> buildVnfResourceViews() {
        return List.of(buildFirstVnfResourceView(),
                       buildSecondVnfResourceView(),
                       buildThirdVnfResourceView());
    }

    private static VnfResourceView buildFirstVnfResourceView() {
        return buildVnfResourceView("250");
    }

    private static VnfResourceView buildSecondVnfResourceView() {
        return buildVnfResourceView("251");
    }

    private static VnfResourceView buildThirdVnfResourceView() {
        return buildVnfResourceView("252");
    }

    private static VnfResourceView buildVnfResourceView(String idSuffix) {
        return VnfResourceView.builder()
                .vnfInstanceId("d3def1ce-4cf4-477c-aab3-21c454e6a" + idSuffix)
                .vnfInstanceName("testVnfInstanceName" + idSuffix)
                .vnfInstanceDescription("testVnfInstanceDescription" + idSuffix)
                .vnfDescriptorId("testVnfInstanceDescriptorId" + idSuffix)
                .vnfProviderName("testVnfProviderName" + idSuffix)
                .vnfProductName("testVnfProductName" + idSuffix)
                .vnfSoftwareVersion("testVnfSoftwareVersion" + idSuffix)
                .vnfdVersion("testVnfdVersion" + idSuffix)
                .vnfPackageId("testVnfPackageId" + idSuffix)
                .instantiationState(InstantiationState.NOT_INSTANTIATED)
                .clusterName("testClusterName" + idSuffix)
                .namespace("testNamespace" + idSuffix)
                .addedToOss(true)
                .policies("testPolicies" + idSuffix)
                .resourceDetails("testResourceDetails" + idSuffix)
                .manoControlledScaling(true)
                .overrideGlobalRegistry(true)
                .metadata("{\"testMetadata" + idSuffix + "\":\"test\"}")
                .alarmSupervisionStatus("testAlarmSupervisionStatus" + idSuffix)
                .cleanUpResources(true)
                .healSupported(true)
                .broEndpointUrl("testBroEndpointUrl" + idSuffix)
                .vnfInfoModifiableAttributesExtensions("{\"testVnfInfoModifiableAttributesExtensions" + idSuffix + "\":\"test\"}")
                .instantiationLevel("testInstantiationLevel" + idSuffix)
                .crdNamespace("testCrdNamespace" + idSuffix)
                .build();
    }
}
