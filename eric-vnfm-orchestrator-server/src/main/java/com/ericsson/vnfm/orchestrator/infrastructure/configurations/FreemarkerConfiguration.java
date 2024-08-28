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

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Slf4j
@org.springframework.context.annotation.Configuration
public class FreemarkerConfiguration {

    @Bean
    @Primary
    public Configuration getFreemarkerConfiguration() {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        try {
            configuration.setSetting("log_template_exceptions", "false");
        } catch (TemplateException e) {
            LOGGER.warn("An error occurred during getting freemarker configuration", e);
            //suppressing freemarker stack trace
        }
        configuration.setTemplateLoader(new ClassTemplateLoader(this.getClass().getClassLoader(), "templates"));
        return configuration;
    }
}
