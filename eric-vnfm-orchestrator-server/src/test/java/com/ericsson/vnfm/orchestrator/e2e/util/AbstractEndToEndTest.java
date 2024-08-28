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
package com.ericsson.vnfm.orchestrator.e2e.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.ericsson.vnfm.orchestrator.messaging.MessagingServiceImpl;
import com.ericsson.vnfm.orchestrator.presentation.services.mapper.ReplicaDetailsMapper;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.OnboardingUriProvider;
import com.ericsson.vnfm.orchestrator.presentation.services.workflow.routing.WorkflowRoutingService;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.routing.onboarding.OnboardingClient;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@AutoConfigureStubRunner(ids = {
        "com.ericsson.orchestration.mgmt:eric-am-common-wfs-server",
        "com.ericsson.orchestration.mgmt.packaging:eric-am-onboarding-server" })
@TestPropertySource(properties = {
        "workflow.host=localhost:${stubrunner.runningstubs.eric-am-common-wfs-server.port}",
        "onboarding.host=http://localhost:${stubrunner.runningstubs.eric-am-onboarding-server.port}",
        "redis.listener.enabled=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({
        TestingMessageSender.class,
        AwaitHelper.class,
        MessageHelper.class,
        RequestHelper.class,
        VerificationHelper.class,
        StepsHelper.class })
public abstract class AbstractEndToEndTest extends AbstractDbSetupTest {

    private final static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/redis:6.2.13-alpine")
                                           .asCompatibleSubstituteFor("redis"))
                    .withExposedPorts(6379);

    static {
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getContainerIpAddress());
        System.setProperty("spring.data.redis.port", redisContainer.getFirstMappedPort().toString());
    }

    @Autowired
    protected ObjectMapper mapper;

    @SpyBean
    protected LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    protected TestingMessageSender testingMessageSender;

    @Autowired
    protected VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    protected ReplicaDetailsMapper replicaDetailsMapper;

    @Autowired
    protected OnboardingUriProvider onboardingUriProvider;

    @SpyBean
    protected RestTemplate restTemplate;

    @SpyBean
    protected OnboardingClient onboardingClient;

    @SpyBean
    protected WorkflowRoutingService workflowRoutingService;

    @MockBean
    protected MessagingServiceImpl messagingService;

    @Autowired
    protected RequestHelper requestHelper;

    @Autowired
    protected AwaitHelper awaitHelper;

    @Autowired
    protected MessageHelper messageHelper;

    @Autowired
    protected VerificationHelper verificationHelper;

    @Autowired
    protected StepsHelper stepsHelper;
}
