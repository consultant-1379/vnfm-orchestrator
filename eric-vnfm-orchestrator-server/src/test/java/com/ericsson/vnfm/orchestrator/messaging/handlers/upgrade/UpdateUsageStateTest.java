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
package com.ericsson.vnfm.orchestrator.messaging.handlers.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

import com.ericsson.vnfm.orchestrator.logging.InMemoryAppender;
import com.ericsson.vnfm.orchestrator.messaging.MessageHandlingContext;
import com.ericsson.vnfm.orchestrator.messaging.handlers.UpdateUsageState;
import com.ericsson.vnfm.orchestrator.model.WorkflowRoutingResponse;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.onboarding.HelmChartType;
import com.ericsson.vnfm.orchestrator.presentation.services.InstanceService;
import com.ericsson.vnfm.orchestrator.utils.Utility;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseLifecycleMessage;
import com.ericsson.workflow.orchestration.mgmt.model.messaging.HelmReleaseState;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class UpdateUsageStateTest {

    @MockBean
    private InstanceService instanceService;

    static private InMemoryAppender inMemoryAppender;
    static private final Logger LOGGER = (Logger) LoggerFactory.getLogger("com.ericsson.vnfm.orchestrator");

    @BeforeAll
    static public void init() {
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        LOGGER.addAppender(inMemoryAppender);
        LOGGER.setLevel(Level.INFO);
        inMemoryAppender.start();
    }

    @AfterEach
    public void reset() {
        inMemoryAppender.reset();
    }

    @Test
    public void shouldUpdateChartForRollback() {
        UpdateUsageState updateUsageState = spy(new UpdateUsageState(instanceService, false));

        final var message = new HelmReleaseLifecycleMessage();
        final String releaseName = "releaseName";
        message.setReleaseName(releaseName);

        final var helmChart1 = new HelmChart();
        helmChart1.setState(HelmReleaseState.COMPLETED.toString());
        final var helmChart2 = new HelmChart();
        helmChart2.setReleaseName(releaseName);
        helmChart2.setHelmChartType(HelmChartType.CNF);

        final var instance = new VnfInstance();

        final var context = new MessageHandlingContext<>(message);
        context.setVnfInstance(instance);
        context.setOperation(new LifecycleOperation());

        updateUsageState.handle(context);

        assertThat(inMemoryAppender.contains("Update usage state api failed. Flow will continue to update operation state.", Level.WARN)).isTrue();
        verify(updateUsageState, times(1)).getSuccessor();
    }
}
