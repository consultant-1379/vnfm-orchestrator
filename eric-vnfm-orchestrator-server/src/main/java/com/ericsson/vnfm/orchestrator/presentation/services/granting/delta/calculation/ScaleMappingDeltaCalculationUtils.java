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
package com.ericsson.vnfm.orchestrator.presentation.services.granting.delta.calculation;

import static com.ericsson.vnfm.orchestrator.presentation.constants.CommonConstants.REGEX_BOOLEAN_EXPRESSION;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;

import com.ericsson.am.shared.vnfd.model.ScaleMappingContainerDetails;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinition;
import com.ericsson.vnfm.orchestrator.model.granting.request.ResourceDefinitionType;
import com.ericsson.vnfm.orchestrator.presentation.services.ValuesFileService;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ScaleMappingDeltaCalculationUtils {
    public static boolean isResourceCompletelyDisabledByScaleMapping(ResourceDefinition resource,
                                                                     List<String> disabledContainers) {
        return ResourceDefinitionType.OSCONTAINER == resource.getType() &&
                disabledContainers.containsAll(resource.getResourceTemplateId());
    }

    public static boolean isResourcePartiallyDisabledByScaleMapping(ResourceDefinition resource,
                                                                    List<String> disabledContainers) {
        return ResourceDefinitionType.OSCONTAINER == resource.getType() &&
                CollectionUtils.containsAny(resource.getResourceTemplateId(), disabledContainers) &&
                !CollectionUtils.subtract(resource.getResourceTemplateId(), disabledContainers).isEmpty();
    }

    public static boolean isContainerDisabledByScaleMapping(Map.Entry<String, ScaleMappingContainerDetails> container,
                                                            Map<String, Object> valuesYamlMap) {
        Optional<String> property = Optional.ofNullable(container.getValue().getDeploymentAllowed());
        Boolean deploymentAllowed;
        if (isBooleanExpression(property.get())) {
            deploymentAllowed = evaluateDeploymentAllowedExpression(valuesYamlMap, property.get());
        } else {
            deploymentAllowed = property
                    .map(pr -> ValuesFileService.getPropertyFromValuesYamlMap(valuesYamlMap, pr, Boolean.class))
                    .orElse(null);
        }

        return deploymentAllowed != null && !deploymentAllowed;
    }

    public static boolean isBooleanExpression(String expression) {
        return REGEX_BOOLEAN_EXPRESSION.matcher(expression).find();
    }

    public static Boolean evaluateDeploymentAllowedExpression(Map<String, Object> valuesYamlMap, String expression) {
        Boolean booleanExpressionResult;

        List<String> propertiesList = extractPropertiesFromExpression(expression);
        Map<String, String> propertiesContext = ValuesFileService.getPropertiesFromValuesYamlMap(valuesYamlMap, propertiesList);

        String booleanExpression = createBooleanExpression(expression, propertiesContext);

        try {
            booleanExpressionResult = (Boolean) MVEL.eval(booleanExpression);
        } catch (Exception e) {
            LOGGER.warn("Could not evaluate boolean expression {}", booleanExpression, e);
            booleanExpressionResult = null;
        }
        return booleanExpressionResult;
    }

    public static String createBooleanExpression(String expression, Map<String, String> context) {
        String resultExpression = expression;
        if (MapUtils.isEmpty(context)) {
            LOGGER.warn("Could not create boolean expression from this expression: {}", expression);
            return resultExpression;
        }

        for (Map.Entry<String, String> entry : context.entrySet()) {
            resultExpression = resultExpression.replace(entry.getKey(), entry.getValue());
        }

        resultExpression = StringUtils.replace(resultExpression, "&", "&&");
        resultExpression = StringUtils.replace(resultExpression, "is", "==");

        return resultExpression.toLowerCase();
    }

    @SuppressWarnings("java:S1132")
    private static List<String> extractPropertiesFromExpression(String deploymentAllowed) {
        String resultExpression = deploymentAllowed;

        resultExpression = StringUtils.replace(resultExpression, "==", " ");
        resultExpression = StringUtils.replace(resultExpression, "(", " ");
        resultExpression = StringUtils.replace(resultExpression, ")", " ");

        return Arrays.stream(resultExpression.split(" "))
                .filter(x -> !x.equals("is"))
                .filter(x -> !x.equals("NULL"))
                .filter(x -> !x.equals("&"))
                .filter(x -> !x.equals("&&"))
                .filter(x -> !x.equals("||"))
                .filter(x -> !x.equals("true"))
                .filter(x -> !x.equals("false"))
                .filter(x -> !x.equals(""))
                .collect(Collectors.toList());
    }
}
