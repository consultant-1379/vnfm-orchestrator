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
package com.ericsson.vnfm.orchestrator.infrastructure.db.migration.crypto;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ericsson.vnfm.orchestrator.infrastructure.db.migration.common.FieldsTransformingTask;
import com.ericsson.am.shared.vnfd.service.CryptoService;

@Profile({ "disabled" })
@Component
public class EncryptingTransformer implements Consumer<FieldsTransformingTask> {

    private final CryptoService cryptoService;

    @Autowired
    public EncryptingTransformer(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public void accept(final FieldsTransformingTask task) {
        for (int i = 0; i < task.getSelectedValues().length; i++) {
            if (task.getSelectedValues()[i] == null) {
                task.getUpdateValues()[i] = null;
            } else {
                task.getUpdateValues()[i] = cryptoService.encryptString(task.getSelectedValues()[i]);
            }
        }
    }
}
