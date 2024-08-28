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
package com.ericsson.vnfm.orchestrator.infrastructure;

import java.io.IOException;
import java.util.Properties;
import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import lombok.Getter;
import lombok.Setter;

@Profile("dev")
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseDevConfiguration {
    private static final String POSTGRES = "postgres";

    private int port;

    @Bean
    @Primary
    @DependsOn("embeddedPostgresDS")
    public DataSource embeddedPostgresConnection(EmbeddedPostgres postgresDatabase) {
        Properties props = new Properties();
        props.setProperty("stringtype", "unspecified");
        String jdbcUrl = postgresDatabase.getJdbcUrl(POSTGRES, POSTGRES);
        return new DriverManagerDataSource(jdbcUrl, props);
    }

    @Bean
    public EmbeddedPostgres embeddedPostgresDS() throws IOException {
        EmbeddedPostgres embeddedPostgresDS = EmbeddedPostgres.builder()
                .setPort(port)
                .start();
        String jdbcUrl = embeddedPostgresDS.getJdbcUrl(POSTGRES, POSTGRES);
        System.setProperty("DB_URL", jdbcUrl);
        System.setProperty("DB_USERNAME", POSTGRES);
        System.setProperty("DB_PASSWORD", POSTGRES);
        return embeddedPostgresDS;
    }
}

