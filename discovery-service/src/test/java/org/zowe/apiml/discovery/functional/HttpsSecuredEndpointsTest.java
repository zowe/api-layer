/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@ActiveProfiles("https")
public class HttpsSecuredEndpointsTest extends DiscoveryFunctionalTest {

    @Override
    protected String getProtocol() {
        return "https";
    }

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;

    @Value("${server.ssl.keyStore}")
    private String keystore;

    private String clientKeystore = "../keystore/client_cert/client-certs.p12";

    @Override
    @BeforeEach
    void setUp() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(
            keystorePassword,
            clientKeystore,
            keystore);
        SslContext.prepareSslAuthentication(configurer);
    }

    @Test
    void testEurekaEndpoints_whenProvidedCertificate() throws Exception {
        given().config(SslContext.clientCertApiml)
            .when()
            .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

}
