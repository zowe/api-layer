/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTest {

    @Test
    void testIsEmpty() {
        assertFalse(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid").isEmpty());
        assertFalse(new Authentication(AuthenticationScheme.ZOSMF, null).isEmpty());
        assertFalse(new Authentication(null, "applid").isEmpty());
        assertFalse(new Authentication(null, "").isEmpty());
        assertTrue(new Authentication(null, null).isEmpty());
    }

    @ParameterizedTest(name = "{index} - metadata configuration: {0} and authentication scheme: {1}")
    @CsvSource({
            ", ZOSMF",
            "true, ZOSMF",
            ", HTTP_BASIC_PASSTICKET",
            "true, HTTP_BASIC_PASSTICKET",
            ", ZOWE_JWT",
            "true , ZOWE_JWT",
            "true, BYPASS"
    })
    void testSupportSso(Boolean supportSso, AuthenticationScheme authenticationScheme) {
        Authentication authentication = Authentication.builder()
                .supportsSso(supportSso)
                .scheme(authenticationScheme)
                .build();
        assertTrue(authentication.supportsSso());
    }

    @ParameterizedTest(name = "{index} - metadata configuration: {0} and authentication scheme: {1}")
    @CsvSource({
            "false, ZOSMF",
            "false, HTTP_BASIC_PASSTICKET",
            "false, ZOWE_JWT",
            ", BYPASS",
            "false, BYPASS"
    })
    void testNotSupportSso(Boolean supportSso, AuthenticationScheme authenticationScheme) {
        Authentication authentication = Authentication.builder()
                .supportsSso(supportSso)
                .scheme(authenticationScheme)
                .build();
        assertFalse(authentication.supportsSso());
    }

}
