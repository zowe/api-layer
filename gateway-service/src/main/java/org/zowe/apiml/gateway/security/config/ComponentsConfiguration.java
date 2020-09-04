/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.gateway.security.login.x509.X509Authentication;
import org.zowe.apiml.gateway.security.service.zosmf.X509AuthenticationService;
import org.zowe.apiml.gateway.security.service.zosmf.ZSSX509Authentication;
import org.zowe.apiml.passticket.PassTicketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Registers security related beans
 */
@Configuration
public class ComponentsConfiguration {
    @Value("${apiml.security.x509.provider:zss}")
    private String x509Provider;

    /**
     * Used for dummy authentication provider
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Service to call generating and validating of passTickets. If JVM contains mainframe's class, it uses it,
     * otherwise method returns dummy implementation
     *
     * @return mainframe / dummy implementation of passTicket's generation and validation
     */
    @Bean
    public PassTicketService passTicketService() {
        return new PassTicketService();
    }

    @Bean
    public X509Authentication x509Authentication() {
        if (x509Provider.equals("attls")) {
            return new X509AuthenticationService();
        } else {
            return new ZSSX509Authentication();
        }

    }
}
