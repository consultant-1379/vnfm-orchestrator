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
package com.ericsson.vnfm.orchestrator.filters;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.assertMultiValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.TestUtils.assertOneValueFilterExpression;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType.INSTANTIATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.STATE_ENTERED_TIME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Common.LIFECYCLE_OPERATIONS_JOIN_PREFIX;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.OPERATION_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.LifecycleOperationsFilters.STATE_ENTERED_TIME_FILTER;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_NAME;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VnfResources.ADDED_TO_OSS;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.model.entity.VnfResourceView;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"spring.flyway.locations = classpath:db/migration, db/dev"})
public class VnfResourceViewQueryTest extends AbstractDbSetupTest {

    @Autowired
    private VnfInstanceRepository instanceRepository;

    @Autowired
    private VnfResourceViewQuery resourceViewQuery;

    @Test
    public void shouldFilterInstanceWithOnlyModifyInfoOperation() {
        VnfInstance notInstantiatedVnfInstanceWithOnlyModifyInfo =
              instanceRepository.findByVnfInstanceId("b6f2ffb4-d1f3-4c01-afb8-7791a77150f6");
        Page<VnfResourceView> page = resourceViewQuery.getPageWithFilter("", Pageable.unpaged());
        List<String> retrievedIds = page.get().map(VnfResourceView::getVnfInstanceId).collect(Collectors.toList());
        boolean containNotInstantiatedVnfInstanceWithOnlyModifyInfo =
              retrievedIds.contains(notInstantiatedVnfInstanceWithOnlyModifyInfo.getVnfInstanceId());
        boolean containModifyInfoOperation =
              containsLifecycleOperation(notInstantiatedVnfInstanceWithOnlyModifyInfo, LifecycleOperationType.MODIFY_INFO);

        assertThat(containModifyInfoOperation).isTrue();
        assertThat(retrievedIds).isNotEmpty();
        assertThat(containNotInstantiatedVnfInstanceWithOnlyModifyInfo).isFalse();
    }

    @Test
    public void shouldNotFilterInstanceWithOnlyModifyInfoOperationAfterInstantiation() {
        VnfInstance instantiatedVnfInstanceWithModifyInfo =
              instanceRepository.findByVnfInstanceId("b0a33436-1609-476c-83cd-ec474e5a828b");
        Page<VnfResourceView> page = resourceViewQuery.getPageWithFilter("", Pageable.unpaged());
        List<String> retrievedIds = page.get().map(VnfResourceView::getVnfInstanceId).collect(Collectors.toList());
        boolean containInstantiatedVnfInstanceWithModifyInfo =
              retrievedIds.contains(instantiatedVnfInstanceWithModifyInfo.getVnfInstanceId());
        boolean containModifyInfoOperation =
              containsLifecycleOperation(instantiatedVnfInstanceWithModifyInfo, LifecycleOperationType.MODIFY_INFO);
        boolean containInstantiateOperation =
              containsLifecycleOperation(instantiatedVnfInstanceWithModifyInfo, INSTANTIATE);

        assertThat(containInstantiateOperation).isTrue();
        assertThat(containModifyInfoOperation).isTrue();
        assertThat(retrievedIds).isNotEmpty();
        assertThat(containInstantiatedVnfInstanceWithModifyInfo).isTrue();
    }

    @Test
    public void shouldFilterOnInstanceProperties() {
        List<String> instanceIds = loadInstanceIds();
        Pageable pageable = PageRequest.of(0, 5, Sort.by(VNF_INSTANCE_NAME));
        Page<VnfResourceView> page = resourceViewQuery.getPageWithFilter("(cont," + VNF_INSTANCE_NAME + ",paged-)", pageable);
        List<String> retrievedIds = page.get().map(VnfResourceView::getVnfInstanceId).collect(Collectors.toList());

        assertThat(instanceIds).isNotEmpty();
        assertThat(retrievedIds).isNotEmpty();
        assertThat(instanceIds.subList(0, Math.min(5, retrievedIds.size()))).isEqualTo(retrievedIds);
        assertThat(instanceIds.size()).isEqualTo(page.getTotalElements());
    }

    @Test
    public void shouldFilterOnOperationProperties() {
        List<String> instanceIds = loadUpgradedInstanceIds();
        Pageable pageable = PageRequest.of(0, 5, Sort.by(VNF_INSTANCE_NAME));
        Page<VnfResourceView> page = resourceViewQuery.getPageWithFilter(
                "(eq," + OPERATION_FILTER + "," + INSTANTIATE.name() + ");(cont," + VNF_INSTANCE_NAME + ",paged-)", pageable);
        List<String> retrievedIds = page.get().map(VnfResourceView::getVnfInstanceId).collect(Collectors.toList());

        assertThat(instanceIds).isNotEmpty();
        assertThat(retrievedIds).isNotEmpty();
        assertThat(instanceIds.subList(0, Math.min(5, retrievedIds.size()))).isEqualTo(retrievedIds);
        assertThat(instanceIds.size()).isEqualTo(page.getTotalElements());
    }

    @Test
    public void testCreateFilterExpressionOneValue() {
        InstantiationState enumExpectedValue = InstantiationState.INSTANTIATED;
        String enumValue = enumExpectedValue.name();
        FilterExpressionOneValue result = resourceViewQuery
                .createFilterExpressionOneValue(INSTANTIATION_STATE, enumValue,
                                                "eq");

        assertOneValueFilterExpression(INSTANTIATION_STATE, OperandOneValue.EQUAL, enumExpectedValue, result);

        result = resourceViewQuery
                .createFilterExpressionOneValue(ADDED_TO_OSS, "true",
                                                "eq");

        assertOneValueFilterExpression(ADDED_TO_OSS, OperandOneValue.EQUAL, true, result);

        LocalDateTime dateExpectedValue = LocalDateTime.now();
        String dateValue = dateExpectedValue.toString();
        result = resourceViewQuery
                .createFilterExpressionOneValue(STATE_ENTERED_TIME_FILTER, dateValue,
                                                "eq");

        assertOneValueFilterExpression(LIFECYCLE_OPERATIONS_JOIN_PREFIX + STATE_ENTERED_TIME, OperandOneValue.EQUAL, dateExpectedValue, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.INNER);

        String stringValue = "testNamespace";
        result = resourceViewQuery
                .createFilterExpressionOneValue(NAMESPACE, stringValue,
                                                "eq");

        assertOneValueFilterExpression(NAMESPACE, OperandOneValue.EQUAL, stringValue, result);
    }

    @Test
    public void testCreateFilterExpressionMultiValue() {
        List<InstantiationState> enumExpectedValues = Arrays.asList(InstantiationState.INSTANTIATED, InstantiationState.NOT_INSTANTIATED);
        List<String> enumValues = enumExpectedValues.stream().map(InstantiationState::name).collect(Collectors.toList());
        FilterExpressionMultiValue result = resourceViewQuery.createFilterExpressionMultiValue(INSTANTIATION_STATE, enumValues, "cont");

        assertMultiValueFilterExpression(INSTANTIATION_STATE, OperandMultiValue.CONTAINS, enumExpectedValues, result);

        result = resourceViewQuery.createFilterExpressionMultiValue(ADDED_TO_OSS, List.of("true"), "in");

        assertMultiValueFilterExpression(ADDED_TO_OSS, OperandMultiValue.IN, List.of(true), result);

        List<LocalDateTime> dateExpectedValues = Arrays.asList(LocalDateTime.now().minusDays(2), LocalDateTime.now());
        List<String> dateValues = dateExpectedValues.stream().map(LocalDateTime::toString).collect(Collectors.toList());
        result = resourceViewQuery.createFilterExpressionMultiValue(STATE_ENTERED_TIME_FILTER, dateValues, "in");

        assertMultiValueFilterExpression(LIFECYCLE_OPERATIONS_JOIN_PREFIX + STATE_ENTERED_TIME, OperandMultiValue.IN, dateExpectedValues, result);
        assertThat(result.getJoinType()).isEqualTo(JoinType.INNER);

        List<String> stringValues = Arrays.asList("testNamespace1", "testNamespace2");
        result = resourceViewQuery.createFilterExpressionMultiValue(NAMESPACE, stringValues, "nin");

        assertMultiValueFilterExpression(NAMESPACE, OperandMultiValue.NOT_IN, stringValues, result);
    }

    private List<String> loadInstanceIds() {
        return instanceRepository.findAll(Sort.by(VNF_INSTANCE_NAME)).stream()
                .filter(i -> i.getVnfInstanceName().contains("paged-"))
                .sorted(Comparator.comparing(VnfInstance::getVnfInstanceName))
                .map(VnfInstance::getVnfInstanceId)
                .collect(Collectors.toList());
    }

    private List<String> loadUpgradedInstanceIds() {
        List<VnfInstance> allInstances = instanceRepository.findAll(Sort.by(VNF_INSTANCE_NAME));
        final List<String> ids = allInstances.stream()
                .map(VnfInstance::getVnfInstanceId)
                .collect(Collectors.toList());

        return instanceRepository.findAllById(ids).stream()
                .filter(i -> i.getVnfInstanceName().contains("paged-"))
                .filter(i -> i.getAllOperations().stream()
                        .anyMatch(o -> o.getOperationOccurrenceId().equals(i.getOperationOccurrenceId())
                                && o.getLifecycleOperationType() == INSTANTIATE))
                .sorted(Comparator.comparing(VnfInstance::getVnfInstanceName))
                .map(VnfInstance::getVnfInstanceId)
                .collect(Collectors.toList());
    }

    private boolean containsLifecycleOperation(VnfInstance vnfInstance, LifecycleOperationType lifecycleOperationType) {
        return vnfInstance.getAllOperations()
                    .stream()
                    .anyMatch(lifecycleOp -> lifecycleOp.getLifecycleOperationType().equals(lifecycleOperationType));
    }
}
