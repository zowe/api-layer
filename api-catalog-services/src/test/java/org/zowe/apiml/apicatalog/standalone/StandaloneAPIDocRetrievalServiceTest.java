/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandaloneAPIDocRetrievalServiceTest {

    private final StandaloneAPIDocRetrievalService standaloneAPIDocRetrievalService = new StandaloneAPIDocRetrievalService();

    @Nested
    class ThenNothing {

        @Test
        void whenRetrieveApiDoc() {
            assertNull(standaloneAPIDocRetrievalService.retrieveApiDoc("service", null));
        }

        @Test
        void whenRetrieveDefaultApiDoc() {
            assertNull(standaloneAPIDocRetrievalService.retrieveDefaultApiDoc("service"));
        }

        @Test
        void whenRetrieveApiVersions() {
            List<String> apiVersions = standaloneAPIDocRetrievalService.retrieveApiVersions("service");
            assertTrue(apiVersions.isEmpty());
        }

        @Test
        void whenRetrieveDefaultApiVersion() {
            assertNull(standaloneAPIDocRetrievalService.retrieveDefaultApiVersion("service"));
        }

    }
}
