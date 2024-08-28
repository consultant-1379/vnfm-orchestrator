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

import static com.ericsson.vnfm.orchestrator.utils.Utility.deleteFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.ericsson.vnfm.orchestrator.presentation.services.ftp.FtpHelper;
import com.ericsson.vnfm.orchestrator.presentation.services.ftp.FtpInfo;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SshHelper {
    private static final String READ_OUTPUT_ERROR = "Unable to read the output of the executed command";
    private static final String SSH_OPERATION_ERROR = "Unable to do ssh operation";

    @Value("${enm.scripting.cluster.ssh.connection.retry}")
    private int connectionToEnmRetry;

    private ChannelExec channel;

    @Autowired
    private SSHSessionLifecycleService sessionLifecycleService;

    @Autowired
    private FtpHelper ftpHelper;

    public SshResponse executeCommand(SshInfo connectionInfo) {
        SshResponse sshResponse = new SshResponse();
        try {
            channel = (ChannelExec) connectionInfo.getSession().openChannel("exec");
            channel.setCommand(connectionInfo.getCommand());
            channel.setAgentForwarding(true);
            InputStream in = channel.getInputStream();
            InputStream err = channel.getExtInputStream();
            if (!channel.isConnected()) {
                channel.connect();
            }
            sshResponse.setOutput(getCommandOutput(in));
            sshResponse.setErrorResponse(getCommandOutput(err));
            sshResponse.setExitStatus(channel.getExitStatus());
        } catch (JSchException | IOException ex) {
            throw new InternalRuntimeException(SSH_OPERATION_ERROR, ex);
        } finally {
            LOGGER.debug("Trying to close the SSH session");
            closeSession();
        }
        return sshResponse;
    }

    private void closeSession() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
    }

    private static String getCommandOutput(InputStream commandOutput) {
        try (BufferedReader buffReader = new BufferedReader(new InputStreamReader(commandOutput, StandardCharsets.UTF_8))) {
            return buffReader.lines().map(String::trim).collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException ioe) {
            throw new InternalRuntimeException(READ_OUTPUT_ERROR, ioe);
        }
    }

    public SshResponse executeScript(final Path script) {
        Session session = null;
        try {
            session = sessionLifecycleService.createSession(true, connectionToEnmRetry);
            FtpInfo info = executeFtpPut(script, session);
            SshInfo sshInfo = getSshInfo(session, info.getDestinationFileName());
            LOGGER.info("Executing [{}] on {}", sshInfo.getCommand(), sshInfo.getSession().getHost());
            SshResponse sshResponse = executeCommand(sshInfo);
            deleteFtpFile(info, sshInfo);
            return sshResponse;
        } finally {
            deleteFile(script);
            sessionLifecycleService.closeSession(session);
        }
    }

    public SshResponse executeScriptWithFileParam(final Path script, final Path fileToUpload) {
        Session session = null;
        try {
            session = sessionLifecycleService.createSession(true, 3);
            FtpInfo scriptFtpInfo = executeFtpPut(script, session);
            FtpInfo fileFtpInfo = executeFtpPut(fileToUpload, session);
            SshInfo sshInfo = getSshInfo(session, scriptFtpInfo.getDestinationFileName());
            LOGGER.info("Executing [{}] on {}", sshInfo.getCommand(), sshInfo.getSession().getHost());
            SshResponse sshResponse = executeCommand(sshInfo);
            deleteFtpFile(scriptFtpInfo, sshInfo);
            deleteFtpFile(fileFtpInfo, sshInfo);
            return sshResponse;
        } finally {
            deleteFile(script);
            sessionLifecycleService.closeSession(session);
        }
    }

    public FtpInfo executeFtpPut(final Path file, final Session session) {
        FtpInfo info = createFtpInfo(file, session, FtpInfo.FtpOpType.PUT);
        LOGGER.info("Copying {} to {}", file.getFileName(), session.getHost());
        ftpHelper.performFtpOperation(info);
        return info;
    }

    private static FtpInfo createFtpInfo(final Path file, final Session session, FtpInfo.FtpOpType ftpOpType) {
        FtpInfo info = new FtpInfo();
        info.setSession(session);
        info.setFtpOperationType(ftpOpType);
        info.setDestinationFileName(file.getFileName().toString());
        info.setSourceFileName(file.toString());
        return info;
    }

    public void deleteFtpFile(final FtpInfo info, final SshInfo sshInfo) {
        sshInfo.setCommand("rm -rf " + info.getDestinationFileName());
        executeCommand(sshInfo);
    }

    private static SshInfo getSshInfo(final Session session, final String file) {
        SshInfo sshInfo = new SshInfo();
        sshInfo.setSession(session);
        sshInfo.setCommand("chmod +x " + file + "; python " + file);
        return sshInfo;
    }

    private static SshInfo getSshInfo(final Session session) {
        SshInfo sshInfo = new SshInfo();
        sshInfo.setSession(session);
        return sshInfo;
    }

    public FtpInfo executeFtpGet(final Path file, final Session session) {
        FtpInfo info = createFtpInfo(file, session, FtpInfo.FtpOpType.GET);
        LOGGER.info("Downloading {} from {}", file.getFileName(), session.getHost());
        ftpHelper.performFtpOperation(info);
        return info;
    }

    public Path downloadFile(final Path path) {
        Session session = null;
        try {
            session = sessionLifecycleService.createSession(true, 3);
            FtpInfo fileFtpInfo = executeFtpGet(path, session);
            SshInfo sshInfo = getSshInfo(session);
            deleteFtpFile(fileFtpInfo, sshInfo);
            return path;
        } finally {
            sessionLifecycleService.closeSession(session);
        }
    }
}
