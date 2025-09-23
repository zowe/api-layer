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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import com.nimbusds.jose.util.Base64;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookie;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.gateway.security.login.SuccessfulAccessTokenHandler;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.TlsConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.zowe.apiml.util.requests.Endpoints.GENERATE_ACCESS_TOKEN;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_LOGIN;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_QUERY;
import static org.zowe.apiml.util.requests.Endpoints.ZOSMF_AUTH_ENDPOINT;

public class SecurityUtils {
    public static final String GATEWAY_TOKEN_COOKIE_NAME = "apimlAuthenticationToken";

    private static final GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private static final TlsConfiguration tlsConfiguration = ConfigReader.environmentConfiguration().getTlsConfiguration();

    private static final String GATEWAY_SCHEME = serviceConfiguration.getScheme();
    private static final String GATEWAY_HOST = StringUtils.isBlank(serviceConfiguration.getDvipaHost()) ? serviceConfiguration.getHost() : serviceConfiguration.getDvipaHost();
    private static final int GATEWAY_PORT = serviceConfiguration.getPort();

    private static final String ZOSMF_SCHEME = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getScheme();
    private static final String ZOSMF_HOST = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getHost();
    private static final int ZOSMF_PORT = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getPort();

    public static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    public static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

    public static final String OIDC_HOSTNAME = ConfigReader.environmentConfiguration().getOidcConfiguration().getHost();
    public static final String OIDC_CLIENT_ID = ConfigReader.environmentConfiguration().getOidcConfiguration().getClientId();
    public static final String OIDC_CLIENT_PASSWORD = ConfigReader.environmentConfiguration().getOidcConfiguration().getClientSecret();
    public static final String OIDC_USER = ConfigReader.environmentConfiguration().getOidcConfiguration().getUser();
    public static final String OIDC_PASSWORD = ConfigReader.environmentConfiguration().getOidcConfiguration().getPassword();
    public static final String OIDC_ALT_USER = ConfigReader.environmentConfiguration().getOidcConfiguration().getAlternateUser();
    public static final String OIDC_ALT_PASSWORD = ConfigReader.environmentConfiguration().getOidcConfiguration().getAlternatePassword();
    public static final String OIDC_PROVIDER_NAME = ConfigReader.environmentConfiguration().getOidcConfiguration().getProviderName();

    public static final String OKTA_AUTHENTICATE_SESSION_URL = "/api/v1/authn";
    public static final String OKTA_GENERATE_TOKEN_URL = "/oauth2/v1/authorize";
    public static final String KEYCLOAK_GENERATE_TOKEN_URL = "/realms/apiml/protocol/openid-connect/token";

    public static final String COOKIE_NAME = "apimlAuthenticationToken";
    public static final String PAT_COOKIE_AUTH_NAME = "personalAccessToken";

    public static final ObjectMapper MAPPER = new ObjectMapper();

    protected static String getUsername() {
        return USERNAME;
    }

    //@formatter:off

    public static String getGatewayUrl(String path) {
        return getGatewayUrl(path, GATEWAY_PORT);
    }

    public static String getGatewayUrl(String path, int port) {
        return String.format("%s://%s:%d%s", GATEWAY_SCHEME, GATEWAY_HOST, port, path);
    }

    public static String getGatewayLogoutUrl(String path) {
        return getGatewayUrl(path);
    }

    public static String gatewayToken() {
        return gatewayToken(USERNAME, PASSWORD);
    }

    public static String gatewayToken(URI gatewayLoginEndpoint) {
        return gatewayToken(gatewayLoginEndpoint, USERNAME, PASSWORD);
    }

    public static String gatewayToken(String username, String password) {
        return gatewayToken(HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN), username, password);
    }

    public static String gatewayToken(URI gatewayLoginEndpoint, String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password.toCharArray());

        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        try {
            return given()
                .contentType(JSON)
                .body(loginRequest)
                .when()
                .post(gatewayLoginEndpoint)
                .then()
                .log().ifValidationFails(LogDetail.ALL)
                .statusCode(is(SC_NO_CONTENT))
                .cookie(GATEWAY_TOKEN_COOKIE_NAME, not(isEmptyString()))
                .extract().cookie(GATEWAY_TOKEN_COOKIE_NAME);
        } finally {
            RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        }
    }

    /**
     *
     * @return
     */
    public static String getZosmfJwtTokenFromGw() {
        return getZosmfTokenFromGw("apimlAuthenticationToken");
    }

    /**
     *
     * @return
     */
    public static String getZosmfLtpaTokenFromGw() {
        return getZosmfTokenFromGw("LtpaToken2");
    }

    /**
     * Get z/OSMF jwtToken cookie value from a direct call to z/OSMF
     *
     * @return
     */
    public static String getZosmfJwtToken() {
        return getZosmfTokenWebClient("jwtToken");
    }

    /**
     * Get z/OSMF LtpaToken2 cookie value from a direct call to z/OSMF
     *
     * @return
     */
    public static String getZosmfLtpaToken() {
        return getZosmfTokenWebClient("LtpaToken2");
    }

    public static String getZosmfTokenFromGw(String cookie) {
        URI gwUrl = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN);
        return getZosmfToken(gwUrl.toString(), cookie, SC_NO_CONTENT);
    }

    /**
     * Direct call to z/OSMF using Apache HTTP client
     *
     * @param cookie Which cookie name to retrieve
     * @return
     */
    public static String getZosmfTokenWebClient(String cookie) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(getRelaxedSslContext()).build()) { // Can't think of a reason to test SSL connection between Integration test runner and z/OSMF
            HttpClientContext context = HttpClientContext.create();
            CookieStore cookieStore = new BasicCookieStore();
            context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

            HttpUriRequest request = new HttpPost(String.format("%s://%s:%d%s", ZOSMF_SCHEME, ZOSMF_HOST, ZOSMF_PORT, ZOSMF_AUTH_ENDPOINT));
            request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Basic %s", java.util.Base64.getEncoder().encodeToString(String.format("%s:%s", USERNAME, PASSWORD).getBytes())));
            request.addHeader("X-CSRF-ZOSMF-HEADER", "csrf");

            CloseableHttpResponse response = httpClient.execute(request, context);
            if (response.getStatusLine().getStatusCode() == 200) {
                return cookieStore.getCookies()
                    .stream()
                    .filter(c -> cookie.equals(c.getName()))
                    .findFirst()
                    .map(c -> c.getValue())
                    .orElseThrow(() -> new RuntimeException("Cookie " + cookie + " not found in z/OSMF response"));
            } else {
                throw new RuntimeException("Request to z/OSMF failed with status code " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Direct call to z/OSMF using RestAssured
     *
     * @param cookie Which cookie name to retrieve
     * @return
     */
    public static String getZosmfToken(String cookie) {
        return getZosmfToken(String.format("%s://%s:%d%s", ZOSMF_SCHEME, ZOSMF_HOST, ZOSMF_PORT, ZOSMF_AUTH_ENDPOINT), cookie, SC_OK);
    }

    private static String getZosmfToken(String url, String cookie, int expectedCode) {
        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        try {
            return given()
                .contentType(JSON)
                .auth().preemptive().basic(USERNAME, PASSWORD)
                .header("X-CSRF-ZOSMF-HEADER", "")
                .when()
                .post(url)
                .then()
                .statusCode(is(expectedCode))
                .cookie(cookie, not(isEmptyString()))
                .extract().cookie(cookie);
        } finally {
            RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        }
    }

    public static String generateZoweJwtWithLtpa(String ltpaToken) throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        long now = System.currentTimeMillis();
        long expiration = now + 100_000L;

        return Jwts.builder()
            .setSubject(USERNAME)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer(QueryResponse.Source.ZOWE.value)
            .setId(UUID.randomUUID().toString())
            .claim("ltpa", ltpaToken)
            .signWith(getKey(), SignatureAlgorithm.RS256)
            .compact();
    }

    public static String generateJwtWithRandomSignature(String issuer) {
        long now = System.currentTimeMillis();
        long expiration = now + 100_000L;

        return Jwts.builder()
            .setSubject(USERNAME)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer(issuer)
            .setId(UUID.randomUUID().toString())
            .setHeaderParam("kid", "apiKey")
            .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256))
            .compact();
    }

    private static KeyStore loadKeystore(String keystore) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        File keyStoreFile = new File(keystore);
        InputStream inputStream = new FileInputStream(keyStoreFile);

        KeyStore ks = KeyStore.getInstance(SecurityUtils.tlsConfiguration.getKeyStoreType());
        ks.load(inputStream, SecurityUtils.tlsConfiguration.getKeyStorePassword());

        return ks;
    }

    private static Key getKey() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore ks = loadKeystore(SecurityUtils.tlsConfiguration.getKeyStore());

        return ks.getKey(SecurityUtils.tlsConfiguration.getKeyAlias(), SecurityUtils.tlsConfiguration.getKeyStorePassword());
    }

    public static String getClientCertificate() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore ks = loadKeystore(SecurityUtils.tlsConfiguration.getClientKeystore());
        String clientCN = SecurityUtils.tlsConfiguration.getClientCN();
        Enumeration<String> aliases = ks.aliases();

        while (aliases.hasMoreElements()) {
            X509Certificate certificate = (X509Certificate) ks.getCertificate(aliases.nextElement());
            if (certificate.getSubjectX500Principal().getName().contains("CN=" + clientCN)) {
                return Base64.encode(certificate.getEncoded()).toString();
            }
        }

        throw new IllegalArgumentException(
            String.format("TlsConfiguration error: provided client CN: %s is not present in client keystore %s", clientCN, tlsConfiguration.getClientKeystore()));
    }

    public static String getDummyClientCertificate()throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();

        X500Name dnName = new X500Name("CN=USER");
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));
        Date startDate = new Date(now);
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1);
        Date endDate = calendar.getTime();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            dnName,
            certSerialNumber,
            startDate,
            endDate,
            dnName,
            keyPair.getPublic());

        String signatureAlgorithm = "SHA256WithRSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
        X509Certificate certificate  = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);

        return Base64.encode(certificate.getEncoded()).toString();
    }

    public static String personalAccessToken(Set<String> scopes) {
        URI gatewayGenerateAccessTokenEndpoint = HttpRequestUtils.getUriFromGateway(GENERATE_ACCESS_TOKEN);
        SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = new SuccessfulAccessTokenHandler.AccessTokenRequest(60, scopes);

        SSLConfig originalConfig = RestAssured.config().getSSLConfig();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        try {
            return given()
                .contentType(JSON).header("Authorization", "Basic " + Base64.encode(USERNAME + ":" + PASSWORD))
                .body(accessTokenRequest)
                .when()
                .post(gatewayGenerateAccessTokenEndpoint)
                .then()
                .log().ifValidationFails(LogDetail.ALL)
                .statusCode(is(SC_OK))
                .extract().body().asString();
        } finally {
            RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        }
    }

    public static String personalAccessTokenWithClientCert(RestAssuredConfig sslConfig) {
        URI gatewayGenerateAccessTokenEndpoint = HttpRequestUtils.getUriFromGateway(GENERATE_ACCESS_TOKEN);
        Set<String> scopes = new HashSet<>();
        scopes.add("service");

        SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = new SuccessfulAccessTokenHandler.AccessTokenRequest(60, scopes);
        SSLConfig originalConfig = RestAssured.config().getSSLConfig();

        try {
            return given().config(sslConfig)
                .body(accessTokenRequest)
                .when()
                .post(gatewayGenerateAccessTokenEndpoint)
                .then()
                .statusCode(is(SC_OK))
                .extract().body().asString();
        } finally {
            RestAssured.config = RestAssured.config().sslConfig(originalConfig);
        }
    }

    public static String validOidcAccessToken(boolean userHasMappingDefined) {
        if (isKeycloakProvider()) {
            return validKeycloakToken(userHasMappingDefined);
        } else if (isOktaProvider()) {
            return validOktaAccessToken(userHasMappingDefined);
        }
        throw new IllegalArgumentException(String.format("Unsupported OIDC provider: %s", OIDC_PROVIDER_NAME));
    }

    public static String validOktaAccessToken(boolean userHasMappingDefined) {

        assertNotNull(OIDC_HOSTNAME, "OKTA host name is not set.");
        assertNotNull(OIDC_CLIENT_ID, "OKTA client id is not set.");

        String sessionToken;
        if (userHasMappingDefined) {
            sessionToken = getOktaSession(OIDC_USER, OIDC_PASSWORD);
        } else {
            sessionToken = getOktaSession(OIDC_ALT_USER, OIDC_ALT_PASSWORD);
        }
        assertNotNull(sessionToken, "Failed to get session token from Okta authentication.");

        // retrieve the access token from Okta using session token

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(getRelaxedSslContext()).build()) {
            URIBuilder uriBuilder = new URIBuilder(OIDC_HOSTNAME + OKTA_GENERATE_TOKEN_URL);
            uriBuilder.setParameter("client_id", OIDC_CLIENT_ID);
            uriBuilder.setParameter("redirect_uri", "https://localhost:10010/login/oauth2/code/okta");
            uriBuilder.setParameter("response_type", "token");
            uriBuilder.setParameter("response_mode", "form_post");
            uriBuilder.setParameter("sessionToken", sessionToken);
            uriBuilder.setParameter("scope", "openid");
            uriBuilder.setParameter("state", "TEST");
            uriBuilder.setParameter("nonce", "TEST");
            HttpGet request = new HttpGet(uriBuilder.build());
            CloseableHttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                // The response is HTML form where access token is hidden input field (this is controlled by response_mode = form_post)

                String body = EntityUtils.toString(response.getEntity());
                String accessToken = StringUtils.substringBetween(body, "name=\"access_token\" value=\"", "\"/>");
                assertNotNull(accessToken, "Failed to locate access token in the Okta /authorize response.");
                return accessToken;
            } else {
                throw new RuntimeException("Failed obtaining OKTA access token: " + response.getStatusLine().getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isKeycloakProvider() {
        return "keycloak".equalsIgnoreCase(OIDC_PROVIDER_NAME);
    }

    public static boolean isOktaProvider() {
        return "okta".equalsIgnoreCase(OIDC_PROVIDER_NAME);
    }

    public static String validKeycloakToken(boolean userHasMappingDefined) {
        assertNotNull(OIDC_HOSTNAME, "Keycloak host name is not set.");
        assertNotNull(OIDC_CLIENT_ID, "Keycloak client id is not set.");
        // retrieve the access token from Okta using session token
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(getRelaxedSslContext()).build()) {
            URI uri = new URI(OIDC_HOSTNAME + KEYCLOAK_GENERATE_TOKEN_URL);
            HttpPost request = new HttpPost(uri);
            StringBuilder form = new StringBuilder();
            form.append("grant_type=password");
            form.append("&scope=discoverableclient");
            form.append("&client_id=").append(URLEncoder.encode(OIDC_CLIENT_ID, StandardCharsets.UTF_8.name()));
            form.append("&client_secret=").append(URLEncoder.encode(OIDC_CLIENT_PASSWORD, StandardCharsets.UTF_8.name()));
            if (userHasMappingDefined) {
                form.append("&username=").append(URLEncoder.encode(OIDC_USER, StandardCharsets.UTF_8.name()));
                form.append("&password=").append(URLEncoder.encode(OIDC_PASSWORD, StandardCharsets.UTF_8.name()));
            } else {
                form.append("&username=").append(URLEncoder.encode(OIDC_ALT_USER, StandardCharsets.UTF_8.name()));
                form.append("&password=").append(URLEncoder.encode(OIDC_ALT_PASSWORD, StandardCharsets.UTF_8.name()));
            }
            StringEntity entity = new StringEntity(
                form.toString(),
                ContentType.APPLICATION_FORM_URLENCODED
            );
            request.setEntity(entity);
            request.addHeader("content-type", "application/x-www-form-urlencoded");

            CloseableHttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                // The response is HTML form where access token is hidden input field (this is controlled by response_mode = form_post)
                String body = EntityUtils.toString(response.getEntity());
                Object accessToken = MAPPER.readValue(body,HashMap.class).get("access_token");
                assertNotNull(accessToken, "Failed to locate access token in the Keycloak /authorize response.");
                return (String) accessToken;
            } else {
                throw new RuntimeException("Failed obtaining Keycloak access token: " + response.getStatusLine().getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getOktaSession(String username, String password) {
        assertNotNull(username, "OKTA username is not set.");
        assertNotNull(password, "OKTA password is not set.");
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("username", username);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLContext(getRelaxedSslContext()).build()) {
            URIBuilder uriBuilder = new URIBuilder(OIDC_HOSTNAME + OKTA_AUTHENTICATE_SESSION_URL);

            HttpPost request = new HttpPost(uriBuilder.build());
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            StringEntity entity = new StringEntity(requestBody.toString());
            request.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                // The response is HTML form where access token is hidden input field (this is controlled by response_mode = form_post)

                String responseBody = EntityUtils.toString(response.getEntity());
                return new JSONObject(responseBody).getString("sessionToken");
            } else {
                throw new RuntimeException("Failed obtaining OKTA access token: " + response.getStatusLine().getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException | URISyntaxException | ParseException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String expiredOidcAccessToken() {
        if (isOktaProvider()) {
            return expiredOktaAccessToken();
        } else if (isKeycloakProvider()) {
            return expiredKeycloakAccessToken();
        }
        throw new IllegalArgumentException(String.format("Unsupported OIDC provider: %s", OIDC_PROVIDER_NAME));
    }

    public static String expiredOktaAccessToken() {
        return "eyJraWQiOiJGTUM5UndncFVJMUt0V25QWkdmVmFKYzZUZGlTTElZU29jeWs4aHlEbE44IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULkVzZ051RGxkcm5FN0VDYlhnNUhEdUY4MW9BV3k1UDF4WUZLT1psTmVJcmMiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2Njc5MTc5MjcsImV4cCI6MTY2NzkyMTUyNywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU3NmVvZjB6bnNNYkY3NDVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjY3OTE3ODg2LCJzdWIiOiJpdF90ZXN0QGFjbWUuY29tIiwiZ3JvdXBzIjpbIkV2ZXJ5b25lIl19.KiPa0c1U5IClozwZI5aDRSwjoi-hYtIkQZWpizGF8PPsgzvfMaivUzMoPi5GfEUZF6Bjlg_fQFUK7kJQ8NWjL6gY_5QQMfONw0U9dzQy2HLHb5gU55IKt6mBIutBSPk2FmCTd4SaPmllMb6nAyhIZf0DI7xuAXqRgt5JnasnmCKSIM3HJMlTeXDzHQ5BvMr7tVHWmwQ-8W3nef5nsKi2Sw05rds9RgkcckGUzhA2tMeF_rVTitufeG7h2oXYICtv60wfK6YSnmE78aoHf5NQD5517gnGrRxGMM6UAn3SV4GKOll6OlGDzpz87mq-AR2tigkDfVcOtJA9mkxFFv7HSg";
    }

    public static String expiredKeycloakAccessToken() {
        return "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJJZzc0dlkyMEZ5bXNxSmN5ZDc1MjVGbGloWElPdk13cFZMVVc5TkN2VVFrIn0.eyJleHAiOjE3NTgxOTgyMzUsImlhdCI6MTc1ODE5NzkzNSwianRpIjoib25ydHJvOjBiYjQzNTE4LTQ0MTQtODg1Zi01YThlLWQ1YmExYWMzOGI2NCIsImlzcyI6Imh0dHA6Ly8xMC4yNTIuMTIxLjE5MDo4MDgwL3JlYWxtcy9hcGltbCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJmYzA4NWE1NC01N2E5LTRhMTEtOGQzMy1hMGU5Njc1YmMwZTkiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjbGllbnRpZGFwaW1sIiwic2lkIjoiOGYzYzlkZmItYzEwNS00NzIyLWE5NTYtODhmZTc1ZjcwMTcwIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1hcGltbCIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZGlzY292ZXJhYmxlY2xpZW50IGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiQVBJTUwgTWluaXBsZXggSW50ZWdyYXRpb24gVGVzdHMgQXV0aG9yaXplZCBVc2VyIiwiZ3JvdXBzIjp7Im1lbWViZXJPZiI6WyIvYXBpbWwiLCIva2V5Y2xvYWtncm91cCIsIi9yZWFkZXJzIiwiL3dhdGNoVG93ZXJVc2VyIl19LCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ6b3dlc29uQHpvd2UuY29tIiwiZ2l2ZW5fbmFtZSI6IkFQSU1MIE1pbmlwbGV4IEludGVncmF0aW9uIFRlc3RzIiwiZmFtaWx5X25hbWUiOiJBdXRob3JpemVkIFVzZXIiLCJ1c2VybmFtZSI6eyJmdWxsTmFtZSI6Inpvd2Vzb25Aem93ZS5jb20ifX0.Yut1S6tIoBdX-h782Zde4g2xvkMu28HduOf4DwwpCUzWSJ-KxPbk5JZl7x_8ga81EuR8myv9o4bowOUA3nLZwOd9iC4_gcj_ZnS3dHIAlPA1PC7cUmppDRl_3GGYdEfAk7a_uLbyc9S1RyrD4FuvJASv4HPoB8pcrF7MJAFuQnWSVOhvDzKq-aRhuAHDXNBBb8SzwfKOuQ5cCeq1AjDrGPUt3XGWySeGxOFzQHPRdqPYTbgmJ7nOqzKJdJ-5hd6tvQRoGmGsq8nU1RCcrayc-27flnR1M3JJEVB3CcAp1DmofijHHO-CSgwbycUSz5zdx1c_tY_iUseQeSWsvshPxA";
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
        SSLContext sslContext = getSslContext();
        HostnameVerifier hostnameVerifier = tlsConfiguration.isNonStrictVerifySslCertificatesOfServices() ? new NoopHostnameVerifier() : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        SSLSocketFactoryAdapter sslSocketFactory = new SSLSocketFactoryAdapter(new SSLConnectionSocketFactory(sslContext, hostnameVerifier));
        return SSLConfig.sslConfig().with().sslSocketFactory(sslSocketFactory);
    }

    static SSLContext getRelaxedSslContext() {
        final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        try {
            return SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLContext getSslContext() {
        try {
            return SSLContexts.custom()
            .loadKeyMaterial(
                new File(tlsConfiguration.getKeyStore()),
                tlsConfiguration.getKeyStorePassword(),
                tlsConfiguration.getKeyPassword(),
                (aliases, socket) -> tlsConfiguration.getKeyAlias())
            .loadTrustMaterial(
                new File(tlsConfiguration.getTrustStore()),
                tlsConfiguration.getTrustStorePassword())
            .build();
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .when()
            .get(HttpRequestUtils.getUriFromGateway(ROUTED_QUERY))
            .then()
            .statusCode(status.value());
    }

    public static void assertLogout(String url, String jwtToken, int expectedStatusCode) {
        given()
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(url)
            .then()
            .statusCode(is(expectedStatusCode));
    }

    public static void assertValidAuthToken(Cookie cookie) {
        assertValidAuthToken(cookie, Optional.empty());
    }

    public static void assertValidAuthToken(Cookie cookie, Optional<String> username) {
        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1L));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims, username);
    }

    public static void assertThatTokenIsValid(Claims claims) {
        assertThatTokenIsValid(claims, Optional.empty());
    }

    public static void assertThatTokenIsValid(Claims claims, Optional<String> username) {
        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(username.orElseGet(SecurityUtils::getUsername)));
    }

    public static Claims parseJwtString(String untrustedJwtString) {
        return Jwts.parserBuilder().build()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();
    }

    //@formatter:on
}
