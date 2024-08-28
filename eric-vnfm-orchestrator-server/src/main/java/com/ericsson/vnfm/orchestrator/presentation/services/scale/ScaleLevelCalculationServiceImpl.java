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
package com.ericsson.vnfm.orchestrator.presentation.services.scale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ericsson.am.shared.vnfd.model.policies.ScalingAspectDeltas;
import com.ericsson.am.shared.vnfd.model.policies.VduLevelDataType;
import com.ericsson.vnfm.orchestrator.model.ScaleVnfRequest;

@Service
public class ScaleLevelCalculationServiceImpl implements ScaleLevelCalculationService {

    @Override
    public int calculateScaleLevelAfterLinearScaling(int numOfInstances,
                                                             ScalingAspectDeltas scaleDelta,
                                                             final String targetName,
                                                             final int initialDelta) {
        Map<String, VduLevelDataType> deltas = scaleDelta.getProperties().getDeltas();
        int delta = deltas.get(deltas.keySet().toArray()[0]).getNumberOfInstances();
        if (numOfInstances == initialDelta) {
            return 0;
        }
        if ((numOfInstances - initialDelta) % delta != 0) {
            int closestScaleLevel = (numOfInstances - initialDelta) / delta;
            int closestNumberOfInstances = (closestScaleLevel - initialDelta) / delta;
            throw new IllegalArgumentException(String.format("For target %s replica count %s does not belong to any scaling level. Closest "
                                                                     + "scaling "
                                                                     + "level: %s, "
                                                                     + "closets replica count: %s.", targetName, numOfInstances, closestScaleLevel,
                                                             closestNumberOfInstances));
        }
        return (numOfInstances - initialDelta) / delta;
    }

    @Override
    public int calculateScaleLevelAfterNonLinearScaling(int currentScaleLevel,
                                                                int currentNumOfInstances,
                                                                int numOfInstances,
                                                                ScalingAspectDeltas scaleDelta,
                                                                ScaleVnfRequest.TypeEnum scaleOperation,
                                                                final String targetName) {
        Map<String, VduLevelDataType> deltas = scaleDelta.getProperties().getDeltas();
        List<String> allDeltaName = new ArrayList<>(deltas.keySet());
        int tempScaleLevel = 1;
        int nextDelta = currentScaleLevel;
        int numberOfInstanceToAdd = currentNumOfInstances;
        if (ScaleVnfRequest.TypeEnum.OUT.equals(scaleOperation)) {
            while (numberOfInstanceToAdd < numOfInstances) {
                nextDelta = currentScaleLevel + tempScaleLevel;

                if (nextDelta > deltas.size()) {
                    numberOfInstanceToAdd += deltas.get(allDeltaName.get(deltas.size() - 1)).getNumberOfInstances();
                } else {
                    numberOfInstanceToAdd += deltas.get(allDeltaName.get(nextDelta - 1)).getNumberOfInstances();
                }
                tempScaleLevel++;
            }
        }
        if (ScaleVnfRequest.TypeEnum.IN.equals(scaleOperation)) {
            while (numberOfInstanceToAdd > numOfInstances) {

                if (nextDelta > deltas.size()) {
                    numberOfInstanceToAdd -= deltas.get(allDeltaName.get(deltas.size() - 1)).getNumberOfInstances();
                } else {
                    numberOfInstanceToAdd -= deltas.get(allDeltaName.get(nextDelta - 1)).getNumberOfInstances();
                }

                nextDelta = currentScaleLevel - tempScaleLevel;

                tempScaleLevel++;
            }
        }
        if (numberOfInstanceToAdd != numOfInstances) {
            throw new IllegalArgumentException(String.format("For target %s replica count %s does not belong to any scaling level. Closest "
                                                                     + "scaling "
                                                                     + "level: %s, "
                                                                     + "closets replica count: %s.", targetName, numOfInstances, nextDelta,
                                                             numberOfInstanceToAdd));
        }
        return nextDelta;
    }
}
