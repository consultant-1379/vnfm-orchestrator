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
package com.ericsson.vnfm.orchestrator.contracts.base;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.vnfm.orchestrator.filters.LifecycleOperationsViewQuery;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationType;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationView;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationViewRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

public class GetAllLifecycleOperationsPositiveBase extends ContractTestRunner {

    private static final String TEST_NAMESPACE = "default";
    private static final String FILTER_PARAM_TEMPLATE = String.format("(eq,namespace,%s)", TEST_NAMESPACE);
    private static final String SORT_PROPERTY_NAME  = "vnfProductName";
    private static final int TOTAL_ITEMS_NUMBER = 18;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    LifecycleOperationViewRepository lifecycleOperationViewRepository;

    @MockBean
    private LifecycleOperationsViewQuery lifecycleOperationsViewQuery;
    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.webAppContextSetup(context);

            // mock behavior when findAll() receive Pageable object with pageNumber = 0
        given(lifecycleOperationViewRepository.findAll(argThat((Pageable pageable) ->
                    pageable != null && pageable.getPageNumber() == 0 ))
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateOperationsList1(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        given(lifecycleOperationViewRepository.findAll(argThat((Pageable pageable) ->
                    pageable != null && pageable.getPageNumber() == 1))
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateOperationsList2(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        given(lifecycleOperationViewRepository.findAll(argThat((Pageable pageable) ->
                    pageable != null && pageable.getPageNumber() == 1 && pageable.getPageSize() == 5))
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateOperationsListPageSize(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        // Mock behavior when sort object initialized in Pageable
        given(lifecycleOperationViewRepository.findAll(
                argThat((Pageable pageable) -> {
                    Sort sort = pageable.getSort();
                    List<Sort.Order> orders = sort.get().collect(Collectors.toList());
                    Sort.Order order = orders.get(0);
                    Sort.Direction direction = order.getDirection();
                    String property = order.getProperty();

                    return pageable != null &&
                            orders.size() == 1 &&
                            direction.equals(Sort.Direction.ASC) &&
                            property.equals(SORT_PROPERTY_NAME);
                }))
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateClusterConfigsSortedAscByVnfProductNameOrder(), invocationOnMock.getArgument(0), TOTAL_ITEMS_NUMBER));

        given(lifecycleOperationsViewQuery.getPageWithFilter(
                    eq(FILTER_PARAM_TEMPLATE),
                    argThat((Pageable pageable) -> pageable != null && pageable.getPageNumber() == 0)
                    )
        ).willAnswer(invocationOnMock -> new PageImpl<>(populateOperationsListOnPage1WithinDefaultNamespace(), invocationOnMock.getArgument(1), TOTAL_ITEMS_NUMBER));

    }

    private List<LifecycleOperationView> populateOperationsList1() {
        List<LifecycleOperationView> operationViewList = new ArrayList<>();
        for (int i = 1; i < 16; i++) {
            LifecycleOperationView operation = createOperationView(String.valueOf(i));
            operationViewList.add(operation);
        }
        return operationViewList;
    }

    private List<LifecycleOperationView> populateOperationsList2() {
        List<LifecycleOperationView> operationViewList = new ArrayList<>();
        for (int i = 16; i < 19; i++) {
            LifecycleOperationView operation = createOperationView(String.valueOf(i));
            operationViewList.add(operation);
        }
        return operationViewList;
    }

    private List<LifecycleOperationView> populateOperationsListPageSize() {
        List<LifecycleOperationView> operationViewList = new ArrayList<>();
        for (int i = 6; i < 11; i++) {
            LifecycleOperationView operation = createOperationView(String.valueOf(i));
            operationViewList.add(operation);
        }
        return operationViewList;
    }
    private List<LifecycleOperationView> populateOperationsListOnPage1WithinDefaultNamespace() {
        List<LifecycleOperationView> operationViewList = new ArrayList<>();
        for (int i = 1; i < 16; i++) {
            LifecycleOperationView operation = createOperationView(String.valueOf(i), TEST_NAMESPACE);
            operationViewList.add(operation);
        }
        return operationViewList;
    }
    private List<LifecycleOperationView> populateClusterConfigsSortedAscByVnfProductNameOrder() {
        List<LifecycleOperationView> configs = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            if (i >= 1 &&  i <= 3) {
                configs.add(createOperationView(String.valueOf(i), "default", "Array"));
            }
            if (i > 3 && i <= 10) {
                configs.add(createOperationView(String.valueOf(i), "default", "Bro"));
            }
            if (i > 10) {
                configs.add(createOperationView(String.valueOf(i), "default", "Citrix"));
            }
        }
        return configs;
    }
    private LifecycleOperationView createOperationView(String suffix, String namespace, String vnfProductName) {
        LifecycleOperationView operationView = new LifecycleOperationView();

        operationView.setOperationOccurrenceId("99223f1c-bcb9-474c-9cb6-" + suffix);
        operationView.setOperationState(LifecycleOperationState.COMPLETED);
        operationView.setStateEnteredTime(LocalDateTime.now());
        operationView.setStartTime(LocalDateTime.now());
        operationView.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operationView.setVnfProductName(vnfProductName);
        operationView.setVnfSoftwareVersion("1.0.2");
        operationView.setClusterName("cluster-" + suffix);
        operationView.setNamespace(namespace);
        operationView.setVnfInstanceId("4d3a4b72-e724-4832-83c5-" + suffix);
        operationView.setVnfInstanceName("instance-" + suffix);
        operationView.setUsername("vnfm-" + suffix);

        return operationView;
    }
    private LifecycleOperationView createOperationView(String suffix, String namespace) {
        LifecycleOperationView operationView = new LifecycleOperationView();

        operationView.setOperationOccurrenceId("99223f1c-bcb9-474c-9cb6-" + suffix);
        operationView.setOperationState(LifecycleOperationState.COMPLETED);
        operationView.setStateEnteredTime(LocalDateTime.now());
        operationView.setStartTime(LocalDateTime.now());
        operationView.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operationView.setVnfProductName("spider");
        operationView.setVnfSoftwareVersion("1.0.2");
        operationView.setClusterName("cluster-" + suffix);
        operationView.setNamespace(namespace);
        operationView.setVnfInstanceId("4d3a4b72-e724-4832-83c5-" + suffix);
        operationView.setVnfInstanceName("instance-" + suffix);
        operationView.setUsername("vnfm-" + suffix);

        return operationView;
    }
    private LifecycleOperationView createOperationView(String suffix) {
        LifecycleOperationView operationView = new LifecycleOperationView();

        operationView.setOperationOccurrenceId("99223f1c-bcb9-474c-9cb6-" + suffix);
        operationView.setOperationState(LifecycleOperationState.COMPLETED);
        operationView.setStateEnteredTime(LocalDateTime.now());
        operationView.setStartTime(LocalDateTime.now());
        operationView.setLifecycleOperationType(LifecycleOperationType.INSTANTIATE);
        operationView.setVnfProductName("spider");
        operationView.setVnfSoftwareVersion("1.0.2");
        operationView.setClusterName("cluster-" + suffix);
        operationView.setNamespace("namespace-" + suffix);
        operationView.setVnfInstanceId("4d3a4b72-e724-4832-83c5-" + suffix);
        operationView.setVnfInstanceName("instance-" + suffix);
        operationView.setUsername("vnfm-" + suffix);

        return operationView;
    }

}
