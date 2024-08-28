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
package com.ericsson.vnfm.orchestrator.presentation.services.ssh;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.infrastructure.configurations.EnmMetricsExposers;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.AuthenticationException;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.ConnectionFailureException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SSHSessionLifecycleService {

    private static final String USER_KNOWN_HOST_FILE = "UserKnownHostsFile";

    private static final String USER_KNOWN_HOST_FILE_VALUE = "/dev/null";

    private static final String STRING_HOST_KEY_CHECKING = "StrictHostKeyChecking";

    private static final String STRING_HOST_KEY_CHECKING_VALUE = "no";

    private static final String PREFERRED_AUTHENTICATIONS = "PreferredAuthentications";

    private static final String PREFERRED_AUTHENTICATIONS_VALUE = "publickey,keyboard-interactive,password";

    @Value("${enm.scripting.cluster.host}")
    private String scriptingClusterHost;

    @Value("${enm.scripting.cluster.username}")
    private String scriptingClusterUsername;

    @Value("${enm.scripting.cluster.password}")
    private String scriptingClusterPassword;

    @Value("${enm.scripting.cluster.ssh.key}")
    private String scriptingClusterSshKey;

    @Value("${enm.scripting.cluster.port}")
    private int scriptingClusterPort;

    @Value("${enm.scripting.cluster.ssh.connection.timeout}")
    private int scriptingClusterConnectionTimeout;

    @Value("${enm.scripting.cluster.ssh.connection.delay}")
    private int connectionRetryTime;

    private final Properties sshConnectionConfig;

    private final EnmMetricsExposers enmMetricsExposers;

    public SSHSessionLifecycleService(EnmMetricsExposers enmMetricsExposers) {
        sshConnectionConfig = new Properties();
        this.enmMetricsExposers = enmMetricsExposers;
        sshConnectionConfig.put(USER_KNOWN_HOST_FILE, USER_KNOWN_HOST_FILE_VALUE);
        sshConnectionConfig.put(STRING_HOST_KEY_CHECKING, STRING_HOST_KEY_CHECKING_VALUE);
        sshConnectionConfig.put(PREFERRED_AUTHENTICATIONS, PREFERRED_AUTHENTICATIONS_VALUE);
    }

    public Session createSession(boolean retry, int retryCount) {
        try {
            boolean isSSHKeyFilePresent = StringUtils.isNotBlank(scriptingClusterSshKey);
            LOGGER.info("Starting ssh key generation isRetryAllowed={}, retryCount={}, isSSHKeyFilePresent={}",
                        retry, retryCount, isSSHKeyFilePresent);
            JSch jsch = new JSch();
            if (isSSHKeyFilePresent) {
                jsch.addIdentity(scriptingClusterSshKey);
            }

            Session session = jsch.getSession(scriptingClusterUsername, scriptingClusterHost, scriptingClusterPort);
            if (!isSSHKeyFilePresent) {
                session.setPassword(scriptingClusterPassword);
            }
            session.setConfig(sshConnectionConfig);
            session.connect(scriptingClusterConnectionTimeout);
            enmMetricsExposers.getConnToEnmMetric().increment();
            return session;
        } catch (final JSchException jSchException) {
            handleException(jSchException);
            return tryRetryConnection(retry, retryCount, jSchException);
        }
    }

    public void closeSession(Session session) {
        LOGGER.info("Cleaning ssh session");
        if (session != null && session.isConnected()) {
            session.disconnect();
            enmMetricsExposers.getConnToEnmMetric().decrement();
        }
    }

    private void handleException(JSchException jSchException) {
        LOGGER.error("JSchException:: Exception occurred while connecting to the server. {} ", jSchException.getMessage());
        if (jSchException.getMessage().toLowerCase().contains("auth fail")
                || (jSchException.getMessage().contains("FileNotFoundException: " + scriptingClusterSshKey))) {
            throw new AuthenticationException(String.format("Auth failed. %s", jSchException.getMessage()), jSchException);
        }
    }

    private Session tryRetryConnection(boolean retry, int retryCount, JSchException jSchException) {
        if (retry && retryCount != 0) {
            delay(connectionRetryTime);
            LOGGER.info("Connection failure occurred {} retrying for {} time", jSchException.getMessage(),
                        retryCount);
            return createSession(true, retryCount - 1);
        } else {
            throw new ConnectionFailureException(String.format("Connection to ENM has failed. %s", jSchException.getMessage()), jSchException);
        }
    }

    private static void delay(long delay) {
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Exception occurred while executing a delay", ie);
        }
    }
}
