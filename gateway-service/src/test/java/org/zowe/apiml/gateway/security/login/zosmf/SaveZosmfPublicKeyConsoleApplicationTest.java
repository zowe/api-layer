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

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.HttpsConfig;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaveZosmfPublicKeyConsoleApplicationTest {

    @Test
    void whenProvidedCorrectInputs_thenJWTSecretIsCreated() throws Exception {

        RestTemplate restTemplate = mock(RestTemplate.class);
        String jwk = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg\",\"alg\":\"RS256\",\"n\":\"wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q\"}]}";

        when(restTemplate.getForObject("https://zosmf:1443/jwt/ibm/api/zOSMFBuilder/jwk", String.class))
            .thenReturn(jwk);

        File f = File.createTempFile("jwt", null, new File(System.getProperty("java.io.tmpdir")));
        try {
            String zosmfUrl = "https://zosmf:1443";
            String filename = f.getName();
            String alias = "localca";
            String caKeystore = "../keystore/local_ca/localca.keystore.p12";
            String caKeystoreType = "PKCS12";
            String caPassword = "local_ca_password";
            String[] args = {zosmfUrl, filename, alias, caKeystore, caKeystoreType, caPassword, caPassword};
            assertTrue(SaveZosmfPublicKeyConsoleApplication.saveJWTSecretWithJWKFromZosmf(restTemplate, args));
        } finally {
            f.delete();
        }
    }

    @Test
    void whenSystemPropertiesAreSet_thenConfigIsPopulated() {
        String trustStorePassword = "password";
        String protocol = "TLSv1.2";
        String trustStoreType = "PKCS12";
        String trustStore = "trustore.p12";
        System.setProperty("server.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("server.ssl.trustStore", trustStore);
        HttpsConfig config = SaveZosmfPublicKeyConsoleApplication.readHttpsConfig();
        assertEquals(config.getProtocol(), protocol);
        assertEquals(config.getTrustStore(), trustStore);
        assertEquals(config.getTrustStoreType(), trustStoreType);
        assertEquals(String.valueOf(config.getTrustStorePassword()), trustStorePassword);

    }
}
