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
package com.ericsson.vnfm.orchestrator.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.vnfm.orchestrator.model.BackupsResponseDto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BackupResponseDtoWrapper {
    private List<BackupsResponseDto> backups = new ArrayList<>();
}