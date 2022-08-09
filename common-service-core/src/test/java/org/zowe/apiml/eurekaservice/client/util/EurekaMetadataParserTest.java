/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.config.CodeSnippet;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.auth.AuthenticationScheme.ZOSMF;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

class EurekaMetadataParserTest {

    private final EurekaMetadataParser eurekaMetadataParser = new EurekaMetadataParser();

    @Nested
    class WhenParseApiInfo {
        @Test
        void givenTwoEntries_thenReturnTwoInstances() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_ENDPOINT, "endpoint");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_LANGUAGE, "java");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_CODE_BLOCK, "codeblock");
            metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, "gatewayUrl");
            metadata.put(API_INFO + ".2." + API_INFO_GATEWAY_URL, "gatewayUrl2");
            metadata.put(API_INFO + ".2." + API_INFO_SWAGGER_URL, "swagger");
            metadata.put(API_INFO + ".2." + API_INFO_DOCUMENTATION_URL, "doc");
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, "zowe.apiml.test");
            metadata.put(API_INFO + ".1." + API_INFO_VERSION, "1.0.0");
            metadata.put(API_INFO + ".1." + API_INFO_IS_DEFAULT, "true");

            List<ApiInfo> info = eurekaMetadataParser.parseApiInfo(metadata);

            assertEquals(2, info.size());
            assertEquals("gatewayUrl", info.get(0).getGatewayUrl());
            assertEquals("zowe.apiml.test", info.get(0).getApiId());
            assertEquals("1.0.0", info.get(0).getVersion());
            assertTrue(info.get(0).isDefaultApi());
            assertEquals("gatewayUrl2", info.get(1).getGatewayUrl());
            assertEquals("swagger", info.get(1).getSwaggerUrl());
            assertEquals("doc", info.get(1).getDocumentationUrl());
            assertFalse(info.get(1).isDefaultApi());
        }

        @Test
        void givenCodeSnippets_thenReturnApiInfoWithCodeSnippets() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_ENDPOINT, "endpoint1");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_CODE_BLOCK, "codeblock1");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".1." + CODE_SNIPPET_LANGUAGE, "language1");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".2." + CODE_SNIPPET_ENDPOINT, "endpoint2");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".2." + CODE_SNIPPET_CODE_BLOCK, "codeblock2");
            metadata.put(API_INFO + ".1." + CODE_SNIPPET + ".2." + CODE_SNIPPET_LANGUAGE, "language2");
            metadata.put(API_INFO + ".2." + CODE_SNIPPET, "badvalue");

            List<ApiInfo> info = eurekaMetadataParser.parseApiInfo(metadata);

            CodeSnippet expectedCodeSnippet1 = new CodeSnippet("endpoint1", "codeblock1", "language1");
            CodeSnippet expectedCodeSnippet2 = new CodeSnippet("endpoint2", "codeblock2", "language2");

            assertEquals(1, info.size());
            assertEquals(expectedCodeSnippet1, info.get(0).getCodeSnippet().get(0));
            assertEquals(expectedCodeSnippet2, info.get(0).getCodeSnippet().get(1));
        }

        @Test
        void givenBadField_thenDontReturnInstance() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, "zowe.apiml.test");
            metadata.put(API_INFO + ".2." + "badargument", "value");

            List<ApiInfo> info = eurekaMetadataParser.parseApiInfo(metadata);
            assertEquals(1, info.size());
        }
    }

    @Test
    void testParseRoutes() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put("other.parameter", "value");
        metadata.put(ROUTES + ".api-v2." + ROUTES_SERVICE_URL, "/test");
        metadata.put("some.garbage.again", "null");
        metadata.put(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL, "api/v2");
        metadata.put(ROUTES + ".api-v3." + ROUTES_GATEWAY_URL, "incomplete");
        metadata.put(ROUTES + ".api-v4." + ROUTES_SERVICE_URL, "incomplete");
        metadata.put(ROUTES + ".api-v5." + ROUTES_GATEWAY_URL, "/api/v5/");
        metadata.put(ROUTES + ".api-v5." + ROUTES_SERVICE_URL, "test");

        RoutedServices routes = eurekaMetadataParser.parseRoutes(metadata);

        RoutedServices expectedRoutes = new RoutedServices();
        expectedRoutes.addRoutedService(
            new RoutedService("api-v1", "api/v1", "/"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v2", "api/v2", "/test"));
        expectedRoutes.addRoutedService(
            new RoutedService("api-v5", "api/v5", "/test"));

        assertEquals(expectedRoutes.toString(), routes.toString());
    }

    @Test
    void testParseToListRoute() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api/v1");
        metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put("other.parameter", "value");
        metadata.put(ROUTES + ".api-v2." + ROUTES_SERVICE_URL, "/test");
        metadata.put("some.garbage.again", "null");
        metadata.put(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL, "api/v2");
        metadata.put(ROUTES + ".api-v3." + ROUTES_GATEWAY_URL, "incomplete");
        metadata.put(ROUTES + ".api-v4." + ROUTES_SERVICE_URL, "incomplete");
        metadata.put(ROUTES + ".api-v5." + ROUTES_GATEWAY_URL, "/api/v5/");
        metadata.put(ROUTES + ".api-v5." + ROUTES_SERVICE_URL, "test");

        List<RoutedService> actualRoutes = eurekaMetadataParser.parseToListRoute(metadata);
        List<RoutedService> expectedListRoute = Arrays.asList(
            new RoutedService("api-v1", "api/v1", "/"),
            new RoutedService("api-v2", "api/v2", "/test"),
            new RoutedService("api-v5", "api/v5", "/test")
        );

        assertEquals(3, actualRoutes.size(), "List route size is different");
        assertThat(actualRoutes, containsInAnyOrder(expectedListRoute.toArray()));
    }

    @Test
    void testParseToListRoute_whenMetadatakeyElementsIsDifferentFrom4() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ROUTES + ".api-v1.test." + ROUTES_GATEWAY_URL, "api/v1");

        List<RoutedService> actualRoutes = eurekaMetadataParser.parseToListRoute(metadata);
        assertEquals(0, actualRoutes.size(), "List route is not empty");
    }

    @Test
    void generateFullMetadata() {
        String serviceId = "test service";
        String apiId = "zowe.apiml.test";
        String gatewayUrl = "api/v1";
        String version = "1.0.0";
        String swaggerUrl = "https://service/api-doc";
        String documentationUrl = "https://www.zowe.org";
        String metadataPrefix = API_INFO + ".api-v1.";

        ApiInfo apiInfo = new ApiInfo(apiId, gatewayUrl, version, swaggerUrl, documentationUrl);
        Map<String, String> metadata = EurekaMetadataParser.generateMetadata(serviceId, apiInfo);

        String metaApiId = metadata.get(metadataPrefix + API_INFO_API_ID);
        assertEquals(apiId, metaApiId);

        String metaVersion = metadata.get(metadataPrefix + API_INFO_VERSION);
        assertEquals(version, metaVersion);

        String metaGatewayUrl = metadata.get(metadataPrefix + API_INFO_GATEWAY_URL);
        assertEquals(gatewayUrl, metaGatewayUrl);

        String metaSwaggerUrl = metadata.get(metadataPrefix + API_INFO_SWAGGER_URL);
        assertEquals(swaggerUrl, metaSwaggerUrl);

        String metaDocumentationUrl = metadata.get(metadataPrefix + API_INFO_DOCUMENTATION_URL);
        assertEquals(documentationUrl, metaDocumentationUrl);
    }

    @Test
    void generateMetadataWithCodeSnippets() {
        String endpoint1 = "/endpoint1";
        String codeBlock1 = "code1";
        String language1 = "java1";
        String endpoint2 = "/endpoint2";
        String codeBlock2 = "code2";
        String language2 = "java2";
        String metadataPrefix = API_INFO + ".api-v1.";

        ApiInfo apiInfo = new ApiInfo("zowe.apiml.test", "api/v1", "1.0.0", "https://service/api-doc", "https://www.zowe.org");
        apiInfo.addCodeSnippet(new CodeSnippet(endpoint1, codeBlock1, language1));
        apiInfo.addCodeSnippet(new CodeSnippet(endpoint2, codeBlock2, language2));
        Map<String, String> metadata = EurekaMetadataParser.generateMetadata("test service", apiInfo);

        String codeSnippetEndpoint1 = metadata.get(metadataPrefix + CODE_SNIPPET + ".0." + CODE_SNIPPET_ENDPOINT);
        String codeSnippetBlock1 = metadata.get(metadataPrefix + CODE_SNIPPET + ".0." + CODE_SNIPPET_CODE_BLOCK);
        String codeSnippetLanguage1 = metadata.get(metadataPrefix + CODE_SNIPPET + ".0." + CODE_SNIPPET_LANGUAGE);

        assertEquals(codeSnippetEndpoint1, endpoint1);
        assertEquals(codeSnippetBlock1, codeBlock1);
        assertEquals(codeSnippetLanguage1, language1);

        String codeSnippetEndpoint2 = metadata.get(metadataPrefix + CODE_SNIPPET + ".1." + CODE_SNIPPET_ENDPOINT);
        String codeSnippetBlock2 = metadata.get(metadataPrefix + CODE_SNIPPET + ".1." + CODE_SNIPPET_CODE_BLOCK);
        String codeSnippetLanguage2 = metadata.get(metadataPrefix + CODE_SNIPPET + ".1." + CODE_SNIPPET_LANGUAGE);

        assertEquals(codeSnippetEndpoint2, endpoint2);
        assertEquals(codeSnippetBlock2, codeBlock2);
        assertEquals(codeSnippetLanguage2, language2);
    }

    @Test
    void generateMetadataWithNoGatewayUrl() {
        String serviceId = "test service";
        String version = "1.0.0";

        ApiInfo apiInfo = new ApiInfo(null, null, version, null, null); // isDefaultApi defaults to false
        Map<String, String> metadata = EurekaMetadataParser.generateMetadata(serviceId, apiInfo);

        assertEquals(2, metadata.size());
        assertTrue(metadata.toString().contains(version));
    }

    @Test
    void generateNoMetadata() {
        String serviceId = "test service";

        ApiInfo apiInfo = new ApiInfo(); // isDefaultApi defaults to false
        Map<String, String> metadata = EurekaMetadataParser.generateMetadata(serviceId, apiInfo);
        assertEquals(1, metadata.size());
    }

    @Test
    void generateMetadataWithIncorrectSwaggerUrl() {
        String serviceId = "test service";
        String gatewayUrl = "api/v1";
        String swaggerUrl = "www.badAddress";

        ApiInfo apiInfo = new ApiInfo(null, gatewayUrl, null, swaggerUrl, null);
        Exception exception = assertThrows(MetadataValidationException.class, () -> {
            EurekaMetadataParser.generateMetadata(serviceId, apiInfo);
        });
        assertEquals("The Swagger URL \"" + swaggerUrl + "\" for service " + serviceId + " is not valid", exception.getMessage());
    }

    @Test
    void generateMetadataWithIncorrectDocumentationUrl() {
        String serviceId = "test service";
        String gatewayUrl = "api/v1";
        String documentationUrl = "www.badAddress";

        ApiInfo apiInfo = new ApiInfo(null, gatewayUrl, null, null, documentationUrl);
        Exception exception = assertThrows(MetadataValidationException.class, () -> {
            EurekaMetadataParser.generateMetadata(serviceId, apiInfo);
        });
        assertEquals("The documentation URL \"" + documentationUrl + "\" for service " + serviceId + " is not valid", exception.getMessage());
    }

    @Test
    void whenFullInfo_testAuthenticationParser() {
        String applid = "applid";

        Map<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SCHEME, ZOSMF.getScheme());
        metadata.put(AUTHENTICATION_APPLID, applid);
        metadata.put(AUTHENTICATION_SSO, Boolean.TRUE.toString());

        Authentication authentication = eurekaMetadataParser.parseAuthentication(metadata);

        assertEquals(ZOSMF, authentication.getScheme());
        assertEquals(applid, authentication.getApplid());
        assertTrue(authentication.supportsSso());
    }

    @Test
    void whenNoInfo_testAuthenticationParser() {
        Authentication authentication = eurekaMetadataParser.parseAuthentication(Collections.emptyMap());

        assertNull(authentication.getScheme());
        assertNull(authentication.getApplid());
        assertFalse(authentication.supportsSso());
    }

}
