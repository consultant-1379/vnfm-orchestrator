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

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperations.TERMINAL_OPERATION_STATES;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildExpectedLifecycleOperationByIdWithAllFields;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildLifecycleOperationsByIds;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildLifecycleOperationsForPagination;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildLifecycleOperationsForSpecificationAndPagination;
import static com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData.buildLifecycleOperationsForSpecificationAndSort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.am.shared.filter.model.SpecificationMultiValue;
import com.ericsson.am.shared.filter.model.SpecificationOneValue;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.testData.LifecycleOperationTestData;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

@SpringBootTest
@TestPropertySource(properties = {"spring.flyway.locations = classpath:db/migration", "spring.flyway.clean-disabled = false"})
@Sql(value = "insert_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CustomLifecycleOperationRepositoryTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

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
        final List<LifecycleOperation> expected = LifecycleOperationTestData.buildLifecycleOperations();

        final List<LifecycleOperation> actual = lifecycleOperationRepository.findAll();

        assertThat(actual).hasSize(6);
        assertThat(expected).containsExactlyInAnyOrderElementsOf(actual);
        assertSqlQueriesCount(1);
    }

    @Test
    public void findAllById() {
        List<String> lifecycleOperationIds = List.of("d3def1ce-4cf4-477c-aab3-21c454e6a352", "d3def1ce-4cf4-477c-aab3-21c454e6a353",
                                                     "d3def1ce-4cf4-477c-aab3-21c454e6a355");
        final List<LifecycleOperation> expected = buildLifecycleOperationsByIds();

        final List<LifecycleOperation> actual = lifecycleOperationRepository.findAllById(lifecycleOperationIds);

        assertThat(actual).hasSize(3);
        assertThat(expected).containsExactlyInAnyOrderElementsOf(actual);
        assertSqlQueriesCount(5);
    }

    @Test
    public void selectFields() {
        List<String> fields = List.of("operationParams");
        Map<String, String> expected = Map.of("d3def1ce-4cf4-477c-aab3-21c454e6a350", "{\"testOperationParams350\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a351", "{\"testOperationParams351\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a352", "{\"testOperationParams352\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a353", "{\"testOperationParams353\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a354", "{\"testOperationParams354\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a355", "{\"testOperationParams355\":\"test\"}");
        final List<LifecycleOperation> lifecycleOperations = LifecycleOperationTestData.buildLifecycleOperations();
        final Map<String, Tuple> actualFields = lifecycleOperationRepository.selectFields(lifecycleOperations, fields);
        final Map<String, String> actual = actualFields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue().get(1)));

        assertThat(expected).containsExactlyInAnyOrderEntriesOf(actual);
        assertSqlQueriesCount(1);
    }

    @Test
    public void findByOperationOccurrenceId() {
        String lifecycleOperationId = "d3def1ce-4cf4-477c-aab3-21c454e6a354";
        final LifecycleOperation expected = buildExpectedLifecycleOperationByIdWithAllFields();
        final LifecycleOperation actual = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void findById() {
        String lifecycleOperationId = "d3def1ce-4cf4-477c-aab3-21c454e6a354";
        final LifecycleOperation expected = buildExpectedLifecycleOperationByIdWithAllFields();

        final Optional<LifecycleOperation> actualOptional = lifecycleOperationRepository.findById(lifecycleOperationId);

        assertThat(actualOptional).isPresent();
        final LifecycleOperation actual = actualOptional.get();
        assertThat(expected).isEqualTo(actual);
        assertSqlQueriesCount(5);
    }

    @Test
    public void findAllWithPagination() {
        final List<LifecycleOperation> expected = buildLifecycleOperationsForPagination();
        final PageRequest pageable = PageRequest.of(1, 2);

        final Page<LifecycleOperation> actualPaged = lifecycleOperationRepository.findAll(pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(2);

        final List<LifecycleOperation> actual = actualPaged.getContent();
        assertThat(expected).containsExactlyInAnyOrderElementsOf(actual);
        assertSqlQueriesCount(3);
    }

    @Test
    public void findAllWithSpecificationAndPagination() {
        final List<LifecycleOperation> expected = buildLifecycleOperationsForSpecificationAndPagination();

        FilterExpressionOneValue<String> vnfProductNameFilter = new FilterExpressionOneValue<>();
        vnfProductNameFilter.setKey("vnfProductName");
        vnfProductNameFilter.setOperation(OperandOneValue.EQUAL);
        vnfProductNameFilter.setValue("testVnfProductName352");
        Specification<LifecycleOperation> specification = new SpecificationOneValue<>(vnfProductNameFilter);

        final PageRequest pageable = PageRequest.of(0, 3);

        final Page<LifecycleOperation> actualPaged = lifecycleOperationRepository.findAll(specification, pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(1);

        final List<LifecycleOperation> actual = actualPaged.getContent();
        assertThat(expected).containsExactlyInAnyOrderElementsOf(actual);
        assertSqlQueriesCount(2);
    }

    @Test
    public void findAllWithSpecification() {
        final List<LifecycleOperation> expected = LifecycleOperationTestData.buildLifecycleOperations();

        FilterExpressionMultiValue<String> vnfProductNameFilter = new FilterExpressionMultiValue<>();
        vnfProductNameFilter.setKey("vnfProductName");
        vnfProductNameFilter.setOperation(OperandMultiValue.CONTAINS);
        vnfProductNameFilter.setValues(List.of("testVnfProductName"));
        Specification<LifecycleOperation> specification = new SpecificationMultiValue<>(vnfProductNameFilter);


        final List<LifecycleOperation> actual = lifecycleOperationRepository.findAll(specification);

        assertThat(actual).hasSize(6);
        assertThat(expected).containsExactlyInAnyOrderElementsOf(actual);
        assertSqlQueriesCount(2);
    }

    @Test
    public void findAllWithSpecificationAndSort() {
        final List<LifecycleOperation> expected = buildLifecycleOperationsForSpecificationAndSort();

        FilterExpressionMultiValue<String> vnfProductNameFilter = new FilterExpressionMultiValue<>();
        vnfProductNameFilter.setKey("vnfProductName");
        vnfProductNameFilter.setOperation(OperandMultiValue.CONTAINS);
        vnfProductNameFilter.setValues(List.of("testVnfProductName"));
        Specification<LifecycleOperation> specification = new SpecificationMultiValue<>(vnfProductNameFilter);

        Sort sort = Sort.by("vnfProductName").descending();

        final List<LifecycleOperation> actual = lifecycleOperationRepository.findAll(specification, sort);

        assertThat(actual).hasSize(6);
        assertThat(expected).isEqualTo(actual);
        assertSqlQueriesCount(2);
    }

    @Test
    public void findAllWithSort() {
        final List<LifecycleOperation> expected = LifecycleOperationTestData.buildLifecycleOperations();
        Sort sort = Sort.by("vnfProductName").ascending();

        final List<LifecycleOperation> actual = lifecycleOperationRepository.findAll(sort);

        assertThat(actual).hasSize(6);
        assertThat(expected).isEqualTo(actual);
        assertSqlQueriesCount(2);
    }

    @Sql(value = "insert_test_data_for_db_operations_not_in_terminal_states_count_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void countAllOperationsNotInTerminalStates() {
        int expectedCount = 2;
        Integer instantiateOperationsNotInTerminalStatesCount = lifecycleOperationRepository.countByOperationStateNotIn(
                TERMINAL_OPERATION_STATES);
        assertThat(instantiateOperationsNotInTerminalStatesCount).isEqualTo(expectedCount);
        assertSqlQueriesCount(1);
    }

    @Sql(value = "insert_test_data_for_db_operations_count_by_type_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void shouldReturnOperationsCountNotInTerminalStatesByType() {

        Integer instantiateCount = lifecycleOperationRepository.countByLifecycleOperationTypeAndOperationStateNotIn(LifecycleOperationType.INSTANTIATE,
                                                                                                                    TERMINAL_OPERATION_STATES);

        assertThat(instantiateCount).isEqualTo(3);
        assertSqlQueriesCount(1);
    }

    @Sql(value = "insert_test_data_for_db_operations_count_by_type_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @Test
    public void shouldReturnOperationsCountNotInTerminalStatesByVnfInstance() {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("ef410d07-ce1d-4c86-a73c-5343e3906a50");

        Integer instantiateCount = lifecycleOperationRepository.countByVnfInstanceAndOperationStateNotIn(vnfInstance, TERMINAL_OPERATION_STATES);

        assertThat(instantiateCount).isEqualTo(3);
        assertSqlQueriesCount(5);
    }

    private void assertSqlQueriesCount(int expectedCount) {
        long actualCount = statistics.getQueryExecutionCount();
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}