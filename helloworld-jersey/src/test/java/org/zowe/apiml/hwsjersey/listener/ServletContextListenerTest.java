/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hwsjersey.resource.listener;

import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.hwsjersey.listener.ApiDiscoveryListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class ServletContextListenerTest {

    @Test
    public void testContextOk() {
        ServletContext context = new MockServletContext();
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

        ApiDiscoveryListener contextListener = new ApiDiscoveryListener();
        contextListener.contextInitialized(new ServletContextEvent(context));

        ApiMediationClient apimlClient = contextListener.getApiMediationClient();
        assertNotNull(apimlClient);
        assertNotNull(apimlClient.getEurekaClient());
        assertNotNull(apimlClient.getEurekaClient().getApplicationInfoManager());
        assertNotNull(apimlClient.getEurekaClient().getApplicationInfoManager().getInfo());
        assertNotNull(apimlClient.getEurekaClient().getApplicationInfoManager().getInfo().getMetadata());
    }

    @Test
    public void testContextMissingSubstituteParameters() {
        ServletContext context = new MockServletContext();
        context.setInitParameter("apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.additional-location", "../config/local/helloworld-additional-config.yml");
        context.setInitParameter("apiml.serviceIpAddress", "127.0.0.2");
        context.setInitParameter("apiml.discoveryService.port", "10011");
        context.setInitParameter("apiml.discoveryService.hostname", "localhost");
        context.setInitParameter("apiml.ssl.keyPassword", "password");
        context.setInitParameter("apiml.ssl.keyStorePassword", "password");
        context.setInitParameter("apiml.ssl.trustStore", "password");
        context.setInitParameter("apiml.ssl.trustStorePassword", "password");
        context.setInitParameter("apiml.ssl.verifySslCertificatesOfServices", "true");

        ApiDiscoveryListener contextListener = new ApiDiscoveryListener();
        contextListener.contextInitialized(new ServletContextEvent(context));

        assertNotNull(contextListener.getApiMediationClient());
        assertNotNull(contextListener.getApiMediationClient().getEurekaClient());
    }

    @Test
    public void testContextDestroyed() {
        ServletContext context = new MockServletContext();
        context.setInitParameter("apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.additional-location", "../config/local/helloworld-additional-config.yml");
        context.setInitParameter("apiml.serviceIpAddress", "127.0.0.2");
        context.setInitParameter("apiml.discoveryService.port", "10011");
        context.setInitParameter("apiml.discoveryService.hostname", "localhost");
        context.setInitParameter("apiml.ssl.keyPassword", "password");
        context.setInitParameter("apiml.ssl.keyStorePassword", "password");
        context.setInitParameter("apiml.ssl.trustStore", "safkeyring://PLATDEV/DBMRING");//
        context.setInitParameter("apiml.ssl.trustStorePassword", "password");
        context.setInitParameter("apiml.ssl.verifySslCertificatesOfServices", "true");

        ApiDiscoveryListener contextListener = new ApiDiscoveryListener();
        contextListener.contextInitialized(new ServletContextEvent(context));
        assertNotNull(contextListener.getApiMediationClient());

        contextListener.contextDestroyed(null);
        assertNull(contextListener.getApiMediationClient());

        contextListener.contextDestroyed(null);
    }
}
