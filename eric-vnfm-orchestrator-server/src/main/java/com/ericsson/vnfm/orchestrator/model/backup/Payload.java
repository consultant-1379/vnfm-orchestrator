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
package com.ericsson.vnfm.orchestrator.model.backup;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import utils.logging.ExcludeFieldsFromToString;
import utils.logging.ExcludeFieldsFromToStringGenerator;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payload {
    private String backupName;
    private String uri;

    @ExcludeFieldsFromToString
    private String password;

    @Override
    public String toString() {
        ToStringStyle customStringStyle = ExcludeFieldsFromToStringGenerator.INSTANCE.getStyle(this.getClass());
        return ToStringBuilder.reflectionToString(this, customStringStyle);
    }
}
