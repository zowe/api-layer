/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.token.TokenNotValidException;

class AuthSourceTest {
    @Nested
    class GivenCorrectIssuer {
        @Nested
        class WhenIssuerIsZosmf {
            @Test
            void thenOriginIsZosmf() {
                assertEquals(AuthSource.Origin.ZOSMF, AuthSource.Origin.valueByIssuer("zosmf"));
                assertEquals(AuthSource.Origin.ZOSMF, AuthSource.Origin.valueByIssuer("zOSMF"));
            }
        }

        @Nested
        class WhenIssuerIsZowe {
            @Test
            void thenOriginIsZowe() {
                assertEquals(AuthSource.Origin.ZOWE, AuthSource.Origin.valueByIssuer("Zowe"));
                assertEquals(AuthSource.Origin.ZOWE, AuthSource.Origin.valueByIssuer("ZOWE"));
            }
        }

        @Nested
        class WhenIssuerIsClientCertificate {
            @Test
            void thenOriginIsX509() {
                assertEquals(AuthSource.Origin.X509, AuthSource.Origin.valueByIssuer("x509"));
                assertEquals(AuthSource.Origin.X509, AuthSource.Origin.valueByIssuer("X509"));
            }
        }
    }

    @Nested
    class GivenIncorrectIssuer {
        @Nested
        class WhenUnknownIssuer {
            @Test
            void thenThrowException() {
                Exception tnve = assertThrows(TokenNotValidException.class, () -> AuthSource.Origin.valueByIssuer("unknown"));
                assertEquals("Unknown authentication source type : unknown", tnve.getMessage());
            }
        }

        @Nested
        class WhenNullIssuer {
            @Test
            void thenThrowException() {
                Exception tnve = assertThrows(TokenNotValidException.class, () -> AuthSource.Origin.valueByIssuer(null));
                assertEquals("Unknown authentication source type : null", tnve.getMessage());
            }
        }
    }
}
