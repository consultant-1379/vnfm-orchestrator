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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.lcm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourceDefinitionCalculation;
import com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation.GrantingResourcesDeltaCalculationImpl;

@ExtendWith(MockitoExtension.class)
public class GrantingResourceDefinitionCalculationImplTest {
    @InjectMocks
    private GrantingResourceDefinitionCalculation grantingResourceDefinitionCalculation = new GrantingResourceDefinitionCalculationImpl();

    @Mock
    private GrantingResourcesDeltaCalculationImpl deltaCalculation;

    @Test
    public void test() throws IOException, URISyntaxException {
        String descriptorModel = readDataFromFile(getClass(), "descriptor-model.json");
        JSONObject vnfd = new JSONObject(descriptorModel);
        final Map<String, Integer> resourcesWithCount = Map.of("resource-1", 2,
                                                               "resource-2", 3);

        grantingResourceDefinitionCalculation.calculate(vnfd, resourcesWithCount, "vnfdId");

        Mockito.verify(deltaCalculation).calculateResources(any(), eq(resourcesWithCount), eq("vnfdId"));
    }
}
