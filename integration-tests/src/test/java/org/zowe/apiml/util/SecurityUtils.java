/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util;


import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.TlsConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    public final static String GATEWAY_TOKEN_COOKIE_NAME = "apimlAuthenticationToken";
    public final static String GATEWAY_LOGIN_ENDPOINT = "/auth/login";
    public final static String GATEWAY_LOGOUT_ENDPOINT = "/auth/logout";
    public final static String GATEWAY_BASE_PATH = "/gateway/api/v1";
    public final static String GATEWAY_BASE_PATH_OLD_FORMAT = "/api/v1/gateway";

    private final static GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();;

    private final static String gatewayScheme = serviceConfiguration.getScheme();
    private final static String gatewayHost = serviceConfiguration.getHost();
    private final static int gatewayPort = serviceConfiguration.getPort();

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

    //@formatter:off

    public static String getGatewayUrl(String path) {
        return getGatewayUrl(path, gatewayPort);
    }

    public static String getGatewayUrl(String path, int port) {
        return String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, port, GATEWAY_BASE_PATH, path);
    }

    public static String getGatewayUrlOldFormat(String path) {
        return String.format("%s://%s:%d%s%s", gatewayScheme, gatewayHost, gatewayPort, GATEWAY_BASE_PATH_OLD_FORMAT, path);
    }

    public static String getGatewayLogoutUrl() {
        return getGatewayUrl(GATEWAY_LOGOUT_ENDPOINT);
    }

    public static String getGatewayLogoutUrlOldPath() {
        return getGatewayUrlOldFormat(GATEWAY_LOGOUT_ENDPOINT);
    }

    public static String gatewayToken() {
        return gatewayToken(USERNAME, PASSWORD);
    }

    public static String gatewayToken(String username, String password) {
        return gatewayToken(HttpRequestUtils.getUriFromGateway(GATEWAY_BASE_PATH + GATEWAY_LOGIN_ENDPOINT), username, password);
    }

    public static String gatewayToken(URI gatewayLoginEndpoint, String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);

        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        String cookie = given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(gatewayLoginEndpoint)
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(GATEWAY_TOKEN_COOKIE_NAME);

        RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        return cookie;
    }

    public static void logoutOnGateway(String url, String jwtToken) {
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwtToken)
            .when()
            .post(url)
            .then()
            .statusCode(is(SC_NO_CONTENT));
    }

    public static SSLConfig getConfiguredSslConfig() {
        TlsConfiguration tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();
        try {
            SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(
                    new File(tlsConfiguration.getKeyStore()),
                    tlsConfiguration.getKeyStorePassword(),
                    tlsConfiguration.getKeyPassword(),
                    (aliases, socket) -> tlsConfiguration.getKeyAlias())
                .loadTrustMaterial(
                    new File(tlsConfiguration.getTrustStore()),
                    tlsConfiguration.getTrustStorePassword())
                .build();
            HostnameVerifier hostnameVerifier = tlsConfiguration.isNonStrictVerifySslCertificatesOfServices() ? new NoopHostnameVerifier() : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
            SSLSocketFactoryAdapter sslSocketFactory = new SSLSocketFactoryAdapter(new SSLConnectionSocketFactory(sslContext, hostnameVerifier));
            return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
            | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    //@formatter:on
}
