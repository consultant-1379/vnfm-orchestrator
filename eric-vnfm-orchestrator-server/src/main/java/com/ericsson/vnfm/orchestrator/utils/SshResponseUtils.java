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
package com.ericsson.vnfm.orchestrator.utils;

import static com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService.COMMAND_OUTPUT;
import static com.ericsson.vnfm.orchestrator.presentation.services.OssNodeService.EXIT_STATUS;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.ericsson.vnfm.orchestrator.presentation.services.ssh.SshResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SshResponseUtils {

    private static final Pattern STATUS_REGEX = Pattern.compile("Status");

    private SshResponseUtils() {
    }

    public static String extractSpecificFailure(final SshResponse sshResponse, final EnmOperationEnum operation) {
        String output = sshResponse.getOutput();
        if (StringUtils.isNotBlank(output)) {
            return extractSpecificFailure(operation, output);
        } else {
            return sshResponse.getErrorResponse();
        }
    }

    private static String extractSpecificFailure(EnmOperationEnum operation, String output) {
        String specificErrorMessage = "";
        String commandWhichFailed = "";
        try {
            JSONObject jsonOutput = new JSONObject(output);
            Map<String, Object> commandResponses = jsonOutput.getJSONObject(operation.getOperationResponse()).toMap();
            for (Map.Entry<String, Object> entry : commandResponses.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) entry.getValue();
                Object exitStatus = response.get(EXIT_STATUS);
                if (exitStatus != null && (Integer) response.get(EXIT_STATUS) != 0) {
                    specificErrorMessage = (String) response.get(COMMAND_OUTPUT);
                    commandWhichFailed = STATUS_REGEX.split(entry.getKey())[0];
                }
            }
        } catch (Exception e) {
            LOGGER.warn("An error occurred during command output", e);
            return output;
        }
        return String.format("The command [%s] failed with the following output: %s", commandWhichFailed, specificErrorMessage);
    }
}
