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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks z/OSMF JWK endpoint availability at {@code /jwt/ibm/api/zOSMFBuilder/jwk}.
 * Interprets the HTTP response code to determine if the endpoint is functional
 */
@SuppressWarnings("squid:S106")
public class JwkEndpointChecker {

    static final String JWK_ENDPOINT_PATH = "/jwt/ibm/api/zOSMFBuilder/jwk";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";

    private final HttpClientWrapper httpClient;
    private final PreFlightCheckConfig conf;

    public JwkEndpointChecker(HttpClientWrapper httpClient, PreFlightCheckConfig conf) {
        this.httpClient = httpClient;
        this.conf = conf;
    }

    public boolean check() {
        String urlString = conf.getScheme() + "://" + conf.getZosmfHost() + ":" + conf.getZosmfPort() + JWK_ENDPOINT_PATH;

        Map<String, String> headers = new HashMap<>();
        headers.put(ZOSMF_CSRF_HEADER, "");

        try {
            URL url = new URL(urlString);
            System.out.println("Checking z/OSMF JWK endpoint: " + urlString);

            int responseCode = httpClient.executeCall(url, headers);
            return evaluateResponseCode(responseCode, urlString);
        } catch (SSLHandshakeException e) {
            System.err.println("FAILURE: SSL handshake failed when connecting to " + urlString + ".");
            System.err.println("Verify that the truststore contains the z/OSMF server certificate.");
            System.err.println("Details: " + e.getMessage());
            return false;
        } catch (ConnectException e) {
            System.err.println("FAILURE: Cannot connect to " + conf.getZosmfHost() + ":" + conf.getZosmfPort() + ".");
            System.err.println("Verify the host and port are correct and z/OSMF is running.");
            System.err.println("Details: " + e.getMessage());
            return false;
        } catch (SocketTimeoutException e) {
            System.err.println("FAILURE: Connection timed out to " + conf.getZosmfHost() + ":" + conf.getZosmfPort() + ".");
            System.err.println("Details: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("FAILURE: Unexpected error when calling " + urlString + ".");
            System.err.println("Details: " + e.getMessage());
            return false;
        }
    }

    private boolean evaluateResponseCode(int responseCode, String urlString) {
        if (responseCode >= 200 && responseCode < 300) {
            System.out.println("SUCCESS: z/OSMF JWK endpoint is reachable and responding. HTTP " + responseCode);
            return true;
        }

        if (responseCode == 401) {
            System.out.println("SUCCESS: z/OSMF JWK endpoint exists (returned 401 Unauthorized — expected without credentials). HTTP 401");
            return true;
        }

        if (responseCode == 404) {
            System.err.println("FAILURE: z/OSMF JWK endpoint not found. HTTP 404");
            System.err.println("Try configuring the jwtAutoConfiguration to LTPA");
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
}
