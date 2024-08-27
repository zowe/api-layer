/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UrlTomcatCustomizerTest {

    private final Connector connector = new Connector();

    @Test
    void givenConnector_whenCustomize_thenCustomized() {
        UrlTomcatCustomizer urlTomcatCustomizer = new UrlTomcatCustomizer();
        urlTomcatCustomizer.customize(connector);
        assertTrue(connector.getAllowBackslash());
        assertEquals(EncodedSolidusHandling.PASS_THROUGH.getValue(), connector.getEncodedSolidusHandling());
    }
}
