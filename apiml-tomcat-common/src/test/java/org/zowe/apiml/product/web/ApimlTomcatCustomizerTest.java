/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.web;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApimlTomcatCustomizerTest {

    @Test
    void customizeConnector() {
        ApimlTomcatCustomizer customizer = new ApimlTomcatCustomizer<>();
        Http11NioProtocol protocol = new Http11NioProtocol();
        Connector connector = new Connector(protocol);
        customizer.customizeConnector(connector);
        Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
        AbstractEndpoint abstractEndpoint = ReflectionTestUtils.invokeMethod(protocolHandler, "getEndpoint");
        assertEquals(ApimlTomcatCustomizer.ApimlAttlsHandler.class, abstractEndpoint.getHandler().getClass());
    }
}
