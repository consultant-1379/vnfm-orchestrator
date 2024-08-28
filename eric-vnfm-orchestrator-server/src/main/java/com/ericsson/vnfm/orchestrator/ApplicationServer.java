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
package com.ericsson.vnfm.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.ericsson.am.shared.vnfd.service.CryptoServiceImpl;


@SpringBootApplication
@Import(CryptoServiceImpl.class)
public class ApplicationServer {

    public static void main(final String[] args) {
        SpringApplication.run(ApplicationServer.class, args);
    }
}
