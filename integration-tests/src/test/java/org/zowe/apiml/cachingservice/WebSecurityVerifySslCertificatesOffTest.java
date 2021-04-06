/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cachingservice;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CachingServiceTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.service.*;

import java.util.*;

import static io.restassured.RestAssured.given;

@NotForMainframeTest
@Disabled // Move to different set of tests.
@CachingServiceTest
public class WebSecurityVerifySslCertificatesOffTest implements TestWithStartedInstances {

    private static Map<String, String> parameters = new HashMap<>();
    private static final RunningService cachingServiceInstance;
    private static final String ServiceId = "cachingnosslverification";
    private static final String CERT_HEADER_NAME = "X-Certificate-DistinguishedName";

    private String caching_url;
    static {
        parameters.put("-Dapiml.service.serviceId", ServiceId);
        parameters.put("-Dapiml.service.port", "10029");
        parameters.put("-Dapiml.security.ssl.verifySslCertificatesOfServices", "false");
        cachingServiceInstance = new RunningService(ServiceId,
            ServiceJars.CACHING, parameters, new HashMap<>());
    }

    private static final String CACHING_PATH = String.format("/%s/api/v1/cache", ServiceId);

    @BeforeAll
    static void setup() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication();
        cachingServiceInstance.start();
        cachingServiceInstance.waitUntilReady();
    }

    @BeforeEach
    void setupCachingUrl() {
        List<DiscoveryUtils.InstanceInfo> cachingInstances = DiscoveryUtils.getInstances(ServiceId);
        caching_url = cachingInstances.stream().findFirst().map(i -> String.format("%s", i.getUrl()))
            .orElseThrow(() -> new RuntimeException("Cannot determine Caching service from Discovery"));
    }

    @AfterAll
    static void tearDown() {
        cachingServiceInstance.stop();
    }

    @Nested
    class calledWithoutCertificate {

        @BeforeEach
        void setUp() {
            clearSsl();
        }

        @Test
        void cachingApiIsAccessible() {
            given()
                .header(CERT_HEADER_NAME, "value")
                .when().get(caching_url + CACHING_PATH)
                .then().statusCode(HttpStatus.OK.value());
        }
    }

    private void clearSsl() {
        RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig());
        RestAssured.useRelaxedHTTPSValidation();
    }
}
