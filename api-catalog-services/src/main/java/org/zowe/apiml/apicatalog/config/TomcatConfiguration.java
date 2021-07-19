/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.config;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.product.tomcat.ApimlTomcatCustomizer;
import org.zowe.commons.attls.InboundAttls;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Configuration of Tomcat for the API Gateway.
 */
@Configuration
public class TomcatConfiguration {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.setProtocol(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        tomcat.setTomcatConnectorCustomizers(Collections.singletonList(tomcatAttlsCustomizer()));
        return tomcat;
    }

    public <S, U> TomcatConnectorCustomizer tomcatAttlsCustomizer() {
        InboundAttls.setAlwaysLoadCertificate(true);
        return connector -> {
            Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
            try {

                Field handlerField = AbstractProtocol.class.getDeclaredField("handler");
                handlerField.setAccessible(true);

                AbstractEndpoint.Handler<S> handler = (AbstractEndpoint.Handler<S>) handlerField.get(protocolHandler);
                handler = new ApimlTomcatCustomizer.ApimlAttlsHandler<S>(handler);
                Method method = AbstractProtocol.class.getDeclaredMethod("getEndpoint");
                method.setAccessible(true);
                AbstractEndpoint<S, U> abstractEndpoint = (AbstractEndpoint<S, U>) method.invoke(protocolHandler);

                abstractEndpoint.setHandler(handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
