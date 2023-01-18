/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.apicatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.config.ApiCatalogServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

@CatalogTest
public class ApiCatalogStandaloneTest {

    private String baseHost;

    @BeforeEach
    void setUp() {
        ApiCatalogServiceConfiguration configuration = ConfigReader.environmentConfiguration().getApiCatalogStandaloneConfiguration();
        String host = configuration.getHost();
        int port = configuration.getPort();
        baseHost = host + ":" + port;
    }

    @Nested
    class ApiDoc {

        @Nested
        class ThenUpdatedReferences {

        }

    }

    @Nested
    class Access {

        @Nested
        class AuthenticationIsNotRequired {

            @Test
            void whenAccess_thenOk() {

            }
        }
    }
}
