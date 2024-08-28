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
package com.ericsson.vnfm.orchestrator.presentation.services.mapper;

import java.time.LocalDateTime;
import java.util.Date;

import org.mapstruct.Mapper;

import com.ericsson.vnfm.orchestrator.utils.Utility;

@Mapper(componentModel = "spring")
public class LocalDateMapper {

    public Date toDate(LocalDateTime localDateTime) {
        return Utility.convertToDate(localDateTime);
    }

}
