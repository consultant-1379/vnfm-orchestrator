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
package com.ericsson.vnfm.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidPaginationQueryException;

public class PaginationUtilsTest {
    private static final String ERROR_BODY_SORT_ORDER = "Invalid sorting values :: %s. Acceptable values are :: 'desc' or 'asc' (case insensitive)";
    private static final String ERROR_BODY_SORT_COLUMN = "Invalid column value for sorting:: %s. Acceptable values are :: [";

    private static final Sort NAME = Sort.by("name");
    private static final Sort STATUS = Sort.by("status");
    private static final Sort TIME = Sort.by(Sort.Direction.DESC, "time");
    private static final List<Sort> DEFAULTS = List.of(NAME, STATUS, TIME);

    @Test
    public void shouldParseSingleExpression() {
        List<String> nameDefaultDirection = List.of("name");
        List<String> nameWithEmptyDirection = List.of("name,");
        List<String> time = List.of("time");

        Sort sort = PaginationUtils.parseSort(nameDefaultDirection, DEFAULTS);
        checkSingleOrderSort(sort, "name", Sort.Direction.ASC);

        sort = PaginationUtils.parseSort(nameWithEmptyDirection, DEFAULTS);
        checkSingleOrderSort(sort, "name", Sort.Direction.ASC);

        sort = PaginationUtils.parseSort(time, DEFAULTS);
        checkSingleOrderSort(sort, "time", Sort.Direction.DESC);
    }

    @Test
    public void shouldParseMultipleParameters() {
        Sort sort1 = PaginationUtils.parseSort(List.of("name", "status,desc"), DEFAULTS);
        assertTrue(sort1.isSorted());
        assertThat(sort1)
                .hasSize(2)
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("name");
                    assertTrue(order.isAscending());
                })
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("status");
                    assertTrue(order.isDescending());
                });

        Sort sort2 = PaginationUtils.parseSort(List.of("name", "status"), DEFAULTS);
        assertTrue(sort2.isSorted());
        assertThat(sort2)
                .hasSize(2)
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("name");
                    assertTrue(order.isAscending());
                })
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("status");
                    assertTrue(order.isAscending());
                });

        Sort sort3 = PaginationUtils.parseSort(List.of("name,desc", "status"), DEFAULTS);
        assertTrue(sort3.isSorted());
        assertThat(sort3)
                .hasSize(2)
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("name");
                    assertTrue(order.isDescending());
                })
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("status");
                    assertTrue(order.isAscending());
                });

        Sort sort4 = PaginationUtils.parseSort(List.of("name", "status", "time"), DEFAULTS);
        assertTrue(sort4.isSorted());
        assertThat(sort4)
                .hasSize(3)
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("name");
                    assertTrue(order.isAscending());
                })
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("status");
                    assertTrue(order.isAscending());
                })
                .anySatisfy(order -> {
                    assertThat(order.getProperty()).isEqualTo("time");
                    assertTrue(order.isDescending());
                });
    }

    @Test
    public void sortShouldFailWithInvalidParameters() {
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0", "asc"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0,asc", "name"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0,asc", "name,asc"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0", "invalid1"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("invalid0,asc", "invalid1,desc"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_COLUMN, "invalid0"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("name,ascending", "status"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_ORDER, "ascending"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("name", "status,descending"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_ORDER, "descending"));
        assertThatThrownBy(() -> PaginationUtils.parseSort(List.of("name,asc", "status,descending", "status,asc"), DEFAULTS))
                .isInstanceOf(InvalidPaginationQueryException.class).hasMessageContaining(String.format(ERROR_BODY_SORT_ORDER, "descending"));
    }

    private static void checkSingleOrderSort(Sort sort, String property, Sort.Direction direction) {
        assertTrue(sort.isSorted());
        List<Sort.Order> orders = sort.toList();
        assertEquals(1, orders.size());
        assertEquals(property, orders.get(0).getProperty());
        assertEquals(direction, orders.get(0).getDirection());
    }
}
