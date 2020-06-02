/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login.zosmf;

import java.io.FileNotFoundException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;

/**
 * Gets z/OSMF public key for JWT validation.
 * It is used by Zowe certificate setup script.
 *
 * This can be started using this command:
 * java -cp gateway-service/build/libs/gateway-service.jar
 * -Dapiml.security.ssl.verifySslCertificatesOfServices=false
 * -Dloader.main=org.zowe.apiml.gateway.security.login.zosmf.SaveZosmfPublicKeyConsoleApplication
 * org.springframework.boot.loader.PropertiesLauncher https://zosmf:443 jwtsecret.pem localca keystore/local_ca/localca.keystore.p12 PKCS12 local_ca_password local_ca_password
 */
public class SaveZosmfPublicKeyConsoleApplication {

    public static void main(String[] args) {
        HttpsConfig httpsConfig = readHttpsConfig();
        RestTemplate restTemplate = restTemplate(httpsConfig);

        String jwkUrl = args[0];
        String filename = args[1];
        ZosmfJwkToPublicKey zosmfJwkToPublicKey = new ZosmfJwkToPublicKey(restTemplate);

        System.out.printf("Loading public key of z/OSMF at %s\n", jwkUrl);
        try {
            if (zosmfJwkToPublicKey.updateJwtPublicKeyFile(jwkUrl, filename, args[2], args[3], args[4], args[5], args[6])) {
                System.out.printf("Public key of z/OSMF at stored as a certificate to %s\n", filename);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());  // NOSONAR: It is a console application
            System.exit(1);  // NOSONAR
        } catch (ResourceAccessException e) {
            System.err.println(e.getMessage());  // NOSONAR
            System.exit(2);  // NOSONAR
        }
    }

    private static RestTemplate restTemplate(HttpsConfig httpsConfig) {
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        CloseableHttpClient secureHttpClient = httpsFactory.createSecureHttpClient();
        HttpComponentsClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory(
                secureHttpClient);
        RestTemplate restTemplate = new RestTemplate(clientFactory);
        return restTemplate;
    }

    private static HttpsConfig readHttpsConfig() {
        String protocol = System.getProperty("server.ssl.protocol", "TLSv1.2");
        String trustStoreType = System.getProperty("server.ssl.trustStoreType", "PKCS12");
        String trustStorePassword = System.getProperty("server.ssl.trustStorePassword");
        String trustStore = System.getProperty("server.ssl.trustStore");
        boolean verifySslCertificatesOfServices = !System
                .getProperty("apiml.security.ssl.verifySslCertificatesOfServices", "true").equalsIgnoreCase("false");

        HttpsConfig httpsConfig = HttpsConfig.builder().protocol(protocol).trustStore(trustStore)
                .trustStoreType(trustStoreType).trustStorePassword(trustStorePassword)
                .trustStoreRequired(verifySslCertificatesOfServices)
                .verifySslCertificatesOfServices(verifySslCertificatesOfServices).build();
        return httpsConfig;
    }
}
