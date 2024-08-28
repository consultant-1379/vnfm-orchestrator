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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest.TypeEnum;
import com.ericsson.vnfm.orchestrator.model.entity.HelmChart;
import com.ericsson.vnfm.orchestrator.model.entity.ReplicaDetails;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.exceptions.NotInstantiatedException;
import com.ericsson.vnfm.orchestrator.presentation.services.packageing.PackageService;
import com.ericsson.vnfm.orchestrator.presentation.services.scale.ScaleService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


@AutoConfigureMockMvc
public class ScaleServiceTest extends AbstractDbSetupTest {

    private static final String VNFD_ID_1 = "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2";
    private static final String PAYLOAD = "Payload";
    private static final String POLICIES_NOT_PRESENT_ERROR_MESSAGE = "Policies not present for " + "instance %s";
    private static final String REQUIRED_SCALE_LEVEL_EXCEEDS_MAX_LIMIT_SCALE_LEVEL = "Scale Out operation cannot be "
            + "performed for aspect Id %s because required scale level %s exceeds max scale level %s and current "
            + "scale level is %s";
    private static final String REQUIRED_SCALE_LEVEL_EXCEEDS_MIN_LIMIT_SCALE_LEVEL = "Scale In operation cannot be "
            + "performed for aspect Id %s because required scale level %s exceeds min scale level 0 and current "
            + "scale level is %s";
    private static final String SCALE_INFO_MISSING_IN_VNF_INSTANCE =
            "Scale not supported as scale info is not " + "present for vnf instance %s";

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @Autowired
    private ScaleService scaleService;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private PackageService packageService;

    @Test
    public void testSetReplicaParameterForScaleRequest() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350001", "scale-service/test-vnfd.json");

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("d8a8da64");
        ScaleVnfRequest scaleRequest = new ScaleVnfRequest();
        scaleRequest.setNumberOfSteps(2);
        scaleRequest.setAspectId("Aspect2");
        scaleRequest.setType(TypeEnum.OUT);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        List<HelmChart> allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isEqualTo(8);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(8);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(8);
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(1);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(1);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(1);
            }
        }
        for (ScaleInfoEntity entity : vnfInstance.getScaleInfoEntity()) {
            if (entity.getAspectId().equals("Aspect2")) {
                entity.setScaleLevel(2);
            }
        }

        scaleRequest.setNumberOfSteps(1);
        scaleRequest.setAspectId("Aspect2");
        scaleRequest.setType(TypeEnum.IN);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);

        allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isEqualTo(5);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(5);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(5);
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(1);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(1);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(1);
            }
        }
    }

    @Test
    public void testSetReplicaParameterForScaleRequestWithAutoScalingParameterNotPresent() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350001", "scale-service/test-vnfd.json");

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("d8a8da65");
        ScaleVnfRequest scaleRequest = new ScaleVnfRequest();
        scaleRequest.setNumberOfSteps(2);
        scaleRequest.setAspectId("Aspect2");
        scaleRequest.setType(TypeEnum.OUT);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        List<HelmChart> allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(8);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(1);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(1);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(1);
            }
        }
    }


    @Test
    public void testSetReplicaParameterForNonLinearScaling() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350001", "scale-service/test-vnfd.json");

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("d8a8da65");
        ScaleVnfRequest scaleRequest = new ScaleVnfRequest();
        scaleRequest.setNumberOfSteps(1);
        scaleRequest.setAspectId("Aspect1");
        scaleRequest.setType(TypeEnum.OUT);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        List<HelmChart> allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(2);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(6);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(6);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(6);
            }
        }
        for (ScaleInfoEntity entity : vnfInstance.getScaleInfoEntity()) {
            if (entity.getAspectId().equals("Aspect1")) {
                entity.setScaleLevel(1);
            }
        }

        scaleRequest.setNumberOfSteps(5);
        scaleRequest.setAspectId("Aspect1");
        scaleRequest.setType(TypeEnum.OUT);

        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(2);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(17);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(17);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(17);
            }
        }
        for (ScaleInfoEntity entity : vnfInstance.getScaleInfoEntity()) {
            if (entity.getAspectId().equals("Aspect1")) {
                entity.setScaleLevel(6);
            }
        }

        scaleRequest.setNumberOfSteps(5);
        scaleRequest.setAspectId("Aspect1");
        scaleRequest.setType(TypeEnum.IN);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(2);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(6);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(6);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(6);
            }
        }
        for (ScaleInfoEntity entity : vnfInstance.getScaleInfoEntity()) {
            if (entity.getAspectId().equals("Aspect1")) {
                entity.setScaleLevel(1);
            }
        }

        scaleRequest.setNumberOfSteps(1);
        scaleRequest.setAspectId("Aspect1");
        scaleRequest.setType(TypeEnum.IN);
        scaleService.setReplicaParameterForScaleRequest(vnfInstance, scaleRequest);
        allHelmChart = vnfInstance.getHelmCharts();
        for (HelmChart chart : allHelmChart) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                        chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                        });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(2);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                        chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                        });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(1);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(1);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(1);
            }
        }
    }

    @Test
    public void testSetReplicaParameterForScaleInfo() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350001", "scale-service/test-vnfd.json");

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("d8a8da65");
        List<ScaleInfoEntity> allScaleInfo = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : allScaleInfo) {
            if (scaleInfo.getAspectId().equals("Aspect1")) {
                scaleInfo.setScaleLevel(3);
            }
            if (scaleInfo.getAspectId().equals("Aspect2")) {
                scaleInfo.setScaleLevel(5);
            }
        }
        scaleService.setReplicaParameterForScaleInfo(vnfInstance);
        for (HelmChart chart : vnfInstance.getHelmCharts()) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(17);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                    chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                    });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(11);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(11);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(11);
            }
        }
        for (ScaleInfoEntity info : vnfInstance.getScaleInfoEntity()) {
            if (info.getAspectId().equals("Aspect1")) {
                assertThat(info.getScaleLevel()).isEqualTo(3);
            } else if (info.getAspectId().equals("Aspect2")) {
                assertThat(info.getScaleLevel()).isEqualTo(5);
            } else {
                fail("It should fail");
            }
        }
    }

    @Test
    public void testSetReplicaParameterForScaleInfoWithMixedVdus() throws Exception {
        whenOnboardingRespondsWithVnfd("9392468011745350007", "scale-service/mixed_vdus_vnfd.json");

        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance("d8a8da65efe");
        List<ScaleInfoEntity> allScaleInfo = vnfInstance.getScaleInfoEntity();
        for (ScaleInfoEntity scaleInfo : allScaleInfo) {
            if (scaleInfo.getAspectId().equals("Aspect1")) {
                scaleInfo.setScaleLevel(3);
            }
        }
        scaleService.setReplicaParameterForScaleInfo(vnfInstance);
        for (HelmChart chart : vnfInstance.getHelmCharts()) {
            if (chart.getPriority() == 2) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                        chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                        });
                ReplicaDetails replica = replicaDetails.get("test-cnf");
                assertThat(replica.getMinReplicasCount()).isNull();
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(5);
                assertThat(replica.getMaxReplicasCount()).isNull();
            }
            if (chart.getPriority() == 1) {
                Map<String, ReplicaDetails> replicaDetails = mapper.readValue(
                        chart.getReplicaDetails(), new TypeReference<Map<String, ReplicaDetails>>() {
                        });
                ReplicaDetails replica = replicaDetails.get("eric-pm-bulk-reporter");
                assertThat(replica.getMinReplicasCount()).isEqualTo(1);
                assertThat(replica.getCurrentReplicaCount()).isEqualTo(1);
                assertThat(replica.getMaxReplicasCount()).isEqualTo(1);
            }
        }

        ScaleInfoEntity info = vnfInstance.getScaleInfoEntity().get(0);
        assertThat(info.getScaleLevel()).isEqualTo(3);
    }

    @Test
    public void testGetValidScaleParametersForScaleOut() {
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(VNFD_ID_1);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 3));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(13);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(13);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(13);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForScaleOutOperation() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 3);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(13);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(13);
    }

    @Test
    public void testGetValidScaleParametersForScaleIn() {
        String vnfInstanceId = "d8a8da6b-4488-4b18";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForScaleInOperation() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b18";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(5);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(5);
    }

    @Test
    public void testGetValidScaleParametersForScaleInToTwoLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b18";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 2));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(1);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(1);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForScaleInToTwoLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b18";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 2);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(1);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(1);
    }

    @Test
    public void testGetValidScaleParametersForScaleMoreThanMaxScale() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThatThrownBy(() -> scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 11))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(REQUIRED_SCALE_LEVEL_EXCEEDS_MAX_LIMIT_SCALE_LEVEL,
                                          PAYLOAD, 11, 10, 0));
    }

    @Test
    public void testGetScaleParametersForScaleNotSupported() {
        String vnfInstanceId = "e3def1ce-4cf4-477c-aab3-21c454e6a389";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThatThrownBy(() -> scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                        "Payload_2", 2))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(POLICIES_NOT_PRESENT_ERROR_MESSAGE, vnfInstanceId));
    }

    @Test
    public void testGetScaleInfoMissing() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f9f9e123";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThatThrownBy(() -> scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                        "Payload_2", 2))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(SCALE_INFO_MISSING_IN_VNF_INSTANCE, vnfInstanceId));
    }

    @Test
    public void testGetMissingScalingAspect() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThatThrownBy(() -> scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                        "Payload_9", 2))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Provided aspectId [Payload_9] does not exist in VNFD");
    }

    @Test
    public void testForNonLinearScalingForScaleOutForFirstScaleLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleOutToFirstLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(5);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(5);
    }

    @Test
    public void testForNonLinearScalingForScaleOutForSecondScaleLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 2));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(7);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(7);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(7);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleOutToSecondLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 2);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(7);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(7);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(1);
    }

    @Test
    public void testForNonLinearScalingForScaleOutForThirdLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 3));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(14);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(14);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(14);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleOutToThirdLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 3);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(14);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(14);
    }

    @Test
    public void testForNonLinearScalingForScaleOutForForthLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 4));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleOutToForthLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 4);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(21);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(21);
    }

    @Test
    public void testForNonLinearScalingForScaleOutForFifthLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 5));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(28);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(28);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(28);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleOutToFifthLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f989";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 5);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(28);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(28);
    }

    @Test
    public void testForNonLinearScalingForScaleInFromFifthLevelToForthLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b11";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(21);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromFifthLevelToForthLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b11";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(21);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(21);
    }

    @Test
    public void testForNonLinearScalingForScaleInFromForthLevelToThirdLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b12";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        assertThat(scaleParameters.get("PL__scaled_vm")).containsValues(14, 14, 14);
        assertThat(scaleParameters.get("TL_scaled_vm")).containsValues(14, 14, 14);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromForthLevelToThirdLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b12";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(14);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(14);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(21);
    }

    @Test
    public void testForNonLinearScalingForScaleInFromThirdLevelToSecondLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b13";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        assertThat(scaleParameters.get("PL__scaled_vm")).containsValues(7, 7, 7);
        assertThat(scaleParameters.get("TL_scaled_vm")).containsValues(7, 7, 7);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromThirdLevelToSecondLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b13";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(7);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(7);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(14);
    }

    @Test
    public void testForNonLinearScalingForScaleInFromSecondLevelToFirstLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b14";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        assertThat(scaleParameters.get("PL__scaled_vm")).containsValues(5, 5, 5);
        assertThat(scaleParameters.get("TL_scaled_vm")).containsValues(5, 5, 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromSecondLevelToFirstLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b14";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(5);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(5);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(7);
    }

    @Test
    public void testForNonLinearScalingForScaleInFromFirstLevelToZeroLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b15";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 1));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        assertThat(scaleParameters.get("PL__scaled_vm")).containsValues(1, 1, 1);
        assertThat(scaleParameters.get("TL_scaled_vm")).containsValues(1, 1, 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromFirstLevelToZeroLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b15";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 1);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(1);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(1);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(5);
    }

    @Test
    public void testForNonLinearScalingForScaleInForLowerThanZeroLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b16";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        assertThatThrownBy(() -> scaleService.getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                                                    PAYLOAD, 1))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format(REQUIRED_SCALE_LEVEL_EXCEEDS_MIN_LIMIT_SCALE_LEVEL, PAYLOAD, 1, 0));
    }

    @Test
    public void testForNonLinearScalingForScaleInFromFifthLevelToFirstLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b11";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN,
                                                                       PAYLOAD, 4));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        for (String target : scaleParameters.keySet()) {
            Map<String, Integer> parameters = scaleParameters.get(target);
            assertThat(parameters).isNotNull().isNotEmpty();
            if (target.equals("PL__scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else if (target.equals("TL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else if (target.equals("CL_scaled_vm")) {
                for (String key : parameters.keySet()) {
                    assertThat(parameters.get(key)).isNotNull().isEqualTo(5);
                }
            } else {
                fail("Some extra replica parameters present");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromFifthLevelToFirstLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b11";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.IN, PAYLOAD, 4);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(5);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(5);
    }

    @Test
    public void testForNonLinearScalingForScaleOutFromSecondLevelToFifthLevel() {
        String vnfInstanceId = "d8a8da6b-4488-4b17";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        Map<String, Map<String, Integer>> scaleParameters = scaleService
                .getScaleParameters(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                       PAYLOAD, 3));
        assertThat(scaleParameters).isNotNull().isNotEmpty();
        assertThat(scaleParameters.get("PL__scaled_vm")).containsValues(28,28,28);
        assertThat(scaleParameters.get("TL_scaled_vm")).containsValues(28,28,28);
    }

    @Test
    public void testGetScaleParametersWhenOtherAspectsAlreadyScaled() throws IOException, URISyntaxException {
        // Given
        final VnfInstance vnfInstance = new VnfInstance();
        vnfInstance.setPolicies(readDataFromFile("sm-50216-scaling-policies.json"));
        List<ScaleInfoEntity> scaleInfoEntities = new ArrayList<>();
        addScaleInfoEntity(scaleInfoEntities, "nrf_management_mgmt", 1);
        addScaleInfoEntity(scaleInfoEntities, "nrf_notify_agent", 0);
        addScaleInfoEntity(scaleInfoEntities, "nrf_management_notification", 0);
        addScaleInfoEntity(scaleInfoEntities, "nrf_discovery", 0);
        addScaleInfoEntity(scaleInfoEntities, "nssf_slice_selection_control", 0);
        addScaleInfoEntity(scaleInfoEntities, "nrf_register_agent", 0);
        addScaleInfoEntity(scaleInfoEntities, "nrf_discovery_agent", 0);
        vnfInstance.setScaleInfoEntity(scaleInfoEntities);
        vnfInstance.setResourceDetails("{\"eric-nrf.eric-nrf-management.mgmt\":3,\"eric-nrfagent"
                + ".eric-nrf-discovery-agent\":2,\"eric-nssf.eric-nssf-slice-selection-control"
                + ".sliceselectioncontrol\":2,\"eric-nrfagent.eric-nrf-register-agent\":2,\"eric-nrfagent"
                + ".eric-nrf-notify-agent\":2,\"eric-nrf.eric-nrf-discovery.disc\":2,\"eric-nrf"
                + ".eric-nrf-management.notification\":2}");

        // When
        Map<String, Map<String, Integer>> scaleParameters = scaleService.getScaleParameters(vnfInstance,
                                                                                            createScaleVnfRequest(TypeEnum.OUT, "nrf_discovery", 2));

        // Then
        assertThat(scaleParameters).hasSize(7);
        assertThat(scaleParameters.get("eric-nrf.eric-nrf-discovery.disc")).containsValues(4,4,4);
        assertThat(scaleParameters.get("eric-nssf.eric-nssf-slice-selection-control.sliceselectioncontrol")).containsValues(2,2,2);
        assertThat(scaleParameters.get("eric-nrf.eric-nrf-management.mgmt")).containsValues(3,3,3);
    }

    private void addScaleInfoEntity(final List<ScaleInfoEntity> scaleInfoEntities, final String aspectId,
            final int level) {
        ScaleInfoEntity scaleInfoEntity = new ScaleInfoEntity();
        scaleInfoEntity.setAspectId(aspectId);
        scaleInfoEntity.setScaleLevel(level);
        scaleInfoEntities.add(scaleInfoEntity);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResourcesDetailsForNonLinearScaleInFromSecondLevelToFifthLevel() throws Exception{
        String vnfInstanceId = "d8a8da6b-4488-4b17";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        ScaleVnfRequest scaleVnfRequest = createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT, PAYLOAD, 3);
        String resourcesDetails = scaleService.updateResourcesDetails(vnfInstance, scaleVnfRequest);
        Map<String, Integer> currentScaleResourceDetails = mapper.readValue(resourcesDetails, Map.class);
        assertThat(currentScaleResourceDetails).isNotNull().isNotEmpty();
        assertThat(currentScaleResourceDetails.get("PL__scaled_vm")).isEqualTo(28);
        assertThat(currentScaleResourceDetails.get("CL_scaled_vm")).isEqualTo(28);
        assertThat(currentScaleResourceDetails.get("TL_scaled_vm")).isEqualTo(7);
    }

    @Test
    public void testUpdateResourcesDetails() {
        String vnfInstanceId = "d8a8da6b-4488-4b14-a578-38b4f9f9e5e2";
        VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        String updatedReplicaCount = scaleService
                .updateResourcesDetails(vnfInstance, createScaleVnfRequest(ScaleVnfRequest.TypeEnum.OUT,
                                                                           PAYLOAD, 3));
        assertThat(updatedReplicaCount).isNotNull().isNotEmpty();
    }

    @Test
    public void testGetVnfcScaleInfoList() {
        // given
        final var vnfInstance = databaseInteractionService.getVnfInstance("d8a8da6b-4488-4b11");

        whenOnboardingRespondsWithVnfd("9392468011745350001", "scale-service/test-vnfd.json");

        // when
        final var vnfScaleInfoList = scaleService.getVnfcScaleInfoList(vnfInstance, TypeEnum.IN, 2, "Payload");

        // then
        assertThat(vnfScaleInfoList).isNotEmpty();
    }

    @Test
    public void testGetVnfcScaleInfoListInstanceNotInstantiated() {
        // given
        final var vnfInstance = databaseInteractionService.getVnfInstance("r3def1ce-4cf4-477c-aab3-21c454e6a379");

        // when and then
        assertThatThrownBy(() -> scaleService.getVnfcScaleInfoList(vnfInstance, TypeEnum.IN, 2, "Payload"))
                .isInstanceOf(NotInstantiatedException.class);
    }

    @Test
    public void testGetVnfcScaleInfoListAspectIdNotFound() {
        // given
        final var vnfInstance = databaseInteractionService.getVnfInstance("d8a8da6b-4488-4b11");

        // when and then
        assertThatThrownBy(() -> scaleService.getVnfcScaleInfoList(vnfInstance, TypeEnum.IN, 2, "id-not-found"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scaling Aspect id-not-found not defined for instance d8a8da6b-4488-4b11");
    }

    private ScaleVnfRequest createScaleVnfRequest(ScaleVnfRequest.TypeEnum type, String aspectId, int numberOfSteps) {
        ScaleVnfRequest scaleRequest = new ScaleVnfRequest();
        scaleRequest.setType(type);
        scaleRequest.setAspectId(aspectId);
        scaleRequest.setNumberOfSteps(numberOfSteps);
        return scaleRequest;
    }

    private void whenOnboardingRespondsWithVnfd(final String vnfdId, final String fileName) {
        when(packageService.getVnfd(eq(vnfdId))).thenReturn(new JSONObject(readFile(fileName)));
    }

    private String readFile(final String fileName) {
        return readDataFromFile(getClass(), fileName);
    }
}
