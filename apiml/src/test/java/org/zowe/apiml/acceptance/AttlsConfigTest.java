/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.ApimlApplication;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    @Nested
    @TestPropertySource(
        properties = {
            "server.attls.enabled=true",
            "server.ssl.enabled=false"
        }
    )
    @ActiveProfiles("attls")
    @DirtiesContext
    @SpringBootTest(
        classes = ApimlApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenAttlsProfile {

    }

    /**
     * This test intends to verify ICSF workaround (no keyring load)
     */
    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.enabled=false",
            "server.ssl.keyStoreType=",
            "server.ssl.keyStorePassword=",
            "server.ssl.keyPassword=",
            "server.ssl.keyAlias=",
            "server.ssl.keyStore="
        }
    )
    @ActiveProfiles("attls")
    @DirtiesContext
    @SpringBootTest(
        classes = ApimlApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled {

    }

}
