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

/**
 * Checks z/OSMF JWK endpoint availability at {@code /jwt/ibm/api/zOSMFBuilder/jwk}.
 * Interprets the HTTP response code to determine if the endpoint is functional
 */
@SuppressWarnings("squid:S106")
public class JwkEndpointChecker {

    static final String JWK_ENDPOINT_PATH = "/jwt/ibm/api/zOSMFBuilder/jwk";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";

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

        try {
            URL url = new URL(urlString);
            System.out.println("Checking z/OSMF JWK endpoint: " + urlString);

            HttpClientWrapper.Response response = httpClient.executeCall(url, headers);
            if (conf.isVerbose() && response.getBody() != null) {
                System.out.println("Response body:\n" + response.getBody());
            }
            return evaluateResponseCode(response.getStatusCode(), urlString);
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
            System.err.println("This is commonly caused by an incorrect host/port or a firewall blocking the connection.");
            System.err.println("Verify the z/OSMF host and port are correct and that no firewall is blocking access.");
            return false;
        } catch (UnknownHostException e) {
            System.err.println("FAILURE: Error when calling " + urlString + " verify hostname and port.");
            System.err.println("The host '" + conf.getZosmfHost() + "' could not be resolved.");
            return false;
        } catch (Exception e) {
            System.err.println("FAILURE: Error when calling " + urlString + " verify hostname and port.");
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
}
