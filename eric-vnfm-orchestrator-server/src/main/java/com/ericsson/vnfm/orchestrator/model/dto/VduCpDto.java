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

import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VduCpDto {

    private String format;
    private String path;
    private String param;
    private String multusInterface;
    private List<String> set;
    private List<VduCp> vduCps;

    @Data
    @NoArgsConstructor
    public static class VduCp {
        private String cpId;
        private Integer order;
        private List<Triple<String, String, String>> interfaceToNadParams;
    }
}
