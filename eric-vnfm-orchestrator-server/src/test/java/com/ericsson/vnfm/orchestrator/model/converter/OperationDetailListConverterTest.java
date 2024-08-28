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
package com.ericsson.vnfm.orchestrator.model.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ericsson.am.shared.vnfd.model.OperationDetail;

public class OperationDetailListConverterTest {

    private final OperationDetailListConverter converter = new OperationDetailListConverter();

    @Test
    public void testConvertToDatabaseColumn() {
        List<OperationDetail> operationDetails = List.of(
                new OperationDetail.Builder()
                        .operationName("name_1")
                        .supported(true)
                        .build(),
                new OperationDetail.Builder()
                        .operationName("name_2")
                        .supported(true)
                        .build()
        );
        String expected =
                "[{\"operationName\":\"name_1\",\"supported\":true,\"errorMessage\":null}," +
                        "{\"operationName\":\"name_2\",\"supported\":true,\"errorMessage\":null}]";
        String actual = converter.convertToDatabaseColumn(operationDetails);
        assertEquals(expected, actual);
    }

    @Test
    public void testConvertToEntityAttribute() {
        String operationDetailListStr =
                "[{\"operationName\":\"name_1\",\"supported\":true,\"errorMessage\":null}," +
                        "{\"operationName\":\"name_2\",\"supported\":true,\"errorMessage\":null}]";
        List<OperationDetail> actualOperationDetails = converter.convertToEntityAttribute(operationDetailListStr);
        assertEquals(2, actualOperationDetails.size());
    }
}
