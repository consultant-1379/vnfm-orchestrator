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
package com.ericsson.vnfm.orchestrator.presentation.services.ftp;

import java.io.File;
import java.nio.file.Paths;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InternalRuntimeException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class FtpHelper {

    private ChannelSftp channel;

    public FtpResponse performFtpOperation(FtpInfo connectionInfo) {
        FtpResponse ftpResponse = new FtpResponse();
        try {
            channel = (ChannelSftp) connectionInfo.getSession().openChannel("sftp");
            channel.connect();
            if (channel.isConnected()) {
                if (FtpInfo.FtpOpType.GET.equals(connectionInfo.getFtpOperationType())) {
                    checkRemoteFileExists(connectionInfo);
                    channel.get(connectionInfo.getDestinationFileName(), connectionInfo.getSourceFileName());
                } else {
                    checkLocalFileExists(connectionInfo);
                    channel.put(connectionInfo.getSourceFileName(), connectionInfo.getDestinationFileName());
                }
            }
            ftpResponse.setExitStatus(channel.getExitStatus());
            ftpResponse.setFilePath(Paths.get(connectionInfo.getDestinationFileName()));
        } catch (JSchException | SftpException ex) {
            throw new InternalRuntimeException(String.format("Unable to SFTP operation due to: %s", ex.getMessage()), ex);
        } finally {
            LOGGER.debug("Trying to close the SFTP session");
            closeSession();
        }
        return ftpResponse;
    }

    private void checkRemoteFileExists(FtpInfo connectionInfo) throws SftpException {
        channel.ls(connectionInfo.getDestinationFileName());
    }

    private static void checkLocalFileExists(FtpInfo connectionInfo) {
        final File file = new File(connectionInfo.getSourceFileName());
        if (!(file.exists() && !file.isDirectory())) {
            throw new IllegalArgumentException("File does not exists in local machine.");
        }
    }

    private void closeSession() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
    }
}
