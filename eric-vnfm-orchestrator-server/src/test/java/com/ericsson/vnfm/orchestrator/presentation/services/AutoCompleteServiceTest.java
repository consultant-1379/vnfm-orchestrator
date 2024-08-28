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
package com.ericsson.vnfm.orchestrator.presentation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Errors.INVALID_PARAMETER_NAME_PROVIDED_ERROR_MESSAGE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.Request.TYPE;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceViewRepository;

@SpringBootTest(classes = AutoCompleteService.class)
@MockBean (VnfInstanceViewRepository.class)
public class AutoCompleteServiceTest {

    @Autowired
    private AutoCompleteService autoCompleteService;

    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @Test
    public void testGetAutoCompleteResponse() throws ExecutionException, InterruptedException {
        List<String> mockedProductNameList = List.of("SomeProductName", "SomeOtherProductName");

        when(vnfInstanceRepository.findDistinctVnfProductName(any(), any())).thenReturn(mockedProductNameList);
        CompletableFuture<List<String>> allValue = autoCompleteService
              .getAutoCompleteResponse(TYPE, "", 0, 5);
        CompletableFuture.allOf(allValue).join();

        assertThat(allValue.get()).isNotNull().isNotEmpty().isEqualTo(mockedProductNameList);
    }

    @Test
    public void testGetAutoCompleteResponseWithNullValue() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> allValue = autoCompleteService
              .getAutoCompleteResponse(TYPE, null, 0, 5);
        CompletableFuture.allOf(allValue).join();

        assertThat(allValue.get()).isNotNull().isEmpty();
    }

    @Test
    public void testGetAutoCompleteResponseWithInvalidParameterName() {
        String parameterName = "test";

        assertThatThrownBy(
              () -> autoCompleteService.getAutoCompleteResponse(parameterName, "",0, 5))
              .isInstanceOf(InvalidInputException.class)
              .hasMessage(String.format(INVALID_PARAMETER_NAME_PROVIDED_ERROR_MESSAGE, parameterName));
    }
}
