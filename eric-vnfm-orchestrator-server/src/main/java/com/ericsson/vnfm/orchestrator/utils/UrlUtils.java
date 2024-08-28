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

import static com.ericsson.am.shared.http.HttpUtility.getHostUrl;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_OP_OCCS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.LCM_VNF_INSTANCES;
import static com.ericsson.vnfm.orchestrator.utils.PaginationUtils.buildLinks;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpHeaders;

import com.ericsson.vnfm.orchestrator.model.PaginationInfo;
import com.ericsson.vnfm.orchestrator.model.PaginationLinks;
import com.ericsson.vnfm.orchestrator.model.URILink;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponse;
import com.ericsson.vnfm.orchestrator.model.VnfInstanceResponseLinks;

public final class UrlUtils {

    public static final String PAGINATION = "PaginationInfo";
    public static final String FIRST = "first";
    public static final String PREVIOUS = "previous";
    public static final String SELF = "self";
    public static final String NEXT = "next";
    public static final String LAST = "last";
    public static final String NUMBER = "number";
    public static final String SIZE = "size";
    public static final String TOTAL_PAGES = "totalPages";
    public static final String TOTAL_ELEMENTS = "totalElements";
    public static final String NEXTPAGE_OPAQUE_MARKER = "nextpage_opaque_marker";
    public static final String PAGE = "page";

    private UrlUtils() {
        // no constructor
    }

    public static void updateVnfInstanceWithLinks(final VnfInstanceResponse vnfInstanceResponse) {
        final VnfInstanceResponseLinks links = new VnfInstanceResponseLinks();

        final URILink self = getVnfInstanceLink(vnfInstanceResponse.getId());
        links.setSelf(self);
        setInstantiateLink(links, self);
        setTerminateLink(links, self);
        setChangeVnfPkgLink(links, self);
        setScaleLink(links, self);

        vnfInstanceResponse.setLinks(links);
    }

    public static URILink getVnfInstanceLink(final String vnfInstanceId) {
        final String host = getHostUrl();
        final URILink vnfInstanceLink = new URILink();

        vnfInstanceLink.setHref(host + LCM_VNF_INSTANCES + vnfInstanceId);
        return vnfInstanceLink;
    }

    public static URILink getVnfInstanceLink(String vnfmHost, String vnfInstanceId) {
        final URILink vnfInstanceLink = new URILink();
        vnfInstanceLink.setHref("https://" + vnfmHost + LCM_VNF_INSTANCES + vnfInstanceId);
        return vnfInstanceLink;
    }

    public static URILink getVnfLcmOpOccLink(String vnfmHost, String operationOccurrenceId) {
        final URILink vnfInstanceLink = new URILink();
        vnfInstanceLink.setHref("https://" + vnfmHost + LCM_OP_OCCS + operationOccurrenceId);
        return vnfInstanceLink;
    }

    public static void setChangeVnfPkgLink(final VnfInstanceResponseLinks links, final URILink self) {
        final URILink changePackage = new URILink();
        changePackage.setHref(self.getHref() + "/change_vnfpkg");
        links.setChangeVnfpkg(changePackage);
    }

    public static void setScaleLink(final VnfInstanceResponseLinks links, final URILink self) {
        final URILink scale = new URILink();
        scale.setHref(self.getHref() + "/scale");
        links.setScale(scale);
    }

    private static void setInstantiateLink(final VnfInstanceResponseLinks links, final URILink self) {
        final URILink instantiate = new URILink();
        instantiate.setHref(self.getHref() + "/instantiate");
        links.setInstantiate(instantiate);
    }

    public static void setTerminateLink(final VnfInstanceResponseLinks links, final URILink self) {
        final URILink terminate = new URILink();
        terminate.setHref(self.getHref() + "/terminate");
        links.setTerminate(terminate);
    }

    public static HttpHeaders getHttpHeaders(final String lifeCycleOperationOccurrenceId) {
        HttpHeaders headers = new HttpHeaders();
        final String host = getHostUrl();
        headers.add(HttpHeaders.LOCATION, host + LCM_OP_OCCS + lifeCycleOperationOccurrenceId);
        return headers;
    }

    public static HttpHeaders createPaginationHttpHeaders(final PaginationInfo vnfInstancePage) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, createLinkHeaderValue(vnfInstancePage));
        headers.add(PAGINATION, createPaginationHeaderValue(vnfInstancePage));
        return headers;
    }

    private static String createPaginationHeaderValue(final PaginationInfo vnfInstancePage) {
        HashMap<String, Integer> pagination = new HashMap<>();
        pagination.put(NUMBER, vnfInstancePage.getNumber());
        pagination.put(SIZE, vnfInstancePage.getSize());
        pagination.put(TOTAL_PAGES, vnfInstancePage.getTotalPages());
        pagination.put(TOTAL_ELEMENTS, vnfInstancePage.getTotalElements());

        return pagination.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    private static String createLinkHeaderValue(final PaginationInfo vnfInstancePage) {
        PaginationLinks headerLinks = buildLinks(vnfInstancePage, NEXTPAGE_OPAQUE_MARKER, PAGE);
        Optional<Link> first = Optional.ofNullable(headerLinks.getFirst()).map(link -> Link.of(link.getHref(), LinkRelation.of(FIRST)));
        Optional<Link> prev = Optional.ofNullable(headerLinks.getPrev()).map(link -> Link.of(link.getHref(), LinkRelation.of(PREVIOUS)));
        Optional<Link> self = Optional.ofNullable(headerLinks.getSelf()).map(link -> Link.of(link.getHref(), LinkRelation.of(SELF)));
        Optional<Link> next = Optional.ofNullable(headerLinks.getNext()).map(link -> Link.of(link.getHref(), LinkRelation.of(NEXT)));
        Optional<Link> last = Optional.ofNullable(headerLinks.getLast()).map(link -> Link.of(link.getHref(), LinkRelation.of(LAST)));

        return Stream.of(first, prev, self, next, last)
                .flatMap(link -> link.map(Link::toString).stream())
                .collect(Collectors.joining(","));
    }
}
