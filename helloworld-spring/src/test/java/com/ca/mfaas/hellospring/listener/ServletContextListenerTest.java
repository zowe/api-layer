/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.hellospring.listener;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


//@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class ServletContextListenerTest {
    @Autowired
    private WebApplicationContext webAppContext;

   /* @BeforeClass
    void setupContext() {
        ctx = mock(ServletContext.class);
        when(ctx.getResourceAsStream(anyString())).thenAnswer(new Answer<InputStream>() {
            String path = ServletContextListenerTest .class.getProtectionDomain().getCodeSource().getLocation().getPath()
                + "../../src/main/webapp";

            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String relativePath = (String) args[0];
                InputStream is = new FileInputStream(path + relativePath);
                return is;
            }
        });
    }*/

    @Test
    public void contextStartsAndStopsTest() {
        //ServletContext context = webAppContext.getServletContext();

        // The parameter can't be null in reality.
        //  TODO: Rewrite the test with some context variations
        ServletContext context = new MockServletContext();
        context.setInitParameter("apiml.config.location", "/service-config.yml");
        context.setInitParameter("apiml.config.additional-location", "../config/local/apiml-plugin-poc_plain-java-enabler.xml");
        context.setInitParameter("apiml.serviceIpAddress", "127.0.0.2");
        context.setInitParameter("apiml.discoveryService.port", "10011");
        context.setInitParameter("apiml.discoveryService.hostname", "localhost");
        context.setInitParameter("apiml.ssl.keyPassword", "password");

        ApiDiscoveryListener contextListener = new ApiDiscoveryListener();
        contextListener.contextInitialized(new ServletContextEvent(context));
    }
}
