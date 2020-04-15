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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

public class ZosmfJwkToPublicKeyTest {

    @Test
    public void zosmfJwkIsConvertedToPublicKey() throws IOException {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String jwk = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg\",\"alg\":\"RS256\",\"n\":\"wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q\"}]}";
        String expectedPublicKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwRdwksGIAR2A4cHsoOsY\ncGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg/6pIXzMRhmOvmjXjz1PL\nfI2GD3drmeqsStjISWdDfH/rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKN\nw9frJthKSMM52JtsgwrgN++Ub2cKWioU/d52SC2SfDzOdnChqlU7xkqXwKXSUqcG\nM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQ\nmwRVUvyC5kfBemX+7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO/LDMN2J\n+QIDAQAB\n-----END PUBLIC KEY-----";

        when(restTemplate.getForObject("https://zosmf:1433/jwt/ibm/api/zOSMFBuilder/jwk", String.class)).thenReturn(jwk);

        ZosmfJwkToPublicKey zosmfJwkToPublicKey = new ZosmfJwkToPublicKey(restTemplate);

        File f = File.createTempFile("jwt", null, new File(System.getProperty("java.io.tmpdir")));
        try {
            String filename = f.getName();
            assertTrue(zosmfJwkToPublicKey.updateJwtPublicKeyFile("https://zosmf:1433", filename));
            assertEquals(expectedPublicKey, new String(Files.readAllBytes(Paths.get(filename))).trim());
        } finally {
            f.delete();
        }
    }
}
