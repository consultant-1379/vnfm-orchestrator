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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.PaginationLinks;
import com.ericsson.vnfm.orchestrator.model.URILink;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidPaginationQueryException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaginationUtils {
    private static final Pattern REGEX_SPLIT_QUERIES = Pattern.compile("\\s*,\\s*");

    private PaginationUtils() {
    }

    public static PaginationLinks buildLinks(PaginationInfo paginationInfo, String pageQuery, String... queriesToRemove) {
        final int pageSelf = paginationInfo.getNumber();
        final int pageLast = paginationInfo.getTotalPages();
        PaginationLinks links = new PaginationLinks()
                .self(buildLink(pageQuery, pageSelf, queriesToRemove))
                .first(buildLink(pageQuery, 1, queriesToRemove))
                .last(buildLink(pageQuery, pageLast, queriesToRemove));
        if (pageSelf > 1) {
            links.prev(buildLink(pageQuery, pageSelf - 1, queriesToRemove));
        }
        if (pageSelf < paginationInfo.getTotalPages()) {
            links.next(buildLink(pageQuery, pageSelf + 1, queriesToRemove));
        }
        return links;
    }

    static int getLastPage(final Page<?> page) {
        //totalPages can be 0 if the search result is empty or if the number of items < page size in the search
        return page.getTotalPages() == 0 ? 1 : page.getTotalPages();
    }

    private static URILink buildLink(String query, int queryValue,  String... queriesToRemove) {
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
        builder.replaceQueryParam(query, queryValue);
        Arrays.stream(queriesToRemove).forEach(builder::replaceQueryParam);
        return new URILink().href(builder.build().toUriString());
    }

    public static PaginationInfo buildPaginationInfo(Page<?> page) {
        checkMaxPageNumber(page);
        return new PaginationInfo()
                .number(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(getLastPage(page))
                .totalElements((int) page.getTotalElements());
    }

    public static Sort parseSort(List<String> sortQueryList, List<Sort>  validSortColumns) {
        Set<Sort.Order> orders = new LinkedHashSet<>();
        sortQueryList.stream()
                .map(PaginationUtils::getSingleSortQuery)
                .forEach(eachSortQuery -> addSingleSortOrder(eachSortQuery, validSortColumns, orders));
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(new ArrayList<>(orders));
    }

    private static List<String> getSingleSortQuery(String sortQuery) {
        return Arrays.asList(sortQuery.trim().split(REGEX_SPLIT_QUERIES.pattern()));
    }

    private static void addSingleSortOrder(List<String> sortQueryList, List<Sort> validSortValues, Set<Sort.Order> orders) {
        validSortValues.stream()
                .filter(eachDefaultSort -> eachDefaultSort.getOrderFor(sortQueryList.get(0)) != null)
                .findFirst()
                .ifPresentOrElse(defaultSort -> {
                    if (sortQueryList.size() > 1) {
                        orders.add(new Sort.Order(getDirection(sortQueryList.get(1)), sortQueryList.get(0)).ignoreCase());
                    } else {
                        orders.add(defaultSort.getOrderFor(sortQueryList.get(0)));
                    }
                }, () -> throwInvalidColumnError(sortQueryList, validSortValues));
    }

    private static Sort.Direction getDirection(String direction) {
        return Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> {
                    String invalidSortMessage = String
                            .format("Invalid sorting values :: %s. Acceptable values are :: 'desc' or 'asc' (case insensitive)", direction);
                    return new InvalidPaginationQueryException(invalidSortMessage);
                });
    }

    @Setter
    public static class PageableBuilder {
        public static final int MAX_PAGE_SIZE = 100;
        private Integer page = 1;
        private Integer size = 15;
        private Sort sort = Sort.unsorted();

        public PageableBuilder defaults(final Sort defaultSort) {
            setSort(defaultSort);
            return this;
        }

        public PageableBuilder defaults(final Integer defaultSize, final Sort defaultSort) {
            setSize(defaultSize);
            setSort(defaultSort);
            return this;
        }

        public PageableBuilder page(Integer page) {
            Optional.ofNullable(page).ifPresent(this::setPage);
            return this;
        }

        public PageableBuilder page(String page) {
            Optional.ofNullable(page).ifPresent(pageAsString -> setPage(parsePageNumber(pageAsString)));
            return this;
        }

        public PageableBuilder size(Integer size) {
            Optional.ofNullable(size).ifPresent(this::setSize);
            return this;
        }

        public PageableBuilder sort(final List<String> sortQueries, final List<Sort> validSortColumns) {
            Optional.ofNullable(sortQueries)
                    .ifPresent(queries -> setSort(parseSort(queries, validSortColumns)));
            return this;
        }
        public PageableBuilder sort(final List<String> sortQueries, final Set<String> validSortColumns) {
            return sort(sortQueries, validSortColumns, Collections.emptyMap());
        }

        public PageableBuilder sort(final List<String> sortQueries, final Set<String> validSortColumns,
                                    final Map<String, String> sortColumnMappings) {
            if (sortQueries == null || sortQueries.isEmpty()) {
                return this;
            }
            List<Sort.Order> orders = sortQueries.stream().map(String::trim)
                    .filter(sq -> !sq.isEmpty())
                    .map(sq -> buildOrder(sq, validSortColumns, sortColumnMappings))
                    .collect(Collectors.toList());
            if (!orders.isEmpty()) {
                this.sort = Sort.by(orders);
            } else {
                this.sort = Sort.unsorted();
            }
            return this;
        }

        private static Sort.Order buildOrder(final String sortExpression, final Set<String> validSortColumns,
                                             final Map<String, String> sortColumnMappings) {
            int delimiterIndex = sortExpression.indexOf(',');
            String column = sortExpression;
            Sort.Direction direction = Sort.Direction.ASC;
            if (delimiterIndex != -1) {
                column = sortExpression.substring(0, delimiterIndex);
                String sortDirection = sortExpression.substring(delimiterIndex + 1);
                if (sortDirection.length() > 0) {
                    direction = Sort.Direction.fromOptionalString(sortDirection)
                            .orElseThrow(() -> new InvalidPaginationQueryException("Invalid sorting direction: " + sortDirection));
                }
            }
            if (!validSortColumns.contains(column)) {
                throw new InvalidPaginationQueryException("Invalid column value for sorting: " + column);
            }
            column = Optional.of(column).map(sortColumnMappings::get).orElse(column);
            return direction == Sort.Direction.ASC ? Sort.Order.asc(column) : Sort.Order.desc(column);
        }

        public PageRequest build() {
            checkQueryParameters(page, size);
            return PageRequest.of(page - 1, size, sort);
        }

        private static int parsePageNumber(final String page) {
            try {
                return Integer.parseInt(page);
            } catch (NumberFormatException numberFormatException) {
                throw new InvalidPaginationQueryException(String.format("Invalid page value for nextpage_opaque_marker:: %s", page),
                                                          numberFormatException);
            }
        }

        static void checkQueryParameters(final int page, final int size) {
            if (page < 1) {
                throw new InvalidPaginationQueryException(String.format("Invalid page number:: %s, page number must be greater than 0", page));
            }

            if (size < 1) {
                throw new InvalidPaginationQueryException(String.format("Invalid page size:: %s, page size must be greater than 0", size));
            }

            if (size > MAX_PAGE_SIZE) {
                throw new InvalidPaginationQueryException(String.format("Total size of the results will be shown cannot be more than 100. Requested"
                                                                                + " page size %s", size));
            }
        }
    }

    private static void checkMaxPageNumber(final Page<?> page) {
        if (page.getNumber() != 0 && page.getNumber() + 1 > page.getTotalPages()) {
            throw new InvalidPaginationQueryException(String.format(
                    "Requested page number exceeds the total number of pages. Requested page:: %s. Total "
                            + "page size:: %s",
                    page.getNumber() + 1,
                    page.getTotalPages()));
        }
    }

    private static void throwInvalidColumnError(final List<String> sortQueryList, final List<Sort> validSortValues) {
        List<String> validColumns = validSortValues.stream()
                .flatMap(Streamable::get)
                .map(Sort.Order::getProperty)
                .collect(Collectors.toList());
        throw new InvalidPaginationQueryException(String.format("Invalid column value for sorting:: %s. Acceptable values are :: %s",
                                                                sortQueryList.get(0), validColumns));
    }
}
