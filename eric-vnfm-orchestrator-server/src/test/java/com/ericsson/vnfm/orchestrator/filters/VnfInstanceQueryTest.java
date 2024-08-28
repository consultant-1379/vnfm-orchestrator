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
package com.ericsson.vnfm.orchestrator.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import static com.ericsson.vnfm.orchestrator.presentation.constants.OperationsConstants.Scale.ASPECT_ID;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.INSTANTIATION_STATE;
import static com.ericsson.vnfm.orchestrator.presentation.constants.VnfInstanceConstants.VNF_INSTANCE_ID;

import java.util.List;
import jakarta.persistence.criteria.JoinType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;

import com.ericsson.am.shared.filter.model.FilterExpressionMultiValue;
import com.ericsson.am.shared.filter.model.FilterExpressionOneValue;
import com.ericsson.am.shared.filter.model.OperandMultiValue;
import com.ericsson.am.shared.filter.model.OperandOneValue;
import com.ericsson.vnfm.orchestrator.model.entity.InstantiationState;
import com.ericsson.vnfm.orchestrator.model.entity.ScaleInfoEntity;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;


@SpringBootTest(classes = {
    VnfInstanceQuery.class
})
@MockBean(classes = VnfInstanceRepository.class)
public class VnfInstanceQueryTest {

    @Autowired
    private VnfInstanceQuery vnfInstanceQuery;
    @MockBean
    private VnfInstanceRepository vnfInstanceRepository;

    @Test
    public void testCreateFilterExpressionOneValueForJoin() {
        FilterExpressionOneValue resultFilterExpressionOneValue = vnfInstanceQuery.createFilterExpressionOneValue(ASPECT_ID, "Aspect1", "eq" );

        assertThat(resultFilterExpressionOneValue.getKey()).isEqualTo("scaleInfoEntity.aspectId");
        assertThat(resultFilterExpressionOneValue.getValue()).isEqualTo("Aspect1");
        assertThat(resultFilterExpressionOneValue.getOperation()).isEqualTo(OperandOneValue.EQUAL);
        assertThat(resultFilterExpressionOneValue.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionOneValueForVnfInstance() {
        FilterExpressionOneValue resultFilterExpressionOneValue =
            vnfInstanceQuery.createFilterExpressionOneValue(INSTANTIATION_STATE,
                                                            InstantiationState.INSTANTIATED.name(), "eq" );

        assertThat(resultFilterExpressionOneValue.getKey()).isEqualTo("instantiationState");
        assertThat(resultFilterExpressionOneValue.getValue()).isEqualTo(InstantiationState.INSTANTIATED);
        assertThat(resultFilterExpressionOneValue.getOperation()).isEqualTo(OperandOneValue.EQUAL);
    }

    @Test
    public void testQueryScaleLevel(){
        VnfInstance instance = new VnfInstance();
        instance.setVnfInstanceId(VNF_INSTANCE_ID);
        instance.setInstantiationState(InstantiationState.INSTANTIATED);
        instance.setScaleInfoEntity(List.of(ScaleInfoEntity.builder().scaleLevel(3).build()));
        List<VnfInstance> allInstances = List.of(instance);
        when(vnfInstanceRepository.findAll(any(Specification.class))).thenReturn(allInstances);
        final VnfInstance vnfInstance =
                allInstances.stream().filter(element -> element.getScaleInfoEntity() != null && !element.getScaleInfoEntity().isEmpty()).findFirst().get();
        int scaleLevel = vnfInstance.getScaleInfoEntity().get(0).getScaleLevel();
        String filter = "(eq,instantiatedVnfInfo/scaleStatus/scaleLevel,%d)";
        List<VnfInstance> instances = vnfInstanceQuery.getAllWithFilter(String.format(filter, scaleLevel));
        assertThat(instances).extracting("vnfInstanceId").contains(vnfInstance.getVnfInstanceId());
    }

    @Test
    public void testCreateFilterExpressionMultiValueForJoin() {
        FilterExpressionMultiValue resultFilterExpressionMultiValue =
            vnfInstanceQuery.createFilterExpressionMultiValue(ASPECT_ID, List.of("Aspect1", "Aspect2"), "cont" );

        assertThat(resultFilterExpressionMultiValue.getKey()).isEqualTo("scaleInfoEntity.aspectId");
        assertThat(resultFilterExpressionMultiValue.getValues()).isEqualTo(List.of("Aspect1", "Aspect2"));
        assertThat(resultFilterExpressionMultiValue.getOperation()).isEqualTo(OperandMultiValue.CONTAINS);
        assertThat(resultFilterExpressionMultiValue.getJoinType()).isEqualTo(JoinType.LEFT);
    }

    @Test
    public void testCreateFilterExpressionMultiValueForVnfInstance() {
        FilterExpressionMultiValue resultFilterExpressionMultiValue =
            vnfInstanceQuery.createFilterExpressionMultiValue("instantiatedVnfInfo/scaleStatus/scaleLevel", List.of("dummy-name-1", "dummy-name-2"), "in");

        assertThat(resultFilterExpressionMultiValue.getKey()).isEqualTo("scaleInfoEntity.scaleLevel");
        assertThat(resultFilterExpressionMultiValue.getValues()).isEqualTo(List.of("dummy-name-1", "dummy-name-2"));
        assertThat(resultFilterExpressionMultiValue.getOperation()).isEqualTo(OperandMultiValue.IN);
    }
}
