/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void removeFirstAndLastOccurrenceTest() {
        assertNull(StringUtils.removeFirstAndLastOccurrence(null, "any-string"));

        String whiteSpace = "       ";
        assertTrue(StringUtils.removeFirstAndLastOccurrence(whiteSpace, "any-string").isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.removeFirstAndLastOccurrence(hasSlashes, "/"), "blah");
    }

    @Test
    public void removeLastOccurrenceTest() {
        assertNull(StringUtils.removeLastOccurrence(null, "any-string"));

        String whiteSpace = "       ";
        assertTrue(StringUtils.removeLastOccurrence(whiteSpace, "any-string").isEmpty());

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.removeLastOccurrence(hasSlashes, "/"), "/blah");
    }

    @Test
    public void prependSubstringTest() {
        assertNull(StringUtils.prependSubstring(null, "any-string"));

        assertNull(StringUtils.prependSubstring("", null));

        String whiteSpace = "       ";
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string"), "any-string");
        assertEquals(StringUtils.prependSubstring("any-string", "any-string"), "any-string");
        assertEquals(StringUtils.prependSubstring("any-string", "any-string", false), "any-stringany-string");
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string", true, false), "any-string" + whiteSpace);
        assertEquals(StringUtils.prependSubstring(whiteSpace, "any-string", false, false), "any-string" + whiteSpace);

        String hasSlashes = "  /blah/   ";
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", true, true), "/blah/");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", false, true), "//blah/");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", false, false), "/  /blah/   ");
        assertEquals(StringUtils.prependSubstring(hasSlashes, "/", true, false), "/  /blah/   ");
    }

    @Test
    public void testResolveExpressions() {

        String expression = "serviceId: ${apiml.serviceId}\n" +
            "title: Hello Spring REST API\n" +
            "description: Example for exposing a Spring REST API\n" +
            "baseUrl: http://localhost:8080/helloworld\n" +
            "serviceIpAddress: ${apiml.serviceIpAddress} #127.0.0.1\n" +
            "\n" +
            "homePageRelativeUrl: /\n" +
            "statusPageRelativeUrl: /application/info\n" +
            "healthCheckRelativeUrl: /application/health\n" +
            "\n" +
            "discoveryServiceUrls:\n" +
            "    - https://${apiml.discoveryService.hostname}:${apiml.discoveryService.port}/eureka\n" +
            "\n" +
            "ssl:\n" +
            "    enabled: ${apiml.ssl.enabled} #true\n" +
            "    verifySslCertificatesOfServices: ${apiml.ssl.verifySslCertificatesOfServices} #true\n" +
            "    protocol: TLSv1.2\n" +
            "    keyAlias: localhost\n" +
            "    keyPassword: ${apiml.ssl.keyPassword} #password\n" +
            "    keyStore: ../keystore/localhost/localhost.keystore.p12\n" +
            "    keyStorePassword: ${apiml.ssl.keystore.password}\n" +
            "    keyStoreType: PKCS12\n" +
            "    trustStore: ${apiml.ssl.truststore} # keystore/localhost/localhost.truststore.p12\n" +
            "    trustStorePassword: ${apiml.ssl.truststore.password}\n" +
            "    trustStoreType: PKCS12\n" +
            "\n" +
            "routes:\n" +
            "    - gatewayUrl: api\n" +
            "      serviceUrl: /greeting\n" +
            "    - gatewayUrl: api/v1\n" +
            "      serviceUrl: /greeting/api/v1\n" +
            "    - gatewayUrl: api/v1/api-doc\n" +
            "      serviceUrl: /greeting/api-doc\n" +
            "\n" +
            "apiInfo:\n" +
            "    - apiId: org.zowe.greeting\n" +
            "      gatewayUrl: api/v1\n" +
            "      swaggerUrl: http://localhost:8080/helloworld/api-doc\n" +
            "\n" +
            "catalog:\n" +
            "  tile:\n" +
            "    id: cademoapps\n" +
            "    title: Sample API Mediation Layer Applications\n" +
            "    description: Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem\n" +
            "    version: 1.0.0\n";

        Map<String, String> properties = new HashMap();
        properties.put("apiml.serviceId", "serviced");
        properties.put("apiml.serviceIpAddress", "192.168.0.");
        properties.put("apiml.ssl.keyPassword", "passworth");
        properties.put("apiml.ssl.truststore", "keystore/localhost/localhost.truststore.p123");

        String resolved = StringUtils.resolveExpressions(expression, null);
        assertNotEquals(resolved.indexOf("${apiml.serviceId}"), -1);
        assertNotEquals(resolved.indexOf("${apiml.serviceIpAddress}"), -1);
        assertNotEquals(resolved.indexOf("${apiml.ssl.keyPassword}"), -1);
        assertNotEquals(resolved.indexOf("${apiml.ssl.truststore}"), -1);
        assertNotEquals(resolved.indexOf("${apiml.ssl.truststore.password}"), -1);

        resolved = StringUtils.resolveExpressions(expression, properties);

        assertNotEquals(resolved.indexOf("serviced"), -1);
        assertNotEquals(resolved.indexOf("192.168.0."), -1);
        assertNotEquals(resolved.indexOf("passworth"), -1);
        assertNotEquals(resolved.indexOf("keystore/localhost/localhost.truststore.p123"), -1);
        assertNotEquals(resolved.indexOf("${apiml.ssl.truststore.password}"), -1);
        assertEquals(resolved.indexOf("${apiml.ssl.truststore.password2}"), -1);
    }
}
