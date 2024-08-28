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
package com.ericsson.vnfm.orchestrator.infrastructure.configurations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This configuration prevents Spring from splitting one parameter
 * with comma into several list items.
 *
 * A 'sort' parameter used in pagination is an example: a single statement may look like
 * 'sort=fieldName,asc'. Without this configuration Spring will convert it to ['fieldName', 'asc']
 * which is incorrect.
 */
@Component
public class FormattersMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.removeConvertible(String.class, Collection.class);
        registry.addConverter(String.class, Collection.class, s -> {
            List<String> list = new ArrayList<>();
            list.add(s);
            return list;
        });
    }
}
