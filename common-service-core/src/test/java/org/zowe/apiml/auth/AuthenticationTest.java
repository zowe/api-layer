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

    @Test
    void testSupportSso() {
        Authentication authentication = Authentication.builder()
                .supportsSso(null)
                .scheme(AuthenticationScheme.ZOSMF)
                .build();
        assertTrue(authentication.supportsSso());

        authentication = Authentication.builder()
                .supportsSso(true)
                .scheme(AuthenticationScheme.ZOSMF)
                .build();
        assertTrue(authentication.supportsSso());

        authentication = Authentication.builder()
                .supportsSso(false)
                .scheme(AuthenticationScheme.ZOSMF)
                .build();
        assertFalse(authentication.supportsSso());

        authentication = Authentication.builder()
                .supportsSso(null)
                .scheme(AuthenticationScheme.BYPASS)
                .build();
        assertFalse(authentication.supportsSso());

        authentication = Authentication.builder()
                .supportsSso(true)
                .scheme(AuthenticationScheme.BYPASS)
                .build();
        assertTrue(authentication.supportsSso());

        authentication = Authentication.builder()
                .supportsSso(false)
                .scheme(AuthenticationScheme.BYPASS)
                .build();
        assertFalse(authentication.supportsSso());
    }

}
