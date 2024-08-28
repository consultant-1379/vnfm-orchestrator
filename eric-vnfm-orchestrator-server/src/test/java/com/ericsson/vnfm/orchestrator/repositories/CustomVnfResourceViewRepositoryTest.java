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
package com.ericsson.vnfm.orchestrator.repositories;

import static java.lang.Boolean.TRUE;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfResourceViewTestData.buildVnfResourceViews;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@TestPropertySource(properties = {"spring.flyway.locations = classpath:db/migration", "spring.flyway.clean-disabled = false"})
@Sql(value = "insert_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomVnfResourceViewRepositoryTest extends AbstractDbSetupTest {

    @Autowired
    private VnfResourceViewRepository vnfResourceViewRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    public void setUp() {
        Session session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(TRUE);
        statistics.clear();
    }

    @Test
    public void findAll() {
        final List<VnfResourceView> expected = buildVnfResourceViews();

        final Sort sort = Sort.by("vnfInstanceName").descending();

        final List<VnfResourceView> actual = vnfResourceViewRepository.findAll(sort);

        assertThat(actual).hasSize(3).containsExactlyInAnyOrderElementsOf(expected);
        assertSqlQueriesCount(2);
    }

    private void assertSqlQueriesCount(int expectedCount) {
        long actualCount = statistics.getQueryExecutionCount();
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}
