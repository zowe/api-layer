/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.Cookie;
import io.restassured.specification.RequestSpecification;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.cert.X509Certificate;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@MainframeDependentTests
class ZosmfAuthenticationLoginIntegrationTest extends Login {
    private String scheme;
    private String host;
    private int port;
    private final static String ZOSMF_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";

    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        providers.switchProvider("zosmf");
    }

    @BeforeEach
    void setUpZosmf() {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
    }

    /**
     * This is how z/OSMF behaves. Two logins with basic auth give identical token.
     */
    @Test
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenIdenticalTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is((jwtToken2)));
    }

    @Test
    void givenValidCertificate_whenRequestToZosmfHappensAfterAuthentication_thenTheRequestSucceeds() throws Exception {
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD)
            // trust server blind folded. Need to test only that the client pass appropriate x509 certificate.
            .loadTrustMaterial(null, trustStrategy)
            .build();

        RequestSpecification clientCertificateRequestConfig = given()
            .config(RestAssuredConfig
                .newConfig()
                .sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext))));

        Cookie cookie = clientCertificateRequestConfig
            .post(new URI(LOGIN_ENDPOINT_URL))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie);

        String dsname1 = "SYS1.PARMLIB";
        String dsname2 = "SYS1.PROCLIB";

        clientCertificateRequestConfig
            .cookie(cookie)
        .get(String.format("%s://%s:%d%s%s", scheme, host, port, BASE_PATH, ZOSMF_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body(
                "items.dsname", hasItems(dsname1, dsname2));
    }
}
