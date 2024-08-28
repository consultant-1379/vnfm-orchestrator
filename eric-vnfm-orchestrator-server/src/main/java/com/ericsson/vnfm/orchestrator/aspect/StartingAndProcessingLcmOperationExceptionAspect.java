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
package com.ericsson.vnfm.orchestrator.aspect;

import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.PROCESSING;
import static com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState.STARTING;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperationState;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.processors.LcmOpErrorManagementService;
import com.ericsson.vnfm.orchestrator.presentation.services.lcm.service.LcmOpSearchService;
import com.ericsson.vnfm.orchestrator.repositories.DatabaseInteractionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class StartingAndProcessingLcmOperationExceptionAspect {

    private static final String VNF_INSTANCE_ID_PARAM_NAME = "vnfInstanceId";
    private static final List<LifecycleOperationState> ONGOING_LCM_OPERATION_STATES = List.of(STARTING, PROCESSING);

    @Autowired
    private LcmOpErrorManagementService lcmOpErrorManagementService;

    @Autowired
    private LcmOpSearchService lcmOpSearchService;

    @Autowired
    private DatabaseInteractionService databaseInteractionService;

    @AfterThrowing(value = "lcmOperationPointcut()", throwing = "e")
    public void afterThrowingExceptionDuringLcmOperation(JoinPoint joinPoint, Exception e) {
        LOGGER.warn("Exception occurred during LCM operation, started error handler process");
        Optional<LifecycleOperation> lifecycleOperationArgument = getLifecycleOperationFromJoinPoint(joinPoint);
        if (lifecycleOperationArgument.isPresent()) {
            final LifecycleOperation lifecycleOperation = lifecycleOperationArgument.get();
            LOGGER.info("LCM operation is present and retrieved from arguments: {}", lifecycleOperation.getOperationOccurrenceId());
            if (!ONGOING_LCM_OPERATION_STATES.contains(lifecycleOperation.getOperationState())) {
                LOGGER.info("LCM operation is not in STARTING or PROCESSING. Operation state is: {}. Skipped updates for LCM operation",
                            lifecycleOperation.getOperationState());
            } else {
                LOGGER.info("Started updating LCM operation with appropriate status and error details");
                lcmOpErrorManagementService.process(lifecycleOperation, e);
            }
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exception occurred during new LCM call, but LifecycleOperation in not yet created");
        }
    }

    @Pointcut("@annotation(com.ericsson.vnfm.orchestrator.infrastructure.annotations.StartingAndProcessingLcmOperationExceptionHandler) || "
            + "@within(com.ericsson.vnfm.orchestrator.infrastructure.annotations.StartingAndProcessingLcmOperationExceptionHandler)")
    private void lcmOperationPointcut() {
        // pointcut for LCM operation exception handling
    }

    private Optional<LifecycleOperation> getLifecycleOperationFromJoinPoint(JoinPoint joinPoint) {
        return getLifecycleOperationFromArgument(joinPoint)
                .or(() -> getLifecycleOperationFromVnfInstanceIdArgument(joinPoint))
                .or(() -> getLifecycleOperationFromVnfInstanceArgument(joinPoint));
    }

    private Optional<LifecycleOperation> getLifecycleOperationFromArgument(JoinPoint joinPoint) {
        return getArgumentByType(joinPoint.getArgs(), LifecycleOperation.class)
                .map(LifecycleOperation::getOperationOccurrenceId)
                .map(operationOccurrenceId -> databaseInteractionService.getLifecycleOperation(operationOccurrenceId));
    }

    private Optional<LifecycleOperation> getLifecycleOperationFromVnfInstanceIdArgument(JoinPoint joinPoint) {
        final MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        final int vnfInstanceIdPosition = List.of(methodSignature.getParameterNames()).indexOf(VNF_INSTANCE_ID_PARAM_NAME);
        if (vnfInstanceIdPosition < 0) {
            return Optional.empty();
        }
        final String vnfInstanceId = (String) joinPoint.getArgs()[vnfInstanceIdPosition];
        final VnfInstance vnfInstance = databaseInteractionService.getVnfInstance(vnfInstanceId);
        return lcmOpSearchService.searchLastOperation(vnfInstance);
    }

    private Optional<LifecycleOperation> getLifecycleOperationFromVnfInstanceArgument(JoinPoint joinPoint) {
        return getArgumentByType(joinPoint.getArgs(), VnfInstance.class)
                .flatMap(vnfInstance -> lcmOpSearchService.searchLastOperation(vnfInstance));
    }

    private <T> Optional<T> getArgumentByType(final Object[] args, Class<T> type) {
        return Arrays.stream(args)
                .filter(arg -> !Objects.isNull(arg) && type.equals(arg.getClass()))
                .map(type::cast)
                .findFirst();
    }
}
