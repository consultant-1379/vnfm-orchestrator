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
package com.ericsson.vnfm.orchestrator.e2e.granting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static com.ericsson.vnfm.orchestrator.TestUtils.getHelmChartByName;
import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN;
import static com.ericsson.vnfm.orchestrator.e2e.granting.GrantingTestUtils.NFVO_TOKEN_PARAM;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getHelmReleaseLifecycleMessage;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getLifeCycleOperationId;
import static com.ericsson.vnfm.orchestrator.e2e.util.EndToEndTestUtils.getWfsEventMessage;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.COMPLETED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.FAILED;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLED_BACK;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.ROLLING_BACK;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.PERSIST_SCALE_INFO;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Rollback.IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY;
import static com.ericsson.vnfm.orchestrator.presentation.controllers.vnf.instances.VnfInstancesControllerImplIntegrationTest.CHANGE_VNFPKG;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import com.ericsson.vnfm.orchestrator.e2e.util.AbstractEndToEndTest;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.GrantingNotificationsConfig;
import com.ericsson.vnfm.orchestrator.infrastructure.configurations.OnboardingConfig;
import com.ericsson.vnfm.orchestrator.model.ChangeCurrentVnfPkgRequest;
import com.ericsson.vnfm.orchestrator.model.ProblemDetails;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.OperationsInProgressRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingRoutingClient;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseOperationType;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventStatus;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.WorkflowServiceEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;


@TestPropertySource("test-granting.properties")
@Import(GrantingTestConfig.class)
public class EndToEndChangePackageWithGrantingTest extends AbstractEndToEndTest {

    private static final String DB_VNF_ID_1 = "186dc69a-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_2 = "186dc8db-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_3 = "186dc9fc-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_4 = "186dc3fd-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_5 = "186dc6de-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_6 = "186dccff-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_7 = "186dc69a-0c2f-11ed-861d-0242ac120003";
    private static final String DB_VNF_ID_8 = "186dc69a-0c2f-11ed-861d-0242ac120004";
    private static final String DB_VNF_ID_9 = "186dc69a-0c2f-11ed-861d-0242ac120005";
    private static final String DB_VNF_ID_10 = "186dc69a-0c2f-11ed-861d-0242ac120006";
    private static final String DB_VNF_ID_11 = "186dc69a-0c2f-11ed-861d-0242ac120007";
    private static final String DB_VNF_ID_12 = "186dc69a-0c2f-11ed-861d-0242ac120008";
    private static final String DB_VNF_ID_13 = "3569c69a-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_14 = "3569c69b-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_15 = "3569c69c-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_16 = "3569c69d-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_17 = "3569c69e-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_18 = "3569c69f-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_19 = "3569c69g-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_20 = "3569c69h-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_21 = "3569c69i-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_22 = "3569c69j-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_23 = "3569c69k-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_24 = "3569c70a-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_25 = "3569c70b-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_26 = "3569c70c-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_27 = "3569c70d-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_28 = "3569c70e-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_29 = "3569c70f-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_30 = "3569c70g-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_31 = "3569c70h-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_32 = "3569c70i-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_33 = "3569c70j-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_34 = "3569c70k-0c2f-11ed-861d-0242ac120002";
    private static final String DB_VNF_ID_35 = "3569c70l-0c2f-11ed-861d-0242ac120002";

    private static final String RELEASE_NAME_SUCCESS = "my-release-name-granting-upgrade-success-1";
    private static final String RELEASE_NAME_SUCCESS_SCALE_CCVP = "my-release-name-granting-upgrade-after-scale-with-persist-scale-info-1";
    private static final String RELEASE_NAME_SUCCESS_MIXED_VDUS_CCVP = "my-release-name-granting-upgrade-with-mixed-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-upgrade-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_MIXED_VDUS_CCVP = "my-release-name-granting-upgrade-rel3-with-mixed-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-upgrade-rel3-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL4_TO_REL3_MIXED_VDUS_CCVP = "my-release-name-granting-upgrade-rel4-to-rel3-with-mixed-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL4_TO_REL3_NON_SCALABLE_VDUS_CCVP =
            "my-release-name-granting-upgrade-rel4-to-rel3-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_TO_REL4_MIXED_VDUS_CCVP = "my-release-name-granting-upgrade-rel3-to-rel4-with-mixed-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_TO_REL4_NON_SCALABLE_VDUS_CCVP =
            "my-release-name-granting-upgrade-rel3-to-rel4-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_CCVP = "my-release-name-granting-upgrade-rel3-success-1";
    private static final String RELEASE_NAME_SUCCESS_REL3_TO_REL4_CCVP = "my-release-name-granting-upgrade-rel3-to-rel4-success-1";
    private static final String RELEASE_NAME_SUCCESS_REL4_TO_REL3_CCVP = "my-release-name-granting-upgrade-rel4-to-rel3-success-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS = "my-release-name-granting-disabled-upgrade-success-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_MIXED_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-with-mixed-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_MIXED_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel3-with-mixed-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel3-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_MIXED_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-mixed-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel4-to-rel3-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_MIXED_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-mixed-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_NON_SCALABLE_VDUS_CCVP = "my-release-name-granting-disabled-upgrade-rel3-to-rel4-with-non-scalable-vdus-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_CCVP = "my-release-name-granting-disabled-upgrade-rel3-success-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_CCVP = "my-release-name-granting-disabled-upgrade-rel3-to-rel4-success-1";
    private static final String RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_CCVP = "my-release-name-granting-disabled-upgrade-rel4-to-rel3-success-1";
    private static final String RELEASE_NAME_DOWNGRADE_SUCCESS = "my-release-name-granting-downgrade-success-1";

    private static final String HELM_CHART_NAME = "spider-app-2.74.7";

    private static final String VDU_NAME = "eric-pm-bulk-reporter";

    private static final String ASPECT_1 = "Aspect1";

    private static final String REVISION_NUMBER = "2";

    private static final String TARGET_VNFD_ID_UPGRADE = "single-chart-c-rel4-545379754e30";
    private static final String TARGET_VNFD_ID_SCALE_UPGRADE = "single-chart-c-rel4-545379754e31-ccvp-scale";
    private static final String TARGET_VNFD_ID_MIXED_VDUS_UPGRADE = "single-chart-527c-arel4-5fcb086597zs-mixed-vdus";
    private static final String TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE = "single-chart-527c-arel4-5fcb086597zs-non-scalable-vdus";
    private static final String TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE = "single-chart-527c-rel3-5fcb086597zs-mixed-vdus";
    private static final String TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE = "single-chart-527c-rel3-5fcb086597zs-non-scalable-vdus";
    private static final String TARGET_VNFD_ID_REL3_UPGRADE = "single-chart-527c-rel3-5fcb086597zs-upgrade";
    private static final String SOURCE_VNFD_ID_UPGRADE = "single-chart-527c-arel4-5fcb086597zs";
    private static final String SOURCE_VNFD_ID_SCALE_UPGRADE = "single-chart-527c-arel4-5fcb086597zz-ccvp-scale";
    private static final String SOURCE_VNFD_ID_REL3_UPGRADE = "multi-chart-477c-aab3-2b04e6a363";

    private static final String TARGET_PACKAGE_ID_UPGRADE = "43bf1225-81e2-46b4-rel42-cadea4432939";
    private static final String TARGET_PACKAGE_ID_SCALE_UPGRADE = "43bf1225-81e1-46b4-rel42-cadea4432940";
    private static final String TARGET_PACKAGE_ID_MIXED_VDUS_UPGRADE = "43bf1225-81e2-46b4-rel42-cad0fu832939";
    private static final String TARGET_PACKAGE_ID_NON_SCALABLE_VDUS_UPGRADE = "43bf1225-81e2-46b4-rel42-cad0fu832940";
    private static final String TARGET_PACKAGE_ID_REL3_MIXED_VDUS_UPGRADE = "43bf1225-81e2-46b4-rel3-lforrjnf";
    private static final String TARGET_PACKAGE_ID_REL3_NON_SCALABLE_VDUS_UPGRADE = "43bf1225-81e2-46b4-rel3-8kj474ijd";
    private static final String TARGET_PACKAGE_ID_REL3_UPGRADE = "8uishkjshd-81e2-46b4-rel3-986ykwer";
    private static final String SOURCE_PACKAGE_ID_UPGRADE = "43bf1225-81e1-46b4-rel41-cadea4432939";
    private static final String SOURCE_PACKAGE_ID_SCALE_UPGRADE = "43bf1225-81e1-46b4-rel41-cadea4432940";
    private static final String SOURCE_PACKAGE_ID_REL3_UPGRADE = "d3def1ce-4cf4-477c-aab3-pkgId4e6a400";

    private static final String SCENARIO_NAME = "CCVP granting";
    private static final String SCENARIO_STATE_FIRST_CALL_TO_NFVO = "First granting request has been sent to NFVO";

    @Autowired
    @Qualifier("nfvoMockServer")
    private WireMockServer wireMockServer;

    @Autowired
    private OnboardingConfig onboardingConfig;

    @Autowired
    private GrantingNotificationsConfig grantingNotificationsConfig;

    @Autowired
    private OperationsInProgressRepository operationsInProgressRepository;

    @Autowired
    @Qualifier("nfvoOnboardingRoutingClient")
    private OnboardingRoutingClient nfvoOnboardingRoutingClient;

    @BeforeEach
    public void prep() throws Exception {
        ReflectionTestUtils.setField(onboardingConfig, "host", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(nfvoOnboardingRoutingClient, NFVO_TOKEN_PARAM, NFVO_TOKEN);
        GrantingTestUtils.stubHealthCheck(wireMockServer);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, SOURCE_PACKAGE_ID_UPGRADE);
        GrantingTestUtils.stubGettingNewVnfd(wireMockServer, TARGET_PACKAGE_ID_UPGRADE);
        GrantingTestUtils.stubGettingCurrentVnfd(wireMockServer, SOURCE_PACKAGE_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingNewVnfd(wireMockServer, TARGET_PACKAGE_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingRel4VnfdWithMixedScalableVdus(wireMockServer, TARGET_PACKAGE_ID_MIXED_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingRel4VnfdWithNonScalableVdus(wireMockServer, TARGET_PACKAGE_ID_NON_SCALABLE_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingRel3Vnfd(wireMockServer, SOURCE_PACKAGE_ID_REL3_UPGRADE);
        GrantingTestUtils.stubGettingRel3VnfdWithMixedScalableVdus(wireMockServer, TARGET_PACKAGE_ID_REL3_MIXED_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingRel3VnfdWithNonScalableVdus(wireMockServer, TARGET_PACKAGE_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingRel3UpgradeVnfd(wireMockServer, TARGET_PACKAGE_ID_REL3_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, SOURCE_VNFD_ID_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, SOURCE_VNFD_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_MIXED_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, SOURCE_VNFD_ID_REL3_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingPackageResponseByVnfd(wireMockServer, TARGET_VNFD_ID_REL3_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, SOURCE_PACKAGE_ID_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, TARGET_PACKAGE_ID_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, SOURCE_PACKAGE_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, TARGET_PACKAGE_ID_SCALE_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, TARGET_PACKAGE_ID_MIXED_VDUS_UPGRADE);
        GrantingTestUtils.stubGettingScalingMappingFile(wireMockServer, TARGET_PACKAGE_ID_NON_SCALABLE_VDUS_UPGRADE);
    }

    @AfterEach
    public void tearDown() {
        // restore granting as some tests switch it on and off
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    /*
     * Tests for combinations of:
     *
     * 1. Packages with:
     *   1.1 All scalable VDUs
     *   1.2 Mixed VDUs
     *   1.3 Non-scalable VDUs
     * 2. Upgrade path:
     *   2.1 Rel4 package -> Rel4 package
     *   2.2 Rel3 package -> Rel3 package
     *   2.3 Rel3 package -> Rel4 package
     *   2.4 Rel4 package -> Rel3 package
     * 3. Granting:
     *   3.1 Enabled
     *   3.2 Disabled
     */

    /* Tests with granting enabled */

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4PackagesWithAllScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_1, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_1, TARGET_VNFD_ID_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_1, RELEASE_NAME_SUCCESS, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4PackagesWithMixedScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_13, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_13, TARGET_VNFD_ID_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_13, RELEASE_NAME_SUCCESS_MIXED_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4PackagesWithNonScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_14, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_14,
                                                                                                   TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_14, RELEASE_NAME_SUCCESS_NON_SCALABLE_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3PackagesWithAllScalableVdus() throws Exception {
        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_21, TARGET_VNFD_ID_REL3_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_21, RELEASE_NAME_SUCCESS_REL3_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3PackagesWithMixedScalableVdus() throws Exception {
        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_15,
                                                                                                   TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_15, RELEASE_NAME_SUCCESS_REL3_MIXED_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3PackagesWithNonScalableVdus() throws Exception {
        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_16,
                                                                                                   TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_16, RELEASE_NAME_SUCCESS_REL3_NON_SCALABLE_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4ToRel3PackageWithAllScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel4ToRel3(DB_VNF_ID_22, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_22, TARGET_VNFD_ID_REL3_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_22, RELEASE_NAME_SUCCESS_REL4_TO_REL3_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4ToRel3PackageWithMixedScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel4ToRel3(DB_VNF_ID_17, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_17,
                                                                                                   TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_17, RELEASE_NAME_SUCCESS_REL4_TO_REL3_MIXED_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel4ToRel3PackageWithNonScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel4ToRel3(DB_VNF_ID_18, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_18,
                                                                                                   TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_18, RELEASE_NAME_SUCCESS_REL4_TO_REL3_NON_SCALABLE_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3ToRel4PackageWithAllScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel3ToRel4(DB_VNF_ID_23, SOURCE_VNFD_ID_REL3_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_23, TARGET_VNFD_ID_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_23, RELEASE_NAME_SUCCESS_REL3_TO_REL4_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3ToRel4PackageWithMixedScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel3ToRel4(DB_VNF_ID_19, SOURCE_VNFD_ID_REL3_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_19,
                                                                                                   TARGET_VNFD_ID_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_19, RELEASE_NAME_SUCCESS_REL3_TO_REL4_MIXED_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingRel3ToRel4PackageWithNonScalableVdus() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageRel3ToRel4(DB_VNF_ID_20, SOURCE_VNFD_ID_REL3_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_20,
                                                                                                   TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_20, RELEASE_NAME_SUCCESS_REL3_TO_REL4_NON_SCALABLE_VDUS_CCVP, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    /* Tests with granting disabled */

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4PackagesWithAllScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_35, TARGET_VNFD_ID_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_35, RELEASE_NAME_GRANTING_DISABLED_SUCCESS, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4PackagesWithMixedScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_24, TARGET_VNFD_ID_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_24, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_MIXED_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4PackagesWithNonScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_25,
                                                                                                   TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_25, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_NON_SCALABLE_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3PackagesWithAllScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_32, TARGET_VNFD_ID_REL3_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_32, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3PackagesWithMixedScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_26,
                                                                                                   TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_26, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_MIXED_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3PackagesWithNonScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_27,
                                                                                                   TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_27, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_NON_SCALABLE_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4ToRel3PackageWithAllScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_33, TARGET_VNFD_ID_REL3_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_33, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4ToRel3PackageWithMixedScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_28,
                                                                                                   TARGET_VNFD_ID_REL3_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_28, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_MIXED_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel4ToRel3PackageWithNonScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_29,
                                                                                                   TARGET_VNFD_ID_REL3_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_29, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL4_TO_REL3_NON_SCALABLE_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3ToRel4PackageWithAllScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_34, TARGET_VNFD_ID_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_34, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3ToRel4PackageWithMixedScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_30,
                                                                                                   TARGET_VNFD_ID_MIXED_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_30, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_MIXED_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithoutGrantingRel3ToRel4PackageWithNonScalableVdus() throws Exception {
        // disable granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_31,
                                                                                                   TARGET_VNFD_ID_NON_SCALABLE_VDUS_UPGRADE);

        runGenericFlowForUpgradeWithoutGrantingRequest(DB_VNF_ID_31, RELEASE_NAME_GRANTING_DISABLED_SUCCESS_REL3_TO_REL4_NON_SCALABLE_VDUS_CCVP, operationId);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);

        // restore granting
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);
    }

    /* End of tests with different combinations of scalable VDUs, package version and granting mode */

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingWithRetryAndDelayFromNfvo() throws Exception {
        String expectedGrantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_7, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(expectedGrantRequestBody)
                                       .inScenario(SCENARIO_NAME)
                                       .whenScenarioStateIs(Scenario.STARTED)
                                       .willReturn(aResponse().withFixedDelay(15000))
                                       .willSetStateTo(SCENARIO_STATE_FIRST_CALL_TO_NFVO));
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(expectedGrantRequestBody)
                                       .inScenario(SCENARIO_NAME)
                                       .whenScenarioStateIs(SCENARIO_STATE_FIRST_CALL_TO_NFVO)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value()))
                                       .willSetStateTo(Scenario.STARTED));
        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_7, TARGET_VNFD_ID_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_7, RELEASE_NAME_SUCCESS, operationId, 2);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingAfterScalingWithPersistScaleInfoTrue() throws Exception {
        //Disable granting for Scale
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //Scale Out
        String scaleLifeCycleOperationId = runScaleOutOperation(DB_VNF_ID_8);

        //Fake completion message of Scale Out
        runGenericFlowForScale(DB_VNF_ID_8, scaleLifeCycleOperationId);

        //Assert Scale worked out correctly
        assertScaleOutIsCompleted(DB_VNF_ID_8);

        //Enable granting for CCVP
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);

        // Prepare GrantRequest
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageAfterScale(DB_VNF_ID_8, SOURCE_VNFD_ID_SCALE_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //CCVP
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_8, TARGET_VNFD_ID_SCALE_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_8, RELEASE_NAME_SUCCESS_SCALE_CCVP, operationId, 1);

        //Assert CCVP completed
        assertUpgradeIsCompleted(operationId);

        //Assert upgraded instance persist scale info from source instance
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_8);
        int upgradedVnfInstanceScaleLevel = getScaleLevelForAspectIdForAspectOne(upgradedVnfInstance);
        assertThat(upgradedVnfInstanceScaleLevel).isOne();
    }

    @Test
    public void successfulChangeVnfPkgRequestWithGrantingAfterScalingWithPersistScaleInfoFalse() throws Exception {
        //Disable granting for Scale
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //Scale Out
        String scaleLifeCycleOperationId = runScaleOutOperation(DB_VNF_ID_9);

        //Fake completion message of Scale Out
        runGenericFlowForScale(DB_VNF_ID_9, scaleLifeCycleOperationId);

        //Assert Scale worked out correctly
        assertScaleOutIsCompleted(DB_VNF_ID_9);

        //Enable granting for CCVP
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);

        // Prepare GrantRequest
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageAfterScale(DB_VNF_ID_9, SOURCE_VNFD_ID_SCALE_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //CCVP
        String jsonString = createChangeVnfPkgVnfRequestBody(TARGET_VNFD_ID_SCALE_UPGRADE, true, false);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_9, CHANGE_VNFPKG);

        //Assert CCVP worked out correctly
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        String operationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(operationId, LifecycleOperationState.PROCESSING));

        runGenericFlowForUpgrade(DB_VNF_ID_9, RELEASE_NAME_SUCCESS_SCALE_CCVP, operationId, 1);

        //Assert CCVP completed
        assertUpgradeIsCompleted(operationId);

        //Assert upgraded instance persist scale info from source instance
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_9);
        int upgradedVnfInstanceScaleLevel = getScaleLevelForAspectIdForAspectOne(upgradedVnfInstance);
        assertThat(upgradedVnfInstanceScaleLevel).isZero();
    }

    @Test
    public void successfulDowngradeRequestWithGranting() throws Exception {
        //prepare
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForDowngrade(DB_VNF_ID_3, TARGET_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //send change package request
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_3, SOURCE_VNFD_ID_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_3, RELEASE_NAME_DOWNGRADE_SUCCESS, operationId, 1);

        //assert change_vnfpkg completed
        assertUpgradeIsCompleted(operationId);
    }

    @Test
    public void successfulDowngradeRequestWithGrantingAfterScalingWithPersistScaleInfoTrue() throws Exception {
        //Disable granting for Scale
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //Scale Out
        String scaleLifeCycleOperationId = runScaleOutOperation(DB_VNF_ID_10);

        //Fake completion message of Scale Out
        runGenericFlowForScale(DB_VNF_ID_10, scaleLifeCycleOperationId);

        //Assert Scale worked out correctly
        assertScaleOutIsCompleted(DB_VNF_ID_10);

        //Enable granting for CCVP
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);

        // Prepare GrantRequest
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageAfterScale(DB_VNF_ID_10, SOURCE_VNFD_ID_SCALE_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //CCVP
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_10, TARGET_VNFD_ID_SCALE_UPGRADE);

        runGenericFlowForUpgrade(DB_VNF_ID_10, RELEASE_NAME_SUCCESS_SCALE_CCVP, operationId, 1);

        //Assert CCVP completed
        assertUpgradeIsCompleted(operationId);

        //Assert upgraded instance persist scale info from source instance
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_10);
        int upgradedVnfInstanceScaleLevel = getScaleLevelForAspectIdForAspectOne(upgradedVnfInstance);
        assertThat(upgradedVnfInstanceScaleLevel).isZero();
    }

    @Test
    public void failedDowngradeRequestWithGrantingAfterScalingWithPersistScaleInfoAndAutoRollbackTrue() throws Exception {
        //Disable granting for Scale
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //Scale Out
        String scaleLifeCycleOperationId = runScaleOutOperation(DB_VNF_ID_11);

        //Fake completion message of Scale Out
        runGenericFlowForScale(DB_VNF_ID_11, scaleLifeCycleOperationId);

        //Assert Scale worked out correctly
        assertScaleOutIsCompleted(DB_VNF_ID_11);

        //Enable granting for CCVP
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);

        // Prepare GrantRequest
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageAfterScale(DB_VNF_ID_11, SOURCE_VNFD_ID_SCALE_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //CCVP
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_11, TARGET_VNFD_ID_SCALE_UPGRADE);

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failedLifecycleMessage = getHelmReleaseLifecycleMessage(
                RELEASE_NAME_SUCCESS_SCALE_CCVP, HelmReleaseState.FAILED, operationId, HelmReleaseOperationType.CHANGE_VNFPKG, REVISION_NUMBER);
        messageHelper.sendCompleteMessageForAllCnfCharts(failedLifecycleMessage, DB_VNF_ID_11, true, FAILED);

        LifecycleOperation rolledBackLifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(rolledBackLifecycleOperation.getOperationState()).isEqualTo(FAILED);

        //Assert scale info of instance is the same as before CCVP
        VnfInstance vnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_11);
        int vnfInstanceScaleLevel = getScaleLevelForAspectIdForAspectOne(vnfInstance);
        assertThat(vnfInstanceScaleLevel).isOne();
    }

    @Test
    public void failedChangeVnfPkgRequestWithGrantingAfterScalingWithPersistScaleInfoAndAutoRollbackIsOn() throws Exception {
        //Disable granting for Scale
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", false);

        //Scale Out
        String scaleLifeCycleOperationId = runScaleOutOperation(DB_VNF_ID_12);

        //Fake completion message of Scale Out
        runGenericFlowForScale(DB_VNF_ID_12, scaleLifeCycleOperationId);

        //Assert Scale worked out correctly
        assertScaleOutIsCompleted(DB_VNF_ID_12);

        //Enable granting for CCVP
        ReflectionTestUtils.setField(grantingNotificationsConfig, "isGrantSupported", true);

        // Prepare GrantRequest
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackageAfterScale(DB_VNF_ID_12, SOURCE_VNFD_ID_SCALE_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));

        //CCVP
        String operationId = sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(DB_VNF_ID_12, TARGET_VNFD_ID_SCALE_UPGRADE);
        //Assert CCVP worked out correctly

        //Assert granting service called
        wireMockServer.verify(exactly(1),
                              postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(DB_VNF_ID_12)));

        //Fake upgrade failed messages
        HelmReleaseLifecycleMessage failedUpgradeLifecycleMessage = getHelmReleaseLifecycleMessage(
                RELEASE_NAME_SUCCESS_SCALE_CCVP, HelmReleaseState.FAILED, operationId, HelmReleaseOperationType.CHANGE_VNFPKG, REVISION_NUMBER);
        messageHelper.sendCompleteMessageForAllCnfCharts(failedUpgradeLifecycleMessage, DB_VNF_ID_12, true, ROLLING_BACK);

        //Fake autoRollback upgrade completion messages
        HelmReleaseLifecycleMessage completedAutoRollbackLifecycleMessage = getHelmReleaseLifecycleMessage(
                RELEASE_NAME_SUCCESS_SCALE_CCVP, HelmReleaseState.COMPLETED, operationId, HelmReleaseOperationType.CHANGE_VNFPKG, REVISION_NUMBER);
        messageHelper.sendCompleteMessageForAllCnfCharts(completedAutoRollbackLifecycleMessage, DB_VNF_ID_12, true, ROLLED_BACK);

        LifecycleOperation rolledBackLifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(rolledBackLifecycleOperation.getOperationState()).isEqualTo(ROLLED_BACK);

        //Assert upgraded instance persist scale info from source instance
        VnfInstance upgradedVnfInstance = vnfInstanceRepository.findByVnfInstanceId(DB_VNF_ID_12);
        int upgradedVnfInstanceScaleLevel = getScaleLevelForAspectIdForAspectOne(upgradedVnfInstance);
        assertThat(upgradedVnfInstanceScaleLevel).isOne();
    }

    @Test
    public void failedChangeVnfPkgRequestWithGrantingForbiddenAutoRollbackIsOn() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_2, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        //send change package request
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);

        String jsonString = createChangeVnfPkgVnfRequestBody(TARGET_VNFD_ID_UPGRADE, true, true);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_2, CHANGE_VNFPKG);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_2);

        //assert internal server error
        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_2, SOURCE_PACKAGE_ID_UPGRADE);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);

        //assert granting service called
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(DB_VNF_ID_2)));
    }

    @Test
    public void failedChangeVnfPkgRequestWithGrantingUnavailableAutoRollbackIsON() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_4, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())));

        //send change package request
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_4);
        String jsonString = createChangeVnfPkgVnfRequestBody(TARGET_VNFD_ID_UPGRADE, true, true);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_4, CHANGE_VNFPKG);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_4);

        //assert internal server error
        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForServiceUnavailableResponse(DB_VNF_ID_4);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);

        //assert granting service called
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(DB_VNF_ID_4)));
    }

    @Test
    public void failedDowngradeRequestWithGrantingForbiddenAutoRollbackIsOFF() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForDowngrade(DB_VNF_ID_5, TARGET_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        //send change package request
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_5);
        String jsonString = createChangeVnfPkgVnfRequestBody(SOURCE_VNFD_ID_UPGRADE, false, true);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_5, CHANGE_VNFPKG);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_5);

        //assert internal server error
        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_5, TARGET_PACKAGE_ID_UPGRADE);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);

        //assert granting service called
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(DB_VNF_ID_5)));
    }

    @Test
    public void failedChangeVnfPkgRequestWithGrantingForbiddenAutoRollbackIsOFF() throws Exception {
        String grantRequestBody = GrantingTestUtils.getGrantRequestBodyForChangePackage(DB_VNF_ID_6, SOURCE_VNFD_ID_UPGRADE);
        wireMockServer.stubFor(GrantingTestUtils.prepareGrantingRequest(grantRequestBody)
                                       .willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

        //send change package request
        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_6);
        String jsonString = createChangeVnfPkgVnfRequestBody(TARGET_VNFD_ID_UPGRADE, false, true);
        MvcResult result = requestHelper.makePostRequest(jsonString, DB_VNF_ID_6, CHANGE_VNFPKG);
        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().until(awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.ROLLED_BACK));

        GrantingTestUtils.verifyNotFoundInOperationsInProgressTable(operationsInProgressRepository, DB_VNF_ID_6);

        //assert internal server error
        ProblemDetails expectedProblemDetails = GrantingTestUtils.getProblemDetailsForForbiddenResponse(DB_VNF_ID_6, SOURCE_PACKAGE_ID_UPGRADE);
        final LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(lifeCycleOperationId);
        assertThat(operation.getOperationState()).isEqualTo(LifecycleOperationState.ROLLED_BACK);
        assertThat(mapper.readValue(operation.getError(), ProblemDetails.class)).isEqualTo(expectedProblemDetails);

        //assert granting service called
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(DB_VNF_ID_6)));
    }

    private String createChangeVnfPkgVnfRequestBody(final String vnfdId, boolean autoRollbackAllowed, boolean persistScaleInfo)
    throws JsonProcessingException {
        ChangeCurrentVnfPkgRequest request = new ChangeCurrentVnfPkgRequest();
        final Map<Object, Object> additionalParams = new HashMap<>();
        additionalParams.put(IS_AUTO_ROLLBACK_ALLOWED_VNFD_KEY, autoRollbackAllowed);
        additionalParams.put(PERSIST_SCALE_INFO, persistScaleInfo);
        request.vnfdId(vnfdId).additionalParams(additionalParams);
        return mapper.writeValueAsString(request);
    }

    private String sendUpgradeRequestVerifyAcceptedAndAwaitForOperationInProcessingState(String vnfId, String vnfdId) throws Exception {
        String jsonString = createChangeVnfPkgVnfRequestBody(vnfdId, true, true);
        final MvcResult result = requestHelper.makePostRequest(jsonString, vnfId, CHANGE_VNFPKG);

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).startsWith("http://localhost/vnflcm/v1/vnf_lcm_op_occs/");

        final String lifeCycleOperationId = getLifeCycleOperationId(result);
        await().timeout(20, TimeUnit.SECONDS).until(
                awaitHelper.operationReachesState(lifeCycleOperationId, LifecycleOperationState.PROCESSING)
        );
        return lifeCycleOperationId;
    }

    private void runGenericFlowForUpgrade(String vnfId, String releaseName, String operationId, int grantingRequestsNumber) throws Exception {
        //assert granting service called
        wireMockServer.verify(exactly(grantingRequestsNumber),
                              postRequestedFor(urlPathEqualTo(GrantingTestUtils.GRANTING_URL)).withRequestBody(containing(vnfId)));

        //Fake CRD upgrade completion message
        WorkflowServiceEventMessage completedUpgradeWfsMessage =
                getWfsEventMessage(releaseName, WorkflowServiceEventStatus.COMPLETED, operationId, WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedUpgradeWfsMessage, vnfId, HelmReleaseOperationType.INSTANTIATE,
                                                            COMPLETED, true, true);

        //Fake CNF completed message from WFS
        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage(
                releaseName, HelmReleaseState.COMPLETED, operationId, HelmReleaseOperationType.CHANGE_VNFPKG, REVISION_NUMBER);
        messageHelper.sendCompleteMessageForAllCnfCharts(completedLifecycleMessage, vnfId, false, COMPLETED);
    }

    private void runGenericFlowForUpgradeWithoutGrantingRequest(String vnfId, String releaseName, String operationId) throws Exception {
        //Fake CRD upgrade completion message
        WorkflowServiceEventMessage completedUpgradeWfsMessage =
                getWfsEventMessage(releaseName, WorkflowServiceEventStatus.COMPLETED, operationId, WorkflowServiceEventType.CRD);
        messageHelper.sendInternalApiMessageForAllCrdCharts(completedUpgradeWfsMessage, vnfId, HelmReleaseOperationType.INSTANTIATE,
                                                            COMPLETED, true, true);

        //Fake CNF completed message from WFS
        HelmReleaseLifecycleMessage completedLifecycleMessage = getHelmReleaseLifecycleMessage(
                releaseName, HelmReleaseState.COMPLETED, operationId, HelmReleaseOperationType.CHANGE_VNFPKG, REVISION_NUMBER);
        messageHelper.sendCompleteMessageForAllCnfCharts(completedLifecycleMessage, vnfId, false, COMPLETED);
    }

    private void assertUpgradeIsCompleted(String operationId) {
        LifecycleOperation completedChangeVnfPkgOperation =
                lifecycleOperationRepository.findByOperationOccurrenceId(operationId);
        assertThat(completedChangeVnfPkgOperation.getOperationState()).isEqualTo(COMPLETED);
        assertThat(completedChangeVnfPkgOperation.getLifecycleOperationType()).isEqualTo(LifecycleOperationType.CHANGE_VNFPKG);
    }

    private int getScaleLevelForAspectIdForAspectOne(VnfInstance vnfInstance) {
        return vnfInstance.getScaleInfoEntity().stream()
                .filter(scaleInfoEntity -> scaleInfoEntity.getAspectId().equals(ASPECT_1))
                .map(ScaleInfoEntity::getScaleLevel)
                .findFirst()
                .orElse(-1);
    }

    private String runScaleOutOperation(String vnfId) throws Exception {
        VnfInstanceResponse vnfInstanceResponse = new VnfInstanceResponse().id(vnfId);
        MvcResult scaleMvcResult = requestHelper.getMvcResultScaleVnfRequest(vnfInstanceResponse, ScaleVnfRequest.TypeEnum.OUT, ASPECT_1);
        return getLifeCycleOperationId(scaleMvcResult);
    }

    private void runGenericFlowForScale(String vnfId, String scaleLifeCycleOperationId) {
        HelmReleaseLifecycleMessage completedScaleHelmMessage = getHelmReleaseLifecycleMessage(RELEASE_NAME_SUCCESS_SCALE_CCVP,
                                                                                               HelmReleaseState.COMPLETED,
                                                                                               scaleLifeCycleOperationId,
                                                                                               HelmReleaseOperationType.SCALE,
                                                                                               REVISION_NUMBER);
        messageHelper.sendMessageForChart(completedScaleHelmMessage, RELEASE_NAME_SUCCESS_SCALE_CCVP, vnfId, false, HelmReleaseState.COMPLETED);
    }

    private void assertScaleOutIsCompleted(String vnfId) {
        VnfInstance scaledVnfInstance = vnfInstanceRepository.findByVnfInstanceId(vnfId);
        int scaleLevel = getScaleLevelForAspectIdForAspectOne(scaledVnfInstance);
        assertThat(scaleLevel).isOne();
        List<HelmChart> scaledHelmCharts = scaledVnfInstance.getHelmCharts();
        HelmChart scaledHelmChart = getHelmChartByName(scaledHelmCharts, HELM_CHART_NAME);
        Map<String, ReplicaDetails> replicaDetails = replicaDetailsMapper.getReplicaDetailsFromHelmChart(scaledHelmChart);
        ReplicaDetails ericPmBulkReportedVmAfterScaleOut = replicaDetails.get(VDU_NAME);
        assertThat(ericPmBulkReportedVmAfterScaleOut.getCurrentReplicaCount()).isEqualTo(2);
    }
}
