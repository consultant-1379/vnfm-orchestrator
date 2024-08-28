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
package com.ericsson.vnfm.orchestrator.presentation.services.crypto;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.ericsson.am.shared.vnfd.service.CryptoService;


@Profile({ "test", "dev", "debug" })
@Service
public class CryptoServiceImplDev implements CryptoService {

    @Override
    public String encryptString(String data) {
        return data;
    }

    @Override
    public String decryptString(String data) {
        return data;
    }
}
