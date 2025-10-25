/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import org.jose4j.http.Response;
import org.jose4j.http.SimpleGet;
import org.jose4j.jwk.HttpsJwks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JWKResolverTest {

    @Mock
    private HttpsJwks httpsJwks;

    @Mock
    private HttpsJwksProvider provider;

    @Mock
    private SimpleGet simpleGet;

    private JWKResolver jwkResolver;

    @BeforeEach
    void setUp() {
        this.jwkResolver = new JWKResolver(provider);
    }

    @Nested
    class JwksUriLoad {

        @Test
        void givenMissingParameterInJWK_doNotThrowException() throws IOException {
            var url = "https://localhost/jwk";
            var jwks = new HttpsJwks(url);
            jwks.setSimpleHttpGet(simpleGet);

            var json = """
                {
                    "keys": [
                        {
                            "kty": null,
                            "alg": "RS256",
                            "kid": "Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4",
                            "use": "sig",
                            "e": "AQAB",
                            "n": "v6wT5k7uLto_VPTV8fW9_wRqWHuqnZbyEYAwNYRdffe9WowwnzUAr0Z93-4xDvCRuVfTfvCe9orEWdjZMaYlDq_Dj5BhLAqmBAF299Kv1GymOioLRDvoVWy0aVHYXXNaqJCPsaWIDiCly-_kJBbnda_rmB28a_878TNxom0mDQ20TI5SgdebqqMBOdHEqIYH1ER9euybekeqJX24EqE9YW4Yug5BOkZ9KcUkiEsH_NPyRlozihj18Qab181PRyKHE6M40W7w67XcRq2llTy-z9RrQupcyvLD7L62KN0ey8luKWnVg4uIOldpyBYyiRX2WPM-2K00RVC0e4jQKs34Gw"
                        }
                    ]
                }
                """;

            when(provider.getFor(url)).thenReturn(jwks);
            when(simpleGet.get(url)).thenReturn(new Response(200, "", Map.of(), json));

            assertDoesNotThrow(() -> jwkResolver.resolve(url));
        }

        @Test
        void giveValidJWK_setPublicKey() throws IOException {
            var url = "https://localhost/jwk";
            var jwks = new HttpsJwks(url);
            jwks.setSimpleHttpGet(simpleGet);

            var json = """
                {
                    "keys": [
                        {
                            "kty": "RSA",
                            "alg": "RS256",
                            "kid": "-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4",
                            "use": "sig",
                            "e": "AQAB",
                            "n": "5rYyqFsxel0Pv-xRDHPbg3IfumE4ks9ffLvJrfZVgrTQyiFmFfBnyD3r7y6626Yr5-68Pj0I5SHlCBPkkgTU_e9Z3tCYiegtIOeJdSdumWR2JDVAsbpwFJDG_kxP9czgX7HL0T2BPSapx7ba0ZBXd2-SfSDDL-c1Q0rJ1uQEJwDXAGZV4qy_oXuQf5DuV65Xj8y2Qn1DtVEBThxita-kis_H35CTWgW2zyyaS_08wa00R98mnQ2SHfmO5fZABITmH0DO0coDHqKZ429VNNpELLX9e95dirQ1jfngDbBCmy-XsT8yc6NpAaXmd8P2NHdsO2oK46EQEaFRyMcoDTs3-w"
                        }
                    ]
                }
                """;
            when(provider.getFor(url)).thenReturn(jwks);
            when(simpleGet.get(url)).thenReturn(new Response(200, "", Map.of(), json));

            assertDoesNotThrow(() -> jwkResolver.resolve(url));
        }

    }
}
