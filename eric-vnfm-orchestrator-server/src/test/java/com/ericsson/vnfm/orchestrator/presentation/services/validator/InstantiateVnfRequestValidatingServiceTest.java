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
package com.ericsson.vnfm.orchestrator.presentation.services.validator;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.InstantiateVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleInfo;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.InvalidInputException;
import com.ericsson.vnfm.orchestrator.presentation.services.validator.impl.InstantiateVnfRequestValidatingServiceImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.vnfm.orchestrator.presentation.constants.ClusterConstants.Request.NAMESPACE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.ADD_NODE_TO_OSS;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.APPLICATION_TIME_OUT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.CMP_V2_ENROLLMENT;
import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Request.SKIP_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Suite
@SelectClasses({
        InstantiateVnfRequestValidatingServiceTest.ValidateTimeoutsTests.class,
        InstantiateVnfRequestValidatingServiceTest.ValidateScaleLevelInfoTests.class,
        InstantiateVnfRequestValidatingServiceTest.ValidateNamespaceTests.class
})
public class InstantiateVnfRequestValidatingServiceTest {

    public static class ValidateTimeoutsTests {

        private final InstantiateVnfRequestValidatingService validatingService =
                new InstantiateVnfRequestValidatingServiceImpl();

        @Test
        public void testValidateTimeoutsEmptyString() {
            Map<String, Object> map = getAdditionalTimeoutParamsMap("");
            assertThatThrownBy(() -> validatingService.validateTimeouts(map)).isInstanceOf(InvalidInputException.class)
                    .hasMessageStartingWith("Invalid timeout value specified for [applicationTimeOut]");
        }

        @Test
        public void testValidateTimeoutsNonEmptyString() {
            Map<String, Object> map = getAdditionalTimeoutParamsMap("test");
            assertThatThrownBy(() -> validatingService.validateTimeouts(map)).isInstanceOf(InvalidInputException.class)
                    .hasMessageStartingWith("Invalid timeout value specified for [applicationTimeOut]");
        }

        @Test
        public void testValidateTimeoutsNullEntry() {
            Map<String, Object> map = getAdditionalTimeoutParamsMap(null);
            assertThatThrownBy(() -> validatingService.validateTimeouts(map)).isInstanceOf(InvalidInputException.class)
                    .hasMessageStartingWith("Invalid timeout value specified for [applicationTimeOut]");
        }

        @Test
        public void testValidateTimeoutsNoErrors() {
            Map<String, Object> map = getAdditionalTimeoutParamsMap(2);
            validatingService.validateTimeouts(map);
        }

        @Test
        public void testVerifyTimeOutsCaseInsensitivity() {
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("aPpliCatIontImeOut", 30);
            validatingService.validateTimeouts(additionalParams);
        }

        @Test
        public void testImmutabilityApplicationTimeout() {
            Map<String, Object> additionalParams = getAdditionalTimeoutParamsMap("3600");
            validatingService.validateTimeouts(additionalParams);
            assertThat(additionalParams.containsKey(APPLICATION_TIME_OUT)).isTrue();
        }

        @Test
        public void testVerifySkipVerificationWithAddNodeToOssAndCMPv2EnrollmentEnabled() {
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put(SKIP_VERIFICATION, true);
            additionalParams.put(CMP_V2_ENROLLMENT, true);
            additionalParams.put(ADD_NODE_TO_OSS, true);

            assertThatThrownBy(() -> validatingService.validateSkipVerification(additionalParams)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        public void testVerifySkipVerificationDisabled() {
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put(SKIP_VERIFICATION, false);
            additionalParams.put(CMP_V2_ENROLLMENT, true);
            additionalParams.put(ADD_NODE_TO_OSS, true);

            validatingService.validateSkipVerification(additionalParams);

            assertThatNoException().isThrownBy(() -> validatingService.validateSkipVerification(additionalParams));
        }

        @Test
        public void testVerifySkipVerificationWithCMPv2EnrollmentDisabled() {
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put(SKIP_VERIFICATION, true);
            additionalParams.put(CMP_V2_ENROLLMENT, false);
            additionalParams.put(ADD_NODE_TO_OSS, true);

            validatingService.validateSkipVerification(additionalParams);

            assertThatNoException().isThrownBy(() -> validatingService.validateSkipVerification(additionalParams));
        }

        private Map<String, Object> getAdditionalTimeoutParamsMap(Object appTimeout) {
            return new HashMap<String, Object>() {{
                put(APPLICATION_TIME_OUT, appTimeout);
            }};
        }
    }

    public static class ValidateScaleLevelInfoTests {

        private final InstantiateVnfRequestValidatingService validatingService =
                new InstantiateVnfRequestValidatingServiceImpl();

        public static Collection<Object[]> scaleLevelInfoData() {
            String instantiationLevelId = "instantiation_level_1";
            List<ScaleInfo> scaleLevelInfo = TestUtils.createTargetScaleLevelInfo(Map.of("Aspect1", 2));

            return Arrays.asList(new Object[][]{
                    {instantiationLevelId, scaleLevelInfo, false},
                    {instantiationLevelId, null, true},
                    {instantiationLevelId, Collections.emptyList(), true},
                    {null, scaleLevelInfo, true},
                    {"", scaleLevelInfo, true},
                    {null, null, true},
                    {"", null, true},
                    {"", Collections.emptyList(), true},
                    {null, Collections.emptyList(), true}
            });
        }

        @ParameterizedTest
        @MethodSource("scaleLevelInfoData")
        public void testValidateScaleLevelInfoThrowException(String instantiationLevel,
                                                             List<ScaleInfo> targetScaleLevelInfo,
                                                             boolean isValid) {
            Runnable runnable = () -> {
                InstantiateVnfRequest request = new InstantiateVnfRequest();

                request.setInstantiationLevelId(instantiationLevel);
                request.setTargetScaleLevelInfo(targetScaleLevelInfo);

                validatingService.validateScaleLevelInfo(request);
            };
            if (isValid) {
                runnable.run();
            } else {
                Assertions.assertThrows(InvalidInputException.class, () -> runnable.run());
            }
        }
    }

    public static class ValidateNamespaceTests {

        private final InstantiateVnfRequestValidatingService validatingService =
                new InstantiateVnfRequestValidatingServiceImpl();

        public static Collection<Object[]> namespaceData() {
            return Arrays.asList(new Object[][]{
                    {null, true},
                    {"valid-namespace", true},
                    {"valid-namespace-with-number12", true},
                    {"n", false},
                    {"n".repeat(100), false},
                    {"test-end-", false},
                    {"-test-start", false},
                    {"test-;-invalid-character", false},
                    {"test-|-invalid-character", false},
                    {"test-'-invalid-character", false},
                    {"test-\"-invalid-character", false},
                    {"1234", false}
            });
        }

        @ParameterizedTest
        @MethodSource("namespaceData")
        public void testValidateNamespaceThrowException(String namespace, boolean isValid)  {
            Runnable runnable = () -> {
                Map<String, Object> additionalParams = new HashMap<>();
                additionalParams.put(NAMESPACE, namespace);

                validatingService.validateNamespace(additionalParams);
            };

            if (isValid) {
                runnable.run();
            } else {
                Assertions.assertThrows(InvalidInputException.class, () -> runnable.run());
            }
        }
    }
}
