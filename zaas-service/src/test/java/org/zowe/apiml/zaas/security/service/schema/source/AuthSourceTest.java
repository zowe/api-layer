/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.token.QueryResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthSourceTest {
    @Nested
    class GivenCorrectTokenSource {
        @Nested
        class WhenSourceIsZosmf {
            @Test
            void thenOriginIsZosmf() {
                assertEquals(AuthSource.Origin.ZOSMF, AuthSource.Origin.valueByTokenSource(QueryResponse.Source.ZOSMF));
            }
        }

        @Nested
        class WhenSourceIsZowe {
            @Test
            void thenOriginIsZowe() {
                assertEquals(AuthSource.Origin.ZOWE, AuthSource.Origin.valueByTokenSource(QueryResponse.Source.ZOWE));
            }
        }

        @Nested
        class WhenSourceIsPAT {
            @Test
            void thenOriginIsX509() {
                assertEquals(AuthSource.Origin.ZOWE_PAT, AuthSource.Origin.valueByTokenSource(QueryResponse.Source.ZOWE_PAT));
            }
        }

        @Nested
        class WhenSourceIsOidc {
            @Test
            void thenOriginIsX509() {
                assertEquals(AuthSource.Origin.OIDC, AuthSource.Origin.valueByTokenSource(QueryResponse.Source.OIDC));
            }
        }
    }

    @Nested
    class GivenUnsupportedSource {

        @Nested
        class WhenNullSource {
            @Test
            void thenThrowException() {
                assertThrows(NullPointerException.class, () -> AuthSource.Origin.valueByTokenSource(null));
            }
        }
    }
}
