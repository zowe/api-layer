/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.InvalidParameterException;
import java.util.Map;

import static org.junit.Assert.*;

public class ApiInfoTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void generateFullMetadata() {
        String serviceId = "test service";
        String gatewayUrl = "api/v1";
        String version = "1.0.0";
        String swaggerUrl = "https://service/api-doc";
        String documentationUrl = "https://www.zowe.org";
        String metadataPrefix = "apiml.apiDocs.api-v1.";

        ApiDoc apiDoc = new ApiDoc("org.zowe", gatewayUrl, version, swaggerUrl, documentationUrl);
        Map<String, String> metadata = apiDoc.generateMetadata(serviceId);

        String metaVersion = metadata.get(metadataPrefix + "version");
        assertNotNull(metaVersion);
        assertEquals(version, metaVersion);

        String metaGatewayUrl = metadata.get(metadataPrefix + "gatewayUrl");
        assertNotNull(metaGatewayUrl);
        assertEquals(gatewayUrl, metaGatewayUrl);

        String metaSwaggerUrl = metadata.get(metadataPrefix + "swaggerUrl");
        assertNotNull(metaSwaggerUrl);
        assertEquals(swaggerUrl, metaSwaggerUrl);

        String metaDocumentationUrl = metadata.get(metadataPrefix + "documentationUrl");
        assertNotNull(metaDocumentationUrl);
        assertEquals(documentationUrl, metaDocumentationUrl);
    }

    @Test
    public void generateMetadataWithNoGatewayUrl() {
        String serviceId = "test service";
        String version = "1.0.0";

        ApiDoc apiDoc = new ApiDoc(null, null, version, null, null);
        Map<String, String> metadata = apiDoc.generateMetadata(serviceId);

        assertEquals(1, metadata.size());
        assertTrue(metadata.toString().contains(version));
    }

    @Test
    public void generateNoMetadata() {
        String serviceId = "test service";

        ApiDoc apiDoc = new ApiDoc();
        Map<String, String> metadata = apiDoc.generateMetadata(serviceId);

        assertEquals(0, metadata.size());
    }

    @Test
    public void generateMetadataWithIncorrectSwaggerUrl() {
        String serviceId = "test service";
        String gatewayUrl = "api/v1";
        String swaggerUrl = "www.badAddress";

        exceptionRule.expect(InvalidParameterException.class);
        exceptionRule.expectMessage("The Swagger URL \"" + swaggerUrl + "\" for service " + serviceId + " is not valid: no protocol: " + swaggerUrl);

        ApiDoc apiDoc = new ApiDoc(null, gatewayUrl, null, swaggerUrl, null);
        apiDoc.generateMetadata(serviceId);
    }


    @Test
    public void generateMetadataWithIncorrectDocumentationUrl() {
        String serviceId = "test service";
        String gatewayUrl = "api/v1";
        String documentationUrl = "www.badAddress";

        exceptionRule.expect(InvalidParameterException.class);
        exceptionRule.expectMessage("The documentation URL \"" + documentationUrl + "\" for service " + serviceId + " is not valid: no protocol: " + documentationUrl);

        ApiDoc apiDoc = new ApiDoc(null, gatewayUrl, null, null, documentationUrl);
        apiDoc.generateMetadata(serviceId);
    }
}
