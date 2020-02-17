/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hellospring.listener;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockServletContext;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.exception.ServiceDefinitionException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Further tests related to the Eureka Configuration and the Configuration of the service are in
 * the onboarding-enabler-java
 *
 * These unit tests verify that the helloworld properly tries to register itself.
 */
public class ServletContextListenerTest {
    @Test
    public void testOnContextCreationRegisterWithEureka() throws ServiceDefinitionException {
        ApiMediationClient mock = Mockito.mock(ApiMediationClient.class);
        ServletContext context = setValidParametersTo(
            new MockServletContext()
        );

        ApiDiscoveryListener registrator = new ApiDiscoveryListener(mock);
        registrator.contextInitialized(new ServletContextEvent(context));

        // Verify that the mock
        verify(mock).register(any());
    }

    @Test
    public void testOnContextDestroyUnregisterWithEureka() {
        ApiMediationClient mock = Mockito.mock(ApiMediationClient.class);
        ServletContext context = setValidParametersTo(
            new MockServletContext()
        );

        ApiDiscoveryListener registrator = new ApiDiscoveryListener(mock);
        registrator.contextDestroyed(null);

        // Verify that the mock
        verify(mock).unregister();
    }

    private ServletContext setValidParametersTo(ServletContext context) {
        context.setInitParameter("apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.additional-location", "../config/local/helloworld-additional-config.yml");
        context.setInitParameter("apiml.serviceIpAddress", "127.0.0.2");
        context.setInitParameter("apiml.discoveryService.port", "10011");
        context.setInitParameter("apiml.discoveryService.hostname", "localhost");
        context.setInitParameter("apiml.ssl.verifySslCertificatesOfServices", "true");
        context.setInitParameter("apiml.ssl.keyPassword", "password123");
        context.setInitParameter("apiml.ssl.keyStorePassword", "password");
        context.setInitParameter("apiml.ssl.trustStore", "../keystore/localhost/localhost.keystore.p12");
        context.setInitParameter("apiml.ssl.trustStorePassword", "password");

        return context;
    }
}
