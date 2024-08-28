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
package com.ericsson.vnfm.orchestrator.scheduler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({ "prod" })
public class ConnectionPoolMonitoring {

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    private DataSource dataSource;

    private static final double CONNECTIONS_TRESHOLD = 0.85;

    private static final int POOL_SUSPEND_DELAY_TIMEOUT = 5;

    @Scheduled(fixedDelay = 1000)
    public void monitorConnectionPool() {
        int maxPoolSize = hikariDataSource.getMaximumPoolSize();
        int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();

        LOGGER.debug("Active DB connections count: {}", activeConnections);

        if ((double) activeConnections / maxPoolSize > CONNECTIONS_TRESHOLD) {
            int freeConnectionsCount = maxPoolSize - activeConnections;
            LOGGER.warn("There are only {} free DB connections", freeConnectionsCount);
        }
    }

    // TODO disabled due to implementation graceful shutdown of Database PG
    //  @Scheduled(fixedDelay = 5000L)
    public void dbAvailabilityCheck() throws InterruptedException {
        LOGGER.debug("DB Availability Check");
        HikariPoolMXBean hikariPoolMXBean = hikariDataSource.getHikariPoolMXBean();
        boolean serviceAvailable = checkServiceState();
        if (!serviceAvailable) {
            LOGGER.debug("Suspend hikari pool");
            hikariPoolMXBean.suspendPool();
            while (!serviceAvailable) {
                LOGGER.debug("Sleep for %d seconds".formatted(POOL_SUSPEND_DELAY_TIMEOUT));
                TimeUnit.SECONDS.sleep(POOL_SUSPEND_DELAY_TIMEOUT);
                serviceAvailable = checkServiceState();
            }
            LOGGER.debug("Resume hikari pool");
            hikariPoolMXBean.resumePool();
        }
    }

    private boolean checkServiceState() {
        LOGGER.debug("Check Service State");
        try (Connection conn = dataSource.getConnection()) { // TODO should be change to provided method by ADP team
            LOGGER.debug("Connection available");
            return true;
        } catch (SQLException e) {
            LOGGER.error("Connection exception", e);
        }
        return false;
    }

    @PostConstruct
    private void init() {
        dataSource = new SimpleDriverDataSource(
                new Driver(),
                dataSourceProperties.getUrl(),
                dataSourceProperties.getUsername(),
                dataSourceProperties.getPassword());
    }
}
