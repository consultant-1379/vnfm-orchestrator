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

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FtpResponse {

    private Path filePath;
    private int exitStatus;
    private String errorResponse;
}
