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
package com.ericsson.vnfm.orchestrator.presentation.services.lcm.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;


public class LcmOpSearchServiceTest extends AbstractDbSetupTest {

    private static final String VNF_INSTANCE_ID = "a014e979-1e0e-48c0-98a9-caeda9787b7e";
    private static final String LCM_OP_ID = "d7b553fd-f09c-421c-a238-947ce6b98860";

    private static final List<String> EXPECTED_LCM_OP_IDS_BEFORE =
            List.of("d7b553fd-f09c-421c-a238-947ce6b98862", "24e58306-4a80-42eb-a19f-19bc9b8c71a91", "d7b553fd-f09c-421c-a238-947ce6b98853");

    private static final List<String> EXPECTED_LCM_OP_IDS_NOT_FAILED_INSTALL_UPGRADE =
            List.of("24e58306-4a80-42eb-a19f-19bc9b8c71a92", "24e58306-4a80-42eb-a19f-19bc9b8c71a91", "d7b553fd-f09c-421c-a238-947ce6b98853");

    private VnfInstance vnfInstance;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @BeforeEach
    public void setUp() {
        vnfInstance = vnfInstanceRepository.findByVnfInstanceId(VNF_INSTANCE_ID);
    }

    @Test
    public void searchAllNotFailedInstallOrUpgradeOperations() {
        List<LifecycleOperation> actual = lcmOpSearchService.searchAllNotFailedInstallOrUpgradeOperations(vnfInstance);
        List<String> actualIds = getIdsFromLcmOperations(actual);

        assertThat(actual).hasSize(3);
        assertThat(actualIds).isEqualTo(EXPECTED_LCM_OP_IDS_NOT_FAILED_INSTALL_UPGRADE);
    }

    @Test
    public void searchLastCompletedInstallOrUpgradeScaleOperation() {
        Optional<LifecycleOperation> actual = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOrScaleOperation(vnfInstance, 0);

        assertThat(actual)
                .map(LifecycleOperation::getOperationOccurrenceId)
                .isPresent()
                .get()
                .isEqualTo("d7b553fd-f09c-421c-a238-947ce6b98862");
    }

    @Test
    public void searchLastCompletedInstallOrUpgradeOperation() {
        Optional<LifecycleOperation> actual = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperation(vnfInstance, 0);

        assertThat(actual)
                .map(LifecycleOperation::getOperationOccurrenceId)
                .isPresent()
                .get()
                .isEqualTo("24e58306-4a80-42eb-a19f-19bc9b8c71a91");
    }

    @Test
    public void searchLastCompletedInstallOrUpgradeOperationForRollbackTo() {
        Optional<LifecycleOperation> actual = lcmOpSearchService.searchLastCompletedInstallOrUpgradeOperationForRollbackTo(vnfInstance);

        assertThat(actual)
                .map(LifecycleOperation::getOperationOccurrenceId)
                .isPresent()
                .get()
                .isEqualTo("d7b553fd-f09c-421c-a238-947ce6b98853");
    }

    @Test
    public void searchAllBefore() {
        LifecycleOperation operation = lifecycleOperationRepository.findByOperationOccurrenceId(LCM_OP_ID);

        List<LifecycleOperation> actual = lcmOpSearchService.searchAllBefore(operation);
        List<String> actualIds = getIdsFromLcmOperations(actual);

        assertThat(actual).hasSize(3);
        assertThat(actualIds).isEqualTo(EXPECTED_LCM_OP_IDS_BEFORE);
    }

    private List<String> getIdsFromLcmOperations(List<LifecycleOperation> lifecycleOperations) {
        return lifecycleOperations.stream()
                .map(LifecycleOperation::getOperationOccurrenceId)
                .collect(Collectors.toList());
    }
}