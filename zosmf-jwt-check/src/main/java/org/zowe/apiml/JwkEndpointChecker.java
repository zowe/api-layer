/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks z/OSMF JWK endpoint availability at {@code /jwt/ibm/api/zOSMFBuilder/jwk}.
 * Interprets the HTTP response code to determine if the endpoint is functional
 */
// TODO: REMOVE all "DEBUG [JwkEndpointChecker]" logging lines after SAF keyring issue is resolved
@SuppressWarnings("squid:S106")
public class JwkEndpointChecker {

    static final String JWK_ENDPOINT_PATH = "/jwt/ibm/api/zOSMFBuilder/jwk";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    private static final Pattern N_VALUE_PATTERN = Pattern.compile("\"n\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClientWrapper httpClient;
    private final ZosmfJwtCheckConfig conf;

    public JwkEndpointChecker(HttpClientWrapper httpClient, ZosmfJwtCheckConfig conf) {
        this.httpClient = httpClient;
        this.conf = conf;
    }

    public boolean check() {
        String urlString = conf.getScheme() + "://" + conf.getZosmfHost() + ":" + conf.getZosmfPort() + JWK_ENDPOINT_PATH;

        Map<String, String> headers = new HashMap<>();
        headers.put(ZOSMF_CSRF_HEADER, "");

        System.out.println("DEBUG [JwkEndpointChecker] check() called");
        System.out.println("DEBUG [JwkEndpointChecker]   target URL=" + urlString);
        System.out.println("DEBUG [JwkEndpointChecker]   headers=" + headers);

        try {
            URL url = new URL(urlString);
            System.out.println("Checking z/OSMF JWK endpoint: " + urlString);
            System.out.println("DEBUG [JwkEndpointChecker] Calling httpClient.executeCall()...");

            HttpClientWrapper.Response response = httpClient.executeCall(url, headers);

            System.out.println("DEBUG [JwkEndpointChecker] Response received: HTTP " + response.getStatusCode());
            System.out.println("DEBUG [JwkEndpointChecker] Response body length=" + (response.getBody() != null ? response.getBody().length() : "<null>"));
            if (conf.isVerbose() && response.getBody() != null) {
                System.out.println("Response body:\n" + response.getBody());
            }
            return evaluateResponseCode(response.getStatusCode(), response.getBody(), urlString);
        } catch (SSLHandshakeException e) {
            System.err.println("FAILURE: SSL handshake failed when connecting to " + urlString + ".");
            System.err.println("Verify that the truststore contains the z/OSMF server certificate.");
            System.err.println("Details: " + e.getMessage());
            System.err.println("DEBUG [JwkEndpointChecker] SSLHandshakeException stack trace:");
            e.printStackTrace(System.err);
            return false;
        } catch (ConnectException e) {
            System.err.println("FAILURE: Cannot connect to " + conf.getZosmfHost() + ":" + conf.getZosmfPort() + ".");
            System.err.println("Verify the host and port are correct and z/OSMF is running.");
            System.err.println("Details: " + e.getMessage());
            System.err.println("DEBUG [JwkEndpointChecker] ConnectException stack trace:");
            e.printStackTrace(System.err);
            return false;
        } catch (SocketTimeoutException e) {
            System.err.println("FAILURE: Connection timed out to " + conf.getZosmfHost() + ":" + conf.getZosmfPort() + ".");
            System.err.println("This is commonly caused by an incorrect host/port or a firewall blocking the connection.");
            System.err.println("Verify the z/OSMF host and port are correct and that no firewall is blocking access.");
            System.err.println("DEBUG [JwkEndpointChecker] SocketTimeoutException stack trace:");
            e.printStackTrace(System.err);
            return false;
        } catch (UnknownHostException e) {
            System.err.println("FAILURE: Error when calling " + urlString + " verify hostname and port.");
            System.err.println("The host '" + conf.getZosmfHost() + "' could not be resolved.");
            System.err.println("DEBUG [JwkEndpointChecker] UnknownHostException stack trace:");
            e.printStackTrace(System.err);
            return false;
        } catch (Exception e) {
            System.err.println("FAILURE: Error when calling " + urlString + " verify hostname and port.");
            System.err.println("Details: " + e.getMessage());
            System.err.println("DEBUG [JwkEndpointChecker] Exception class=" + e.getClass().getName());
            e.printStackTrace(System.err);
            return false;
        }
    }

    boolean evaluateResponseCode(int responseCode, String body, String urlString) {
        if (responseCode >= 200 && responseCode < 300) {
            if (!validateJwkBody(body)) {
                return false;
            }
            System.out.println("SUCCESS: z/OSMF JWK endpoint is reachable and responding. HTTP " + responseCode);
            return true;
        }

        if (responseCode == 401) {
            System.out.println("SUCCESS: z/OSMF JWK endpoint exists (returned 401 Unauthorized, expected without credentials). HTTP 401");
            return true;
        }

        if (responseCode == 404) {
            System.err.println("FAILURE: z/OSMF JWK endpoint not found. HTTP 404");
            System.err.println("JWT support not found, may not be configured. LTPA may be used as an alternative");
            return false;
        }

        if (responseCode >= 400 && responseCode < 500) {
            System.err.println("FAILURE: z/OSMF JWK endpoint returned unexpected client error. HTTP " + responseCode);
            System.err.println("URL: " + urlString);
            return false;
        }

        if (responseCode >= 500) {
            System.err.println("FAILURE: z/OSMF JWK endpoint returned server error. HTTP " + responseCode);
            System.err.println("URL: " + urlString);
            return false;
        }

        System.err.println("FAILURE: z/OSMF JWK endpoint returned unexpected response code. HTTP " + responseCode);
        System.err.println("URL: " + urlString);
        return false;
    }

    boolean validateJwkBody(String body) {
        if (body == null || body.isEmpty()) {
            System.err.println("WARNING: z/OSMF JWK endpoint returned an empty response body.");
            System.err.println("Response body: " + (body == null ? "<null>" : "<empty>"));
            return false;
        }

        Matcher matcher = N_VALUE_PATTERN.matcher(body);
        if (!matcher.find()) {
            System.err.println("WARNING: JWK response does not contain an RSA modulus (\"n\" key).");
            System.err.println("The z/OSMF JWK endpoint may not be properly configured.");
            System.err.println("Response body: " + body);
            return false;
        }

        String nValue = matcher.group(1);
        if (nValue == null || nValue.trim().isEmpty()) {
            System.err.println("FAILURE: JWK response contains an empty RSA modulus (\"n\" key is empty).");
            System.err.println("The z/OSMF server returned a key that cannot be used for JWT verification.");
            System.err.println("Check z/OSMF JWT configuration and ensure the signing key is properly generated.");
            System.err.println("Response body: " + body);
            return false;
        }

        return true;
    }
}
