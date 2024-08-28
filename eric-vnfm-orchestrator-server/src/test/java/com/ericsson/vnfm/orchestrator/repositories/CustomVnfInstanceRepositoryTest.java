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

import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstanceByIdWithAllFields;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstances;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForPagination;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForPaginationAndSpecification;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForPaginationAndSpecificationWithAssociations;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForSorting;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForSpecification;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesForSpecificationAndSort;
import static com.ericsson.vnfm.orchestrator.repositories.testData.VnfInstanceTestData.buildVnfInstancesWithAllFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.TerminatedHelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance_;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@SpringBootTest
@TestPropertySource(properties = {"spring.flyway.locations = classpath:db/migration", "spring.flyway.clean-disabled = false"})
@Sql(value = "insert_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "delete_test_data_for_db_communication_testing.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CustomVnfInstanceRepositoryTest extends AbstractDbSetupTest {

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

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
        final Map<String, VnfInstance> expected = buildVnfInstances();

        final List<VnfInstance> actual = vnfInstanceRepository.findAll();

        assertThat(actual).hasSize(3).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(1);
    }

    @Test
    public void findAllWithFetchAssociationsLifecycleOperationsHelmChartsTerminatedHelmChartsScaleInfoEntity() {
        final Map<String, VnfInstance> expected = buildVnfInstances();

        final List<VnfInstance> actual = vnfInstanceRepository.findAll();
        vnfInstanceRepository.fetchAssociation(actual, LifecycleOperation.class, VnfInstance_.allOperations);
        vnfInstanceRepository.fetchAssociation(actual, HelmChart.class, VnfInstance_.helmCharts);
        vnfInstanceRepository.fetchAssociation(actual, TerminatedHelmChart.class, VnfInstance_.terminatedHelmCharts);
        vnfInstanceRepository.fetchAssociation(actual, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);

        assertThat(actual).hasSize(3).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertVnfInstancesAssociations(expected, actual);
        assertSqlQueriesCount(1);
    }

    @Test
    public void findAllById() {
        List<String> vnfInstanceIds = List.of("d3def1ce-4cf4-477c-aab3-21c454e6a250", "d3def1ce-4cf4-477c-aab3-21c454e6a251");
        final Map<String, VnfInstance> expected = buildVnfInstancesWithAllFields();

        final List<VnfInstance> actual = vnfInstanceRepository.findAllById(vnfInstanceIds);

        assertThat(actual).hasSize(2).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertVnfInstancesAssociations(expected, actual);
        assertSqlQueriesCount(4);
    }

    @Test
    public void selectFields() {
        List<String> fields = List.of("instantiateOssTopology");
        Map<String, String> expected = Map.of("d3def1ce-4cf4-477c-aab3-21c454e6a250", "{\"testInstantiateOssTopology250\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a251", "{\"testInstantiateOssTopology251\":\"test\"}",
                                              "d3def1ce-4cf4-477c-aab3-21c454e6a252", "{\"testInstantiateOssTopology252\":\"test\"}");
        final Map<String, VnfInstance> vnfInstanceMap = buildVnfInstances();
        List<VnfInstance> vnfInstances = new ArrayList<>(vnfInstanceMap.values());

        final Map<String, Tuple> actualFields = vnfInstanceRepository.selectFields(vnfInstances, fields);
        final Map<String, String> actual = actualFields.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue().get(1)));

        assertThat(expected).containsExactlyInAnyOrderEntriesOf(actual);
        assertSqlQueriesCount(1);
    }

    @Test
    public void findByVnfInstanceId() {
        String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a251";
        final VnfInstance expected = buildVnfInstanceByIdWithAllFields(vnfInstanceId);

        final VnfInstance actual = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);

        assertThat(expected).isEqualTo(actual);
        assertVnfInstanceAssociations(expected, actual);
        assertSqlQueriesCount(4);
    }

    @Test
    public void findById() {
        String vnfInstanceId = "d3def1ce-4cf4-477c-aab3-21c454e6a250";
        final VnfInstance expected = buildVnfInstanceByIdWithAllFields(vnfInstanceId);

        final Optional<VnfInstance> actualOptional = vnfInstanceRepository.findById(vnfInstanceId);

        assertThat(actualOptional).isPresent();
        final VnfInstance actual = actualOptional.get();
        assertThat(expected).isEqualTo(actual);
        assertVnfInstanceAssociations(expected, actual);
        assertSqlQueriesCount(4);
    }

    @Test
    public void findAllWithPagination() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForPagination();
        Pageable pageable = PageRequest.of(0, 2);

        final Page<VnfInstance> actualPaged = vnfInstanceRepository.findAll(pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(2);
        final List<VnfInstance> actual = actualPaged.getContent();

        assertThat(actual).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(3);
    }

    @Test
    public void findAllWithPaginationAndFetchAssociationsHelmChartsTerminatedHelmChartsScaleInfoEntity() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForPagination();
        Pageable pageable = PageRequest.of(0, 2);

        final Page<VnfInstance> actualPaged = vnfInstanceRepository.findAll(pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(2);
        final List<VnfInstance> actual = actualPaged.getContent();
        vnfInstanceRepository.fetchAssociation(actual, HelmChart.class, VnfInstance_.helmCharts);
        vnfInstanceRepository.fetchAssociation(actual, TerminatedHelmChart.class, VnfInstance_.terminatedHelmCharts);
        vnfInstanceRepository.fetchAssociation(actual, ScaleInfoEntity.class, VnfInstance_.scaleInfoEntity);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertVnfInstancesAssociations(expected, actual);
        assertSqlQueriesCount(3);
    }

    @Test
    public void testFindAllWithSpecificationAndPagination() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForPaginationAndSpecification();

        FilterExpressionOneValue<String> vnfInstanceNameFilter = new FilterExpressionOneValue<>();
        vnfInstanceNameFilter.setKey("vnfInstanceName");
        vnfInstanceNameFilter.setOperation(OperandOneValue.EQUAL);
        vnfInstanceNameFilter.setValue("testVnfInstanceName252");
        Specification<VnfInstance> specification = new SpecificationOneValue<>(vnfInstanceNameFilter);

        Pageable pageable = PageRequest.of(0, 3);

        final Page<VnfInstance> actualPaged = vnfInstanceRepository.findAll(specification, pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(1);
        final List<VnfInstance> actual = actualPaged.getContent();

        assertThat(actual).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(1);
    }

    @Test
    public void testFindAllWithSpecificationAndPaginationAndFetchAssociationsHelmChartsScaleInfoEntity() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForPaginationAndSpecificationWithAssociations();

        FilterExpressionOneValue<String> vnfInstanceNameFilter = new FilterExpressionOneValue<>();
        vnfInstanceNameFilter.setKey("vnfInstanceName");
        vnfInstanceNameFilter.setOperation(OperandOneValue.EQUAL);
        vnfInstanceNameFilter.setValue("testVnfInstanceName252");
        Specification<VnfInstance> specification = new SpecificationOneValue<>(vnfInstanceNameFilter);

        Pageable pageable = PageRequest.of(0, 3);

        final Page<VnfInstance> actualPaged = vnfInstanceRepository.findAll(specification, pageable);
        assertThat(actualPaged.getNumberOfElements()).isEqualTo(1);
        final List<VnfInstance> actual = actualPaged.getContent();
        vnfInstanceRepository.fetchAssociation(actual, LifecycleOperation.class, VnfInstance_.allOperations);
        vnfInstanceRepository.fetchAssociation(actual, HelmChart.class, VnfInstance_.helmCharts);
        vnfInstanceRepository.fetchAssociation(actual, TerminatedHelmChart.class, VnfInstance_.terminatedHelmCharts);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertVnfInstancesAssociations(expected, actual);
        assertSqlQueriesCount(1);
    }

    @Test
    public void testFindAllWithSpecification() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForSpecification();

        FilterExpressionMultiValue<String> vnfInstanceNameFilter = new FilterExpressionMultiValue<>();
        vnfInstanceNameFilter.setKey("vnfInstanceName");
        vnfInstanceNameFilter.setOperation(OperandMultiValue.CONTAINS);
        vnfInstanceNameFilter.setValues(List.of("testVnfInstanceName"));
        Specification<VnfInstance> specification = new SpecificationMultiValue<>(vnfInstanceNameFilter);

        final List<VnfInstance> actual = vnfInstanceRepository.findAll(specification);

        assertThat(actual).hasSize(3).containsExactlyInAnyOrderElementsOf(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(1);
    }

    @Test
    public void testFindAllWithSpecificationAndSort() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForSpecificationAndSort();

        FilterExpressionMultiValue<String> vnfInstanceNameFilter = new FilterExpressionMultiValue<>();
        vnfInstanceNameFilter.setKey("vnfInstanceName");
        vnfInstanceNameFilter.setOperation(OperandMultiValue.CONTAINS);
        vnfInstanceNameFilter.setValues(List.of("testVnfInstanceName"));
        Specification<VnfInstance> specification = new SpecificationMultiValue<>(vnfInstanceNameFilter);

        Sort sort = Sort.by("vnfInstanceName").descending();

        final List<VnfInstance> actual = vnfInstanceRepository.findAll(specification, sort);

        assertThat(actual).hasSize(3).isEqualTo(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(1);
    }

    @Test
    public void testFindAllWithSort() {
        final Map<String, VnfInstance> expected = buildVnfInstancesForSorting();
        Sort sort = Sort.by("vnfInstanceName").ascending();

        final List<VnfInstance> actual = vnfInstanceRepository.findAll(sort);

        assertThat(actual).hasSize(3).isEqualTo(new ArrayList<>(expected.values()));
        assertSqlQueriesCount(2);
    }

    private void assertVnfInstancesAssociations(Map<String, VnfInstance> expected, List<VnfInstance> actual) {
        for (VnfInstance actualVnfInstance : actual) {
            final VnfInstance expectedVnfInstance = expected.get(actualVnfInstance.getVnfInstanceId());
            assertVnfInstanceAssociations(expectedVnfInstance, actualVnfInstance);
        }
    }

    private void assertVnfInstanceAssociations(VnfInstance expectedVnfInstance, VnfInstance actualVnfInstance) {
        assertThat(expectedVnfInstance.getAllOperations()).containsExactlyInAnyOrderElementsOf(actualVnfInstance.getAllOperations());
        assertThat(expectedVnfInstance.getHelmCharts()).containsExactlyInAnyOrderElementsOf(actualVnfInstance.getHelmCharts());
        assertThat(expectedVnfInstance.getTerminatedHelmCharts()).containsExactlyInAnyOrderElementsOf(actualVnfInstance.getTerminatedHelmCharts());
        assertThat(expectedVnfInstance.getScaleInfoEntity()).containsExactlyInAnyOrderElementsOf(actualVnfInstance.getScaleInfoEntity());
        assertThat(expectedVnfInstance.getSupportedOperations()).containsExactlyInAnyOrderElementsOf(actualVnfInstance.getSupportedOperations());
    }

    private void assertSqlQueriesCount(int expectedCount) {
        long actualCount = statistics.getQueryExecutionCount();
        assertThat(actualCount).isEqualTo(expectedCount);
    }
}